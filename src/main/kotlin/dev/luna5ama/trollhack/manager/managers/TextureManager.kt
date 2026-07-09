package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.graphics.texture.Texture
import dev.luna5ama.trollhack.graphics.texture.loader.AsyncTextureLoader
import dev.luna5ama.trollhack.graphics.texture.loader.LazyTextureContainer
import dev.luna5ama.trollhack.graphics.texture.loader.TextureLoader
import org.lwjgl.opengl.GL46

object TextureManager : TextureLoader by AsyncTextureLoader(2) {
    fun lazyTexture(
        path: String,
        format: Int = GL46.GL_RGBA,
        levels: Int = 3,
        useMipmap: Boolean = true,
        qualityLevel: Int = 2
    ): Texture = LazyTextureContainer(path, format, levels, useMipmap, qualityLevel).register()
}