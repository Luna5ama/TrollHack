package cum.xiaro.trollhack.util.graphics.font.glyph

import cum.xiaro.trollhack.util.graphics.texture.GrayscaleMipmapTexture
import cum.xiaro.trollhack.util.graphics.texture.MipmapTexture
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.glTexParameterf
import org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS

class GlyphChunk(
    /** Id of this chunk */
    val id: Int,

    /** [MipmapTexture] object */
    val texture: GrayscaleMipmapTexture,

    /** Array for all characters' info in this chunk */
    val charInfoArray: Array<CharInfo>
) {
    private var lodbias = Float.NaN

    fun updateLodBias(input: Float) {
        if (input != lodbias) {
            lodbias = input
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, input)
        }
    }

    override fun equals(other: Any?) =
        this === other
            || other is GlyphChunk
            && id == other.id
            && texture == other.texture

    override fun hashCode() = 31 * id + texture.hashCode()
}