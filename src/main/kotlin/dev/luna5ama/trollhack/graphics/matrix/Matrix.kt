package dev.luna5ama.trollhack.graphics.matrix

import dev.luna5ama.trollhack.utils.math.vectors.Vec3f
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.Vector
import kotlin.math.PI

fun Matrix4f.getFloatArray() = floatArrayOf(
    m00(), m01(), m02(), m03(),
    m10(), m11(), m12(), m13(),
    m20(), m21(), m22(), m23(),
    m30(), m31(), m32(), m33()
)

fun Vector3f.getFloatArray() = floatArrayOf(x, y, z)

// Stack


fun MatrixLayerStack.MatrixScope.translatef(x: Float, y: Float, z: Float): MatrixLayerStack.MatrixScope {
    layer.mulPosition(checkInc, Matrix4f().translate(x, y, z))
    return this
}

fun MatrixLayerStack.MatrixScope.scalef(x: Float, y: Float, z: Float): MatrixLayerStack.MatrixScope {
    layer.mulPosition(checkInc, Matrix4f().scale(x, y, z))
    return this
}

fun MatrixLayerStack.MatrixScope.rotatef(angleDegree: Float, axis: Vec3f): MatrixLayerStack.MatrixScope {
    layer.mulPosition(checkInc, Matrix4f().rotate((angleDegree / (180f / PI)).toFloat(), Vector3f(axis.x, axis.y, axis.z)))
    return this
}

fun MatrixLayerStack.MatrixScope.rotate(quat: Quaternionf): MatrixLayerStack.MatrixScope {
    layer.rotate(checkInc, quat)
    return this
}

fun MatrixLayerStack.MatrixScope.mul(other: MatrixLayer): MatrixLayerStack.MatrixScope {
    layer.mul(checkInc, MatrixLayer(other))
    return this
}

fun MatrixLayerStack.MatrixScope.mulPosition(other: Matrix4f): MatrixLayerStack.MatrixScope {
    layer.mulPosition(checkInc, other)
    return this
}

fun MatrixLayerStack.MatrixScope.apply(other: MatrixLayer): MatrixLayerStack.MatrixScope {
    layer.apply(checkInc, MatrixLayer(other))
    return this
}

fun MatrixLayerStack.MatrixScope.applyPosition(other: Matrix4f): MatrixLayerStack.MatrixScope {
    layer.applyPosition(checkInc, other)
    return this
}

fun Vector3f.transform(matrix: Matrix4f): Vector3f {
    return matrix.transformPosition(this)
}

inline fun matrixStack(block: MatrixLayerStack.() -> Unit): MatrixLayerStack = MatrixLayerStack().apply {
    block()
}
