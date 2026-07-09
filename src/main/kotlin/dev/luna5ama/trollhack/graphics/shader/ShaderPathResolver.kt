package dev.luna5ama.trollhack.graphics.shader

import dev.luna5ama.trollhack.utils.ResourceHelper
import java.io.FileNotFoundException
import java.nio.charset.Charset

interface ShaderPathResolver {
    fun resolve(path: String): Result<ByteArray>

    fun resolveSource(path: String, encoding: Charset = Charsets.UTF_8): Result<CharSequence> {
        return resolve(path).mapCatching { it.toString(encoding) }
    }
}

object DefaultShaderPathResolver : ShaderPathResolver {
    override fun resolve(path: String): Result<ByteArray> {
        return runCatching { ResourceHelper.getResourceStream(path)?.readAllBytes() ?: throw FileNotFoundException() }
    }
}