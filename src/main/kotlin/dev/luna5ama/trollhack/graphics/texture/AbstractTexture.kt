package dev.luna5ama.trollhack.graphics.texture

import dev.luna5ama.trollhack.graphics.texture.delegate.LateUploadTexture
import org.lwjgl.opengl.GL46.*
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicReference

abstract class AbstractTexture : Texture {
    final override var msaaLevel: Int
        get() = 0
        set(value) {}
    override val id = glCreateTextures(GL_TEXTURE_2D)
    var type = TextureType.DIFFUSE
    var storageLevels = 1

    val state: AtomicReference<State> = AtomicReference(State.CREATED)
    override val available get() = state.get() == State.UPLOADED

    override fun bindTexture() {
        val state = state.get()
        when {
            state == State.UPLOADED -> glBindTexture(GL_TEXTURE_2D, id)
            state == State.CREATED && this is LateUploadTexture -> glBindTexture(GL_TEXTURE_2D, id)
            state == State.DELETED -> throw IllegalStateException("This texture has been deleted!")
            state == State.CREATED -> throw IllegalStateException("This texture has not uploaded!")
        }
    }

    override fun unbindTexture() = glBindTexture(GL_TEXTURE_2D, 0)

    override fun deleteTexture() {
        glDeleteTextures(id)
        state.set(State.DELETED)
    }

    override fun equals(other: Any?) =
        this === other || other is AbstractTexture && this.id == other.id

    override fun hashCode() = id

    fun uploadImage(
        bufferedImage: BufferedImage,
        format: Int,
        beforeUpload: AbstractTexture.() -> Unit,
        afterUpload: AbstractTexture.() -> Unit
    ) {
        if (state.get() == State.CREATED) {
            width = bufferedImage.width
            height = bufferedImage.height

            beforeUpload.invoke(this@AbstractTexture)
            ImageUtils.uploadImage(this@AbstractTexture, bufferedImage, format, width, height)
            afterUpload.invoke(this@AbstractTexture)
            state.set(State.UPLOADED)
        } else throw IllegalStateException("Texture already uploaded or deleted.")
    }

    enum class State {
        CREATED,
        UPLOADED,
        DELETED
    }
}

enum class TextureType {
    DIFFUSE,
    NORMAL,
    SPECULAR,
    DISPLACEMENT,
    AMBIENT,
    ROUGHNESS
}
