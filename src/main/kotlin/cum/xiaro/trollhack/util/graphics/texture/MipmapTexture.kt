package cum.xiaro.trollhack.util.graphics.texture

import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.glTexParameteri
import org.lwjgl.opengl.GL12.*
import org.lwjgl.opengl.GL30.glGenerateMipmap
import java.awt.image.BufferedImage

class MipmapTexture(bufferedImage: BufferedImage, format: Int, levels: Int) : AbstractTexture() {
    override val width = bufferedImage.width
    override val height = bufferedImage.height

    init {
        // Generate texture id and bind it
        genTexture()
        bindTexture()

        // Setup mipmap levels
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, levels)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, levels)

        // Generate level 0 (original size) texture
        TextureUtils.uploadRGBA(bufferedImage, format)
        glGenerateMipmap(GL_TEXTURE_2D)

        // Unbind texture
        unbindTexture()
    }
}