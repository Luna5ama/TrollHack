package dev.luna5ama.trollhack.graphics.matrix

import dev.luna5ama.trollhack.RenderSystem
import dev.luna5ama.trollhack.utils.collections.fastForEach
import org.joml.Matrix4f
import org.joml.Quaternionf
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

data class MatrixLayer(
    val position: Matrix4f
) {
    constructor() : this(Matrix4f().identity())
    constructor(other: MatrixLayer) : this(Matrix4f(other.position))

    operator fun times(other: MatrixLayer): MatrixLayer {
        position.mul(other.position)
        return this
    }

    fun mul(other: MatrixLayer) = this * other

    fun mulPosition(matrix4f: Matrix4f): MatrixLayer {
        position.mul(matrix4f)
        return this
    }

    fun rotate(quaternionf: Quaternionf): MatrixLayer {
        position.rotate(quaternionf)
        return this
    }

    fun set(other: MatrixLayer): MatrixLayer {
        position.set(other.position)
        return this
    }

    fun setPosition(matrix4f: Matrix4f): MatrixLayer {
        position.set(matrix4f)
        return this
    }

    fun identity(): MatrixLayer {
        position.identity()
        return this
    }

    val transformMatrix: Matrix4f get() = Matrix4f(RenderSystem.projection).mul(RenderSystem.modelView).mul(position)

    fun getFloatArray(): FloatArray = transformMatrix.getFloatArray()
}

class MatrixLayerStack() {

    val stack = Stack<MatrixLayer>().apply { push(MatrixLayer()) }
    var checkID = 0L

    fun pushMatrixLayer(layer: MatrixLayer = MatrixLayer()) {
        stack.push(layer)
    }

    fun popMatrixLayer(): MatrixLayer {
        return if (stack.empty()) {
            throw Exception("Stack must has at least 1 matrix!")
        } else if (stack.size == 1) {
            stack.peek().identity()
        } else {
            stack.pop()
        }
    }

    inline val peek: MatrixLayer get() = stack.peek()

    inline val transformMatrix get() = peek.transformMatrix

    inline val matrixArray get() = peek.getFloatArray()

    private fun getMatrixResult(): MatrixLayer {
        val result = MatrixLayer()
        stack.fastForEach { mat ->
            result * mat
        }
        return result
    }

    fun makeIdentity(): MatrixLayer = peek.identity()

    fun apply(checkInc: Int, matrixLayer: MatrixLayer): MatrixLayer {
        checkID += checkInc
        return peek.set(matrixLayer)
    }

    fun applyPosition(checkInc: Int, matrix4f: Matrix4f): MatrixLayer {
        checkID += checkInc
        return peek.setPosition(matrix4f)
    }

    fun mulPosition(checkInc: Int, matrix4f: Matrix4f): MatrixLayer {
        checkID += checkInc
        return peek.mulPosition(matrix4f)
    }

    fun rotate(checkInc: Int, quaternionf: Quaternionf): MatrixLayer {
        checkID += checkInc
        return peek.rotate(quaternionf)
    }

    fun mul(checkInc: Int, matrixLayer: MatrixLayer): MatrixLayer {
        checkID += checkInc
        return peek * matrixLayer
    }

    private val id = AtomicInteger()
    private fun genID() = id.getAndIncrement()
    fun resetID() {
        id.set(0)
    }

    inner class MatrixScope(val layer: MatrixLayerStack, preMat: MatrixLayer) {
        private val prevMat = MatrixLayer(preMat)
        val checkInc = genID()
        fun recover() {
            layer.apply(checkInc, prevMat)
        }
    }

}

inline fun MatrixLayerStack.newScope(): MatrixLayerStack.MatrixScope {
    return this.MatrixScope(this, peek)
}

inline fun MatrixLayerStack.useScope(
    scope: MatrixLayerStack.MatrixScope,
    block: MatrixLayerStack.MatrixScope.() -> Unit
) {
    val preCheckID = checkID
    scope.block()
    scope.recover()
    checkID = preCheckID
}

@DslMarker
annotation class MatrixDSL

@MatrixDSL
inline fun MatrixLayerStack.scope(
    block: MatrixLayerStack.MatrixScope.() -> Unit
) = useScope(newScope(), block)