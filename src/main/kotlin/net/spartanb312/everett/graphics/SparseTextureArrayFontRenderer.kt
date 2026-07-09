package net.spartanb312.everett.graphics

import dev.luna5ama.trollhack.RS
import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.impl.render.FenceSyncEvent
import dev.luna5ama.trollhack.graphics.GLHelper
import dev.luna5ama.trollhack.graphics.buffer.VertexFormat
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects.VertexMode
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects.draw
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.matrix.scalef
import dev.luna5ama.trollhack.graphics.matrix.scope
import dev.luna5ama.trollhack.graphics.matrix.translatef
import dev.luna5ama.trollhack.graphics.shader.Shader
import dev.luna5ama.trollhack.graphics.texture.ImageUtils
import org.lwjgl.opengl.*
import org.lwjgl.opengl.ARBSparseTexture.glTexturePageCommitmentEXT
import org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP
import org.lwjgl.opengl.GL11.GL_UNPACK_ROW_LENGTH
import org.lwjgl.opengl.GL11.GL_UNPACK_SKIP_PIXELS
import org.lwjgl.opengl.GL11.GL_UNPACK_SKIP_ROWS
import org.lwjgl.opengl.GL11.glPixelStorei
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil

/**
 * Sparse texture array font renderer
 * Presented as a single file
 */
class SparseTextureArrayFontRenderer(
    val font: Font,
    val chunkSize: Int = 64,
    val imgSize: Int = 512,
    val padding: Int = 1,
    val generalScale: Float = 1f
) {
    private val chunkAmount = ceil(65536f / chunkSize).toInt()
    private val badChunks = IntArray(chunkAmount) { 0 }
    private val initialized = IntArray(chunkAmount) { 0 }
    private val charWidthData = IntArray(65536) { 0 }
    private val charDataArray = arrayOfNulls<CharData>(65536)
    private val scaledPadding = (padding * font.size / 12.5f).toInt()
    private var fontHeight = 0
    private var italicAddon = 0

    internal val texture = GL45.glCreateTextures(GL30.GL_TEXTURE_2D_ARRAY).also {
        GL45.glTextureParameteri(it, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT)
        GL45.glTextureParameteri(it, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT)
        GL45.glTextureParameteri(it, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL45.glTextureParameteri(it, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        GL45.glTextureParameteri(it, ARBSparseTexture.GL_VIRTUAL_PAGE_SIZE_INDEX_ARB, 0)
        GL45.glTextureParameteri(it, ARBSparseTexture.GL_TEXTURE_SPARSE_ARB, GL11.GL_TRUE)
        GL45.glTextureStorage3D(it, 1, GL11.GL_RGBA8, imgSize, imgSize, chunkAmount)
    }

    init {
        // initialize all ascii chunks
        repeat(ceil(256f / chunkSize).toInt()) { checkChunk(it) }
    }

    private fun initGlyphPage(chunkID: Int): BufferedImage? {
        return try {
            BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB).also { img ->
                (img.createGraphics()).let { graphics ->
                    graphics.font = font
                    graphics.color = Color(255, 255, 255, 0)
                    graphics.fillRect(0, 0, imgSize, imgSize)
                    graphics.color = Color.white
                    graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
                    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    val metrics = graphics.fontMetrics
                    val ascent = metrics.ascent
                    var posX = 0
                    var posY = 1
                    val rowHeight = metrics.height
                    val charHeight = metrics.ascent + metrics.descent
                    fontHeight = charHeight
                    italicAddon = if (font.isItalic) ceil(rowHeight * 0.17632698070846).toInt() else 0
                    val startIndex = chunkID * chunkSize
                    for (index in 0 until chunkSize) {
                        val charCode = startIndex + index
                        val char = charCode.toChar()
                        var charWidth = metrics.charWidth(char)
                        val isEmpty = charWidth == 0
                        if (isEmpty) charWidth = charHeight
                        val renderWidth = charWidth + italicAddon
                        if (posX + renderWidth > imgSize) {
                            posX = 0
                            posY += rowHeight + scaledPadding * 2
                        }
                        val charData = CharData(charWidth, charHeight, if (isEmpty) 0 else renderWidth)
                        charWidthData[charCode] = charWidth
                        val startX = posX + scaledPadding
                        charData.u = startX / imgSize.toFloat()
                        charData.v = posY / imgSize.toFloat()
                        charData.u1 = (startX + renderWidth) / imgSize.toFloat()
                        charData.v1 = (posY + charHeight) / imgSize.toFloat()
                        charDataArray[charCode] = charData
                        graphics.drawString(char.toString(), startX, posY + ascent)
                        posX += (renderWidth + scaledPadding * 2)
                    }
                }
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            println("Failed to init chunk $chunkID")
            null
        }
    }

    fun checkChunk(chunkID: Int) {
        if (badChunks[chunkID] == 1 || initialized[chunkID] == 1) return
        val img = initGlyphPage(chunkID)
        if (img != null) {
            // Block game workaround
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
            glPixelStorei(GL_UNPACK_SKIP_ROWS, 0)
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0)

            glTexturePageCommitmentEXT(
                texture,
                0,
                0,
                0,
                chunkID,
                imgSize,
                imgSize,
                1,
                true
            )
            GL45.glTextureSubImage3D(
                texture,
                0,
                0,
                0,
                chunkID,
                imgSize,
                imgSize,
                1,
                GL12.GL_BGRA,
                GL11.GL_UNSIGNED_BYTE,
                img.getRGBArray()
            )
            initialized[chunkID] = 1
        } else badChunks[chunkID] = 1
    }

    fun drawString(
        text: String,
        x: Float,
        y: Float,
        color: ColorRGBA,
        scale: Float
    ) {
        val appliedScale = scale * this.generalScale
        val alpha = color.a
        var currentColor = color
        RS.matrixLayer.scope {
            translatef(x, y, 0f)
            scalef(appliedScale, appliedScale, 1f)
            glActiveTexture(GL_TEXTURE0)
            GLHelper.depth = false
            GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, texture)
            var startX = 0f
            var startY = 0f
            var chunk = -1
            var shouldSkip = false
            for (index in text.indices) {
                if (shouldSkip) {
                    shouldSkip = false
                    continue
                }
                val char = text[index]
                if (char == '\n') {
                    startY += fontHeight
                    startX = 0f
                    continue
                }
                if (char == '§' || char == '&') {
                    val next = text.getOrNull(index + 1)
                    if (next != null) {
                        val newColor = next.getColor(color)
                        if (newColor != null) {
                            currentColor = newColor.alpha(alpha)
                            shouldSkip = true
                            continue
                        }
                    }
                }
                val currentChunk = char.code / chunkSize
                if (currentChunk != chunk) {
                    chunk = currentChunk
                    checkChunk(currentChunk)
                }
                val data = charDataArray[char.code] ?: continue
                val endX = startX + data.renderWidth
                val endY = startY + data.height
                GL_TRIANGLE_STRIP.draw(RenderBuffer) {
                    putVertex(
                        endX,
                        startY,
                        data.u1,
                        data.v,
                        currentChunk,
                        currentColor
                    )
                    putVertex(
                        startX,
                        startY,
                        data.u,
                        data.v,
                        currentChunk,
                        currentColor
                    )
                    putVertex(
                        endX,
                        endY,
                        data.u1,
                        data.v1,
                        currentChunk,
                        currentColor
                    )
                    putVertex(
                        startX,
                        endY,
                        data.u,
                        data.v1,
                        currentChunk,
                        currentColor
                    )
                }
                startX += data.width
            }
        }
    }

    fun getHeight(scale: Float = 1f): Float = fontHeight * scale * generalScale

    fun getWidth(char: Char, scale: Float = 1f): Float = charWidthData[char.code] * scale * generalScale

    fun getWidth(text: String = "", scale: Float = 1f): Float = rawWidth(text, scale) * generalScale

    fun rawWidth(text: String = "", scale: Float = 1f): Float {
        var sum = 0f
        var shouldSkip = false
        for (index in text.indices) {
            if (shouldSkip) {
                shouldSkip = false
                continue
            }
            val char = text[index]
            val chunk = char.code / chunkSize
            if (badChunks[chunk] == 1) continue
            checkChunk(chunk)
            val delta = getWidth(char, scale)
            if (char == '§' || char == '&') {
                val next = text.getOrNull(index + 1)
                if (next != null && next.colorCode != null) {
                    shouldSkip = true
                }
                continue
            } else sum += delta
        }
        return sum + italicAddon * scale// specified scale has already been applied in getWidth(char,scale)
    }

    class CharData(
        val width: Int,
        val height: Int,
        val renderWidth: Int
    ) {
        var u = 0f
        var v = 0f
        var u1 = 0f
        var v1 = 1f

        @Override
        override fun toString(): String {
            return "CharData(width=$width, height=$height, u=$u, v=$v, u1=$u1, v1=$v1)"
        }
    }

    private fun BufferedImage.getRGBArray(): IntArray {
        val array = IntArray(width * height)
        getRGB(0, 0, width, height, array, 0, width)
        return array
    }

    private val colorArray = arrayOf(
        ColorRGBA(0, 0, 0),
        ColorRGBA(0, 0, 170),
        ColorRGBA(0, 170, 0),
        ColorRGBA(0, 170, 170),
        ColorRGBA(170, 0, 0),
        ColorRGBA(170, 0, 170),
        ColorRGBA(255, 170, 0),
        ColorRGBA(170, 170, 170),
        ColorRGBA(85, 85, 85),
        ColorRGBA(85, 85, 255),
        ColorRGBA(85, 255, 85),
        ColorRGBA(85, 255, 255),
        ColorRGBA(255, 85, 85),
        ColorRGBA(255, 85, 255),
        ColorRGBA(255, 255, 85),
        ColorRGBA(255, 255, 255)
    )

    private val Char.colorCode
        get() = when (val preCode = code) {
            in 48..57 -> preCode - 48
            in 97..102 -> preCode - 87
            else -> null
        }

    private fun Char.getColor(prev: ColorRGBA = ColorRGBA.WHITE): ColorRGBA? {
        return if (this.code == 'r'.code) prev
        else {
            val code = colorCode ?: return null
            colorArray.getOrNull(code)
        }
    }

    private data object SparseTextureArrayVertexFormat : VertexFormat(
        Element.Position2f, Element.Color, Element.Texture, Element.LayerIndex
    )

    private object RenderBuffer : VertexMode(
        SparseTextureArrayVertexFormat.attribute, Shader(
            "/assets/shader/port/SparseTextureArrayFontRenderer.vsh",
            "/assets/shader/port/SparseTextureArrayFontRenderer.fsh"
        ).apply { GL20.glUniform1i(getUniformLocation("tex"), 0) }
    ), AlwaysListening {
        init {
            handler<FenceSyncEvent> {
                onSync()
            }
        }

        fun putVertex(
            x: Float, y: Float,
            u: Float, v: Float,
            layer: Int,
            color: ColorRGBA
        ) {
            val pointer = arr.ptr
            pointer[0] = x
            pointer[4] = y
            pointer[8] = color.rgba
            pointer[12] = u
            pointer[16] = v
            pointer[20] = layer
            arr += 24
            vertexSize++
        }
    }
}

/**
 * By your self now
 */
//private val program = VFShader.fromSrc(
//    context,
//    """
//        #version 450 core
//
//        layout (location = 0) in vec2 position;
//        layout (location = 1) in vec4 vertColor;
//        layout (location = 2) in vec2 texCoords;
//        layout (location = 3) in int depth;
//
//        uniform mat4 matrix;
//
//        out vec4 color;
//        out vec2 uv;
//        out int z;
//
//        void main() {
//            gl_Position = matrix * vec4(position, 0.0, 1.0);
//            color = vertColor.abgr;
//            uv = texCoords;
//            z = depth;
//        }
//    """.trimIndent(),
//    """
//        #version 450 core
//
//        uniform sampler2DArray tex;
//
//        in vec4 color;
//        in vec2 uv;
//        flat in int z;
//
//        out vec4 FragColor;
//
//        void main() {
//            FragColor = color * texture(tex, vec3(uv, z));
//        }
//    """.trimIndent()
//).apply { GL20.glUniform1i(getUniformLocation("tex"), 0) }