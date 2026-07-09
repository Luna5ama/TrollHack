package dev.luna5ama.trollhack.graphics.texture.delegate

import dev.luna5ama.trollhack.graphics.texture.AbstractTexture
import org.lwjgl.opengl.GL11
import java.awt.image.BufferedImage

/**
 * @author SpartanB312
 */
open class LateUploadTexture(
    private val format: Int = GL11.GL_RGBA,
    private val beforeUpload: AbstractTexture.() -> Unit = {},
    private val afterUpload: AbstractTexture.() -> Unit = {}
) : DelegateTexture() {
    @Synchronized
    fun upload(
        bufferedImage: BufferedImage
    ) {
        uploadImage(bufferedImage, format, beforeUpload, afterUpload)
    }

    final override var width = 0
    final override var height = 0
}