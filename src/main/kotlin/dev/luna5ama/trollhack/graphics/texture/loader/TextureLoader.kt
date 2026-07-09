package dev.luna5ama.trollhack.graphics.texture.loader

import org.lwjgl.opengl.GL46
import java.util.concurrent.LinkedBlockingQueue

/**
 * @author SpartanB312
 */
interface TextureLoader {
    val queue: LinkedBlockingQueue<LazyTextureContainer>
    val activeThread: Int
    val totalThread: Int
    val loadedCount: Int
    val totalCount: Int
    val parallelLimit: Int
    fun renderThreadHook(timeLimit: Int = -1)
    fun add(texture: LazyTextureContainer)
    fun resume()
    fun suspend()
    fun stop()
    fun remaining(): Int = queue.size
    fun LazyTextureContainer.register(): LazyTextureContainer {
        add(this)
        return this
    }

    fun lazyLoad(
        path: String,
        format: Int = GL46.GL_RGBA,
        levels: Int = 3,
        useMipmap: Boolean = true,
        qualityLevel: Int = 2
    ): LazyTextureContainer {
        val lazyTexture = LazyTextureContainer(path, format, levels, useMipmap, qualityLevel)
        add(lazyTexture)
        return lazyTexture
    }
}