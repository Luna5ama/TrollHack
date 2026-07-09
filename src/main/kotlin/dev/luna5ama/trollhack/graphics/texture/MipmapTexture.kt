package dev.luna5ama.trollhack.graphics.texture

import dev.luna5ama.trollhack.utils.ResourceHelper
import dev.luna5ama.trollhack.graphics.texture.delegate.DelegateTexture
import dev.luna5ama.trollhack.graphics.texture.delegate.InstantTexture
import dev.luna5ama.trollhack.graphics.texture.delegate.LateUploadTexture
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.*
import org.lwjgl.opengl.GL45.glGenerateTextureMipmap
import org.lwjgl.opengl.GL45.glTextureParameteri
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class MipmapTexture(val delegate: DelegateTexture) : Texture by delegate {

    constructor(
        img: BufferedImage,
        format: Int = GL_RGBA,
        levels: Int = 3,
        useMipmap: Boolean = true,
        qualityLevel: Int = 3,
    ) : this(instant(img, format, levels, useMipmap, qualityLevel))

    constructor(
        path: String,
        format: Int = GL_RGBA,
        levels: Int = 3,
        useMipmap: Boolean = true,
        qualityLevel: Int = 3,
    ) : this(instant(ImageIO.read(ResourceHelper.getResourceStream(path)!!), format, levels, useMipmap, qualityLevel))

    companion object {

        @JvmStatic
        fun instant(
            img: BufferedImage,
            format: Int = GL_RGBA,
            levels: Int = 3,
            useMipmap: Boolean = true,
            qualityLevel: Int = 3,
        ): InstantTexture = InstantTexture(
            img, format,
            beforeUpload = beforeUpload(levels, useMipmap, qualityLevel),
            afterUpload = afterUpload(useMipmap)
        )

        @JvmStatic
        fun lateUpload(
            format: Int = GL_RGBA,
            levels: Int = 3,
            useMipmap: Boolean = true,
            qualityLevel: Int = 3,
        ): LateUploadTexture = LateUploadTexture(
            format,
            beforeUpload = beforeUpload(levels, useMipmap, qualityLevel),
            afterUpload = afterUpload(useMipmap)
        )

        @JvmStatic
        fun beforeUpload(
            levels: Int = 3,
            useMipmap: Boolean = true,
            qualityLevel: Int = 3
        ): AbstractTexture.() -> Unit = {
            val storageLevels = if (useMipmap) levels.coerceAtLeast(1) else 1
            this.storageLevels = storageLevels

            glTextureParameteri(id, GL_TEXTURE_WRAP_S, GL_REPEAT)
            glTextureParameteri(id, GL_TEXTURE_WRAP_T, GL_REPEAT)

            glTextureParameteri(
                id,
                GL_TEXTURE_MIN_FILTER,
                if (useMipmap) {
                    if (qualityLevel >= 3) GL_LINEAR_MIPMAP_LINEAR
                    else GL_LINEAR_MIPMAP_NEAREST
                } else if (qualityLevel >= 2) GL_LINEAR else GL_NEAREST
            )
            glTextureParameteri(
                id,
                GL_TEXTURE_MAG_FILTER,
                if (qualityLevel >= 1) GL_LINEAR else GL_NEAREST
            )

            if (useMipmap) {
                glTextureParameteri(id, GL_TEXTURE_MIN_LOD, 0)
                glTextureParameteri(id, GL_TEXTURE_MAX_LOD, storageLevels - 1)
                glTextureParameteri(id, GL_TEXTURE_BASE_LEVEL, 0)
                glTextureParameteri(id, GL_TEXTURE_MAX_LEVEL, storageLevels - 1)
            }
        }

        @JvmStatic
        fun afterUpload(
            useMipmap: Boolean = true
        ): AbstractTexture.() -> Unit = {
            if (useMipmap) glGenerateTextureMipmap(id)
        }

    }

}
