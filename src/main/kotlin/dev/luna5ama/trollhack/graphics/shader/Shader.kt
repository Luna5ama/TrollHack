package dev.luna5ama.trollhack.graphics.shader

import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import dev.luna5ama.trollhack.graphics.GLHelper
import dev.luna5ama.trollhack.graphics.GLObject
import dev.luna5ama.trollhack.graphics.matrix.getFloatArray
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.math.vectors.Vec2i
import dev.luna5ama.trollhack.utils.math.vectors.Vec3f
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.lwjgl.opengl.GL20.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class Shader(
    private val vsh: String,
    private val fsh: String,
    private val pathResolver: ShaderPathResolver = DefaultShaderPathResolver
) : GLObject {
    final override val id: Int

    init {
        val id = glCreateProgram()
        val vshID = createShader(vsh, GL_VERTEX_SHADER)
        val fshID = createShader(fsh, GL_FRAGMENT_SHADER)

        glAttachShader(id, vshID)
        glAttachShader(id, fshID)
        glLinkProgram(id)

        if (glGetProgrami(id, GL_LINK_STATUS) == 0) {
            val message = glGetProgramInfoLog(id, 1024)
            glDeleteProgram(id)
            glDeleteShader(vshID)
            glDeleteShader(fshID)
            throw IllegalStateException("Failed to link shader: $message")
        }

        glDeleteShader(vshID)
        glDeleteShader(fshID)
        this.id = id
    }

    private val isValidShader get() = id != 0

    private fun createShader(path: String, shaderType: Int): Int =
        createShader(path, pathResolver.resolve(path).getOrThrow(), shaderType)

    private fun createShader(path: String, bytes: ByteArray, shaderType: Int): Int {
        val srcString = bytes.decodeToString().lineSequence().joinToString(separator = "\n") { line ->
            (includeRegex1.matchEntire(line) ?: includeRegex2.matchEntire(line))?.let {
                "\n${pathResolver.resolveSource(it.groupValues[1]).getOrThrow()}\n"
            } ?: line
        }
        val shaderId = glCreateShader(shaderType)

        glShaderSource(shaderId, srcString)
        glCompileShader(shaderId)

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            TrollHackMod.LOGGER.error("Failed to create" +
                    " ${if (shaderType == GL_VERTEX_SHADER) "vertex shader" else "frag shader"}: " +
                    "$path\n${glGetShaderInfoLog(shaderId, 1024)}")
            glDeleteShader(shaderId)
            return 0
        }
        return shaderId
    }

    fun safeBind() {
        if (!isValidShader) throw Exception("Invalid shader(vsh:$vsh, fsh:$fsh)")
        else GLHelper.useProgram(id)
    }

    override fun bind() = GLHelper.useProgram(id)

    override fun unbind() = GLHelper.unbindProgram()

    override fun destroy() = glDeleteProgram(id)

    fun getUniformLocation(name: String): Int {
        return glGetUniformLocation(id, name)
    }

    companion object {
        val includeRegex1 = "# ?include\\s+\"([^\"]+)\"".toRegex() // # include "source.glsl"
        val includeRegex2 = "# ?include\\s+<([^\"]+)>".toRegex() // # include <source.glsl>
    }
}

fun Boolean.glUniform(location: Int) {
    glUniform1i(location, if (this) 1 else 0)
}

fun Int.glUniform(location: Int) {
    glUniform1i(location, this)
}

fun Float.glUniform(location: Int) {
    glUniform1f(location, this)
}

fun Vec2i.glUniform(location: Int) {
    glUniform2i(location, this.x, this.y)
}

fun Vec2f.glUniform(location: Int) {
    glUniform2f(location, this.x, this.y)
}

fun Vec3i.glUniform(location: Int) {
    glUniform3i(location, this.x, this.y, this.z)
}

fun Vec3f.glUniform(location: Int) {
    glUniform3f(location, this.x, this.y, this.z)
}

fun Vec3.glUniform(location: Int) {
    glUniform3f(location, this.x.toFloat(), this.y.toFloat(), this.z.toFloat())
}

fun Matrix4f.glUniform(location: Int, transpose: Boolean = false) {
    glUniformMatrix4fv(location, transpose, this.getFloatArray())
}

fun FloatArray.glUniform(location: Int, transpose: Boolean = false) {
    glUniformMatrix4fv(location, transpose, this)
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Shader> T.useShader(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    bind()
    block()
    GLHelper.unbindProgram()
}