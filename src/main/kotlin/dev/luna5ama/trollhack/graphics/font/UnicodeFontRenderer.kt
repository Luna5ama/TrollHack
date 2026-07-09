package dev.luna5ama.trollhack.graphics.font

import imgui.ImFont
import imgui.ImFontConfig
import imgui.ImGui
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import dev.luna5ama.trollhack.gui.NullClickGui
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.MinecraftWrapper
import dev.luna5ama.trollhack.utils.extension.getValue
import dev.luna5ama.trollhack.utils.extension.setValue
import dev.luna5ama.trollhack.graphics.GLHelper
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects.draw
import dev.luna5ama.trollhack.graphics.color.ColorGradient
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.matrix.scalef
import dev.luna5ama.trollhack.graphics.matrix.scope
import dev.luna5ama.trollhack.graphics.matrix.translatef
import dev.luna5ama.trollhack.graphics.texture.MipmapTexture
import dev.luna5ama.trollhack.graphics.texture.Texture
import dev.luna5ama.trollhack.graphics.texture.drawTexture
import dev.luna5ama.trollhack.graphics.texture.useTexture
import org.lwjgl.opengl.GL14.*
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints.*
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min
import dev.luna5ama.trollhack.RenderSystem as RS


/**
 * @param chunkSize must be in 1..65536
 */
class UnicodeFontRenderer private constructor(
    private val font: Font,
    val bytes: ByteArray,
    size: Float,
    private val antiAlias: Boolean,
    private val fractionalMetrics: Boolean,
    private val imgSize: Int,
    private val chunkSize: Int,
    private val linearMag: Boolean,
    private var scaleFactor: Float
) : FontRenderer("${font.fontName}+$size", true, true, size.toDouble()) {
    private var height0 = 0
    private var charDataArray = Array<CharData?>(65536) { null }
    private val scaledOffset = 4 * size / 25f
    private var atlas = Array<MipmapTexture?>(65536 / chunkSize) { null }
    private val badChunks = IntArray(65536 / chunkSize) { 0 }
    private val colorCode = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'r')
    private val stringDataArray = Int2ObjectOpenHashMap<StringData>()
    private val shadowedStringArray = Int2ObjectOpenHashMap<StringData>()
    override var disableCache by AtomicBoolean(false); private set
    override val height get() = height0 * scaleFactor
    override val statistics = Statistics(chunkSize, imgSize)

    init {
        require(65536 % chunkSize == 0)
        fontsCache.add(this)
        repeat(4) { initChunk(it) }
    }

    override fun cache(value: Boolean) {
        disableCache = !value
    }

    private fun initShadowedString(string: String): StringData {
        val width = (getWidth0(string) * 1.2).toInt()
        val height = (height0 * 1.2).toInt()
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = (img.graphics as Graphics2D).apply {
            font = this@UnicodeFontRenderer.font
            color = Color(255, 255, 255, 0)
            fillRect(0, 0, imgSize, imgSize)
            color = Color.WHITE
            setRenderingHint(KEY_FRACTIONALMETRICS, if (fractionalMetrics) VALUE_FRACTIONALMETRICS_ON else VALUE_FRACTIONALMETRICS_OFF)
            setRenderingHint(KEY_TEXT_ANTIALIASING, if (antiAlias) VALUE_TEXT_ANTIALIAS_ON else VALUE_TEXT_ANTIALIAS_OFF)
            setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF)
            setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC)
            setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY)
        }
        val metrics = graphics.fontMetrics
        var charHeight = 0
        var posX = scaledOffset
        val posY = 1

        // get the shape object
        val textShape = TextLayout(string, graphics.font, graphics.fontRenderContext).getOutline(
            AffineTransform.getTranslateInstance(posX.toDouble(), (posY + metrics.ascent).toDouble())
        )
        val dimension = metrics.getStringBounds(string, graphics)
        posX += dimension.bounds.width
        charHeight = max(dimension.bounds.height, charHeight)
        graphics.apply {
            val originalStroke = stroke
            color = ColorRGBA.BLACK.alpha(140).awt
            stroke = BasicStroke(2.0f)
            draw(textShape)
            color = ColorRGBA.BLACK.alpha(50).awt
            stroke = BasicStroke(4.0f)
            draw(textShape)
            color = Color.WHITE
            stroke = originalStroke
            fill(textShape)
        }
        val texture = MipmapTexture(img, GL_RGBA).apply {
            bindTexture()
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0f)
            if (!linearMag) glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            unbindTexture()
        }
        // convert to standard device coordination
        return StringData(
            (scaledOffset) / width,
            posY / height.toFloat(),
            (scaledOffset + posX) / width,
            (posY + charHeight) / height.toFloat(),
            posX.toInt(),
            posY + charHeight,
            texture
        )
    }

    private fun initString(string: String): StringData {
        val width = (getWidth0(string) * 1.2).toInt()
        val height = (height0 * 1.2).toInt()
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = (img.graphics as Graphics2D).apply {
            font = this@UnicodeFontRenderer.font
            color = Color(255, 255, 255, 0)
            fillRect(0, 0, imgSize, imgSize)
            color = Color.WHITE
            setRenderingHint(KEY_FRACTIONALMETRICS, if (fractionalMetrics) VALUE_FRACTIONALMETRICS_ON else VALUE_FRACTIONALMETRICS_OFF)
            setRenderingHint(KEY_TEXT_ANTIALIASING, if (antiAlias) VALUE_TEXT_ANTIALIAS_ON else VALUE_TEXT_ANTIALIAS_OFF)
            setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF)
            setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC)
            setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY)
        }
        val metrics = graphics.fontMetrics
        var charHeight = 0
        var posX = scaledOffset
        val posY = 1
        string.forEach { char ->
            val dimension = metrics.getStringBounds(String(charArrayOf(char)), graphics)
            if (dimension.bounds.height > charHeight) {
                charHeight = dimension.bounds.height
                if (charHeight > height0) height0 = charHeight
            }
            val imgWidth = dimension.bounds.width
            graphics.drawString(String(charArrayOf(char)), posX, (posY + metrics.ascent).toFloat() - 2.0f)
            posX += imgWidth
        }
        val texture = MipmapTexture(img, GL_RGBA).apply {
            bindTexture()
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0f)
            if (!linearMag) glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            unbindTexture()
        }
        // convert to standard device coordination
        return StringData(
            (scaledOffset) / width,
            posY / height.toFloat(),
            (scaledOffset + posX) / width,
            (posY + charHeight) / height.toFloat(),
            posX.toInt(),
            posY + charHeight,
            texture
        )
    }

    private fun initChunk(chunk: Int): MipmapTexture {
        val img = BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB)
        val graphics = (img.graphics as Graphics2D).apply {
            font = this@UnicodeFontRenderer.font
            color = Color(255, 255, 255, 0)
            fillRect(0, 0, imgSize, imgSize)
            color = Color.WHITE
            setRenderingHint(KEY_FRACTIONALMETRICS, if (fractionalMetrics) VALUE_FRACTIONALMETRICS_ON else VALUE_FRACTIONALMETRICS_OFF)
            setRenderingHint(KEY_TEXT_ANTIALIASING, if (antiAlias) VALUE_TEXT_ANTIALIAS_ON else VALUE_TEXT_ANTIALIAS_OFF)
            setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF)
            setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC)
            setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY)
        }
        val metrics = graphics.fontMetrics
        var charHeight = 0
        var posX = 0
        var posY = 1
        (0..<chunkSize).forEach { index ->
            // char size
            val dimension = metrics.getStringBounds(String(charArrayOf((chunk * chunkSize + index).toChar())), graphics)
            val charData = CharData(dimension.bounds.width, dimension.bounds.height)
            val charWidth = charData.width + scaledOffset * 2
            if (charData.height > charHeight) {
                charHeight = charData.height
                if (charHeight > height0) height0 = charHeight
            }
            if (posX + charWidth > imgSize) {
                posX = 0
                posY += charHeight
            }
            val fImgSize = imgSize.toFloat()
            charData.apply {
                u = (posX + scaledOffset) / fImgSize
                v = posY / fImgSize
                u1 = (posX + scaledOffset + charData.width) / fImgSize
                v1 = (posY + charData.height) / fImgSize
            }
            charDataArray[chunk * chunkSize + index] = charData
            graphics.drawString(
                (chunk * chunkSize + index).toChar().toString(),
                posX + scaledOffset,
                (posY + metrics.ascent).toFloat()
            )
            posX += charWidth.toInt()
        }
        val texture = MipmapTexture(img, GL_RGBA).apply {
            bindTexture()
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0f)
            if (!linearMag) glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            unbindTexture()
        }
        atlas[chunk] = texture
        return texture
    }

    override fun drawAtlas(index: Int) {
        val atlasTexture = atlas[index] ?: return
        atlasTexture.drawTexture(0f, 0f, imgSize.toFloat(), imgSize.toFloat())
    }

    override fun drawStaticString(string: String) {
        val atlasTexture = stringDataArray[string.hashCode()]?.texture ?: return
        atlasTexture.drawTexture(0f, 0f, atlasTexture.width.toFloat(), atlasTexture.height.toFloat())
    }

    override fun drawShadowedStaticString(string: String) {
        val atlasTexture = shadowedStringArray[string.hashCode()]?.texture ?: return
        atlasTexture.drawTexture(0f, 0f, atlasTexture.width.toFloat(), atlasTexture.height.toFloat())
    }

    override fun setScale(scale: Float) {
        this.apply { scaleFactor = scale }
    }

    override fun getHeight(scale: Float) = height * scale

    override fun getWidth(text: String) = getWidth0(text) * scaleFactor

    override fun getWidth(text: String, scale: Float) = getWidth(text) * scale

    private fun getWidth0(text: String): Int {
        var sum = 0
        var shouldSkip = false
        text.forEachIndexed { index, c ->
            if (shouldSkip) {
                shouldSkip = false
                return@forEachIndexed
            }
            val chunk = c.code / chunkSize // TODO
            if (badChunks[chunk] == 1) return@forEachIndexed
            if (atlas.size <= chunk) return@forEachIndexed
            if (atlas[chunk] == null) {
                try {
                    val newTexture = initChunk(chunk)
                    atlas[chunk] = newTexture
                } catch (_: Exception) {
                    badChunks[chunk] = 1
                    return@forEachIndexed
                }
            }
            var delta = 0
            val data = charDataArray[c.code]
            if (data != null) delta = data.width
            if (c == '§' || c == '&') {
                if (index + 1 < text.length) {
                    val next = text[index + 1]
                    run break1@{
                        colorCode.forEach { c1 ->
                            if (next == c1) {
                                shouldSkip = true
                                return@break1
                            }
                        }
                    }
                }
            } else sum += delta
        }
        return sum
    }

    override fun drawString(text: String, color: ColorRGBA) = drawString0(text, 0f, 0f, color.awt, 1f, false)

    override fun drawString(text: String, x: Float, y: Float) = drawString0(text, x, y, Color.WHITE, 1f, false)

    override fun drawString(text: String, x: Float, y: Float, color: Int) = drawString0(text, x, y, Color(color), 1f, false)

    override fun drawString(text: String, x: Float, y: Float, color: Color) = drawString0(text, x, y, color, 1f, false)

    override fun drawString(text: String, x: Float, y: Float, color: Color, scale: Float) = drawString0(text, x, y, color, scale, false)

    override fun drawStringWithShadow(text: String, x: Float, y: Float) {
        drawString0(text, x + 1f, y + 1f, Color.WHITE, 1f, true)
        drawString0(text, x, y, Color.WHITE, 1f, false)
    }

    override fun drawStringWithShadow(text: String, x: Float, y: Float, color: ColorRGBA) = drawStringWithShadow(text, x, y, color.awt)

    override fun drawStringWithShadow(text: String, color: ColorRGBA) = drawStringWithShadow(text, 0f, 0f, color)

    override fun drawStringWithShadow(text: String, x: Float, y: Float, color: Color) {
        drawString0(text, x + 1f, y + 1f, color, 1f, true)
        drawString0(text, x, y, color, 1f, false)
    }

    override fun drawStringWithShadow(text: String, x: Float, y: Float, color: Color, scale: Float) {
        drawString0(text, x + 1f, y + 1f, color, scale, true)
        drawString0(text, x, y, color, scale, false)
    }

    override fun drawStringWithShadow(text: String, x: Float, y: Float, shadowDepth: Float) {
        drawString0(text, x + shadowDepth, y + shadowDepth, Color.WHITE, 1f, true)
        drawString0(text, x, y, Color.WHITE, 1f, false)
    }

    override fun drawStringWithShadow(text: String, x: Float, y: Float, shadowDepth: Float, color: Color) {
        drawString0(text, x + shadowDepth, y + shadowDepth, color, 1f, true)
        drawString0(text, x, y, color, 1f, false)
    }

    override fun drawStringWithShadow(text: String, x: Float, y: Float, shadowDepth: Float, color: Color, scale: Float) {
        drawString0(text, x + shadowDepth, y + shadowDepth, color, scale, true)
        drawString0(text, x, y, color, scale, false)
    }

    override fun drawGradientText(text: String, x: Float, y: Float, colors: Array<Color>) {
        drawString1(text, x, y, colors, 1f)
    }

    override fun drawGradientText(text: String, x: Float, y: Float, colors: Array<Color>, scale: Float) {
        drawString1(text, x, y, colors, scale)
    }

    override fun drawGradientTextWithShadow(text: String, x: Float, y: Float, colors: Array<Color>) {
        drawString2(text, x + 1f, y + 1f, colors[0], 1f, true)
        drawString1(text, x, y, colors, 1f)
    }

    override fun drawGradientTextWithShadow(text: String, x: Float, y: Float, colors: Array<Color>, scale: Float) {
        drawString2(text, x + 1f, y + 1f, colors[0], scale, true)
        drawString1(text, x, y, colors, scale)
    }

    override fun drawGradientTextWithShadow(text: String, x: Float, y: Float, shadowDepth: Float, colors: Array<Color>) {
        drawString2(text, x + shadowDepth, y + shadowDepth, colors[0], 1f, true)
        drawString1(text, x, y, colors, 1f)
    }

    override fun drawGradientTextWithShadow(
        text: String,
        x: Float,
        y: Float,
        shadowDepth: Float,
        colors: Array<Color>,
        scale: Float
    ) {
        drawString2(text, x + shadowDepth, y + shadowDepth, colors[0], scale, true)
        drawString1(text, x, y, colors, scale)
    }

    private fun drawString2(text: String, x: Float, y: Float, color0: Color, scale0: Float, shadow: Boolean) {
        drawString0(text, x, y, color0, false, arrayOf(), scale0, false, shadow)
    }

    private fun drawString1(text: String, x: Float, y: Float, colors: Array<Color>, scale0: Float) {
        drawString0(text, x, y, colors[0], true, colors, scale0, false, false)
    }

    override fun drawString0(
        text0: String,
        x: Float, y: Float,
        color0: Color, gradient: Boolean, colors: Array<Color>,
        scale0: Float, loop: Boolean, shadow: Boolean
    ) {
        glActiveTexture(GL_TEXTURE0)
        RS.matrixLayer.scope {
            if (text0.isEmpty()) return
            var text = text0
            if (text.length > 1024) text = "Too long..."
            val alpha = color0.alpha
            val shadowColor = Color(0, 0, 0, min(alpha, 128))
            GLHelper.blend = true
            val stops = ColorGradient(
                *colors.mapIndexed { index, color -> ColorGradient.Stop(index.toFloat(), ColorRGBA(color)) }.toTypedArray()
            )
            val maxValue = colors.size - 1

            var startX = x
            var startY = y
            var currentColor = color0
            val scale = scale0 * scaleFactor
            if (scale != 1f) {
                translatef(x, y, 0f)
                scalef(scale, scale, 1f)
                startX = 0f
                startY = 0f
            }
            var chunk = -1
            var shouldSkip = false
            var colorStep = 0f
            var lastTexture: Texture? = null
            text.forEachIndexed { index, c ->
                if (shouldSkip) {
                    shouldSkip = false
                    return@forEachIndexed
                }
                if (c == '\n') {
                    startY += height
                    startX = x
                    return@forEachIndexed
                }
                if (c == '§' || c == '&') {
                    if (index + 1 < text.length) {
                        val next: Char = text[index + 1]
                        val newColor = getColor(next, color0)
                        if (newColor != null) {
                            if (!gradient) currentColor = Color(newColor.red, newColor.green, newColor.blue, alpha)
                            shouldSkip = true
                            return@forEachIndexed
                        }
                    }
                }

                val currentChunk = c.code / chunkSize
                if (currentChunk != chunk || lastTexture == null) {
                    val tex: Texture
                    chunk = currentChunk
                    val texture = atlas[chunk]
                    if (texture == null) {
                        // If this is a bad chunk then we skip it
                        if (badChunks[chunk] == 1) return@forEachIndexed
                        var newTexture: MipmapTexture? = null
                        try {
                            newTexture = initChunk(chunk)
                        } catch (ignore: Exception) {
                            badChunks[chunk] = 1
                        }
                        if (newTexture == null) {
                            return@forEachIndexed
                        } else {
                            atlas[chunk] = newTexture
                            tex = newTexture
                        }
                    } else tex = texture
                    if (lastTexture != tex) {
                        lastTexture = tex
                        tex.bindTexture()
                    }
                }

                val data = if (c.code >= charDataArray.size || charDataArray[c.code] == null) return@forEachIndexed
                else charDataArray[c.code]!!
                val endX = startX + data.width
                val endY = startY + data.height
                colorStep += data.width

                val leftColor = if (shadow) shadowColor else currentColor
                var rightColor = leftColor

                if (gradient && !shadow && !loop) rightColor = stops.get((colorStep / getWidth0(text)) * maxValue).awt
                else if (gradient && !shadow) rightColor = colors[index % colors.size]

                GL_TRIANGLE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
                    universal(endX, startY, data.u1, data.v, ColorRGBA(rightColor))
                    universal(startX, startY, data.u, data.v, ColorRGBA(leftColor))
                    universal(endX, endY, data.u1, data.v1, ColorRGBA(rightColor))
                    universal(startX, endY, data.u, data.v1, ColorRGBA(leftColor))
                }

                startX = endX
                currentColor = rightColor
            }
        }
    }

    override fun drawStringShadowed0(text0: String, x: Float, y: Float, color0: Color, scale0: Float) {
        glActiveTexture(GL_TEXTURE0)
        RS.matrixLayer.scope {
            var text = text0
            if (text.length > 1024) text = "Too long..."
            GLHelper.blend = true
            // region render
            var startX = x
            var startY = y
            val scale = scale0 * scaleFactor
            if (scale != 1f) {
                translatef(x, y, 0f)
                scalef(scale, scale, 1f)
                startX = 0f
                startY = 0f
            }
            if (text.length > 1) {
                val hashcode = text.hashCode()
                val data = shadowedStringArray.computeIfAbsent(hashcode, Int2ObjectFunction {
                    initShadowedString(text)
                })
                val endX = startX + data.width
                val endY = startY + data.height
                data.texture.bindTexture()
                GL_TRIANGLE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
                    universal(startX, startY, data.u, data.v, ColorRGBA(color0))
                    universal(startX, endY, data.u, data.v1, ColorRGBA(color0))
                    universal(endX, startY, data.u1, data.v, ColorRGBA(color0))
                    universal(endX, endY, data.u1, data.v1, ColorRGBA(color0))
                }
            }
            // endregion
        }
    }

    override fun drawString0(text0: String, x: Float, y: Float, color0: Color, scale0: Float, shadow: Boolean) {
        GLHelper.blend = true
        glActiveTexture(GL_TEXTURE0)
        RS.matrixLayer.scope {
            var text = text0
            if (text.length > 1024) text = "Too long..."
            val alpha = color0.alpha
            val shadowColor = Color(0, 0, 0, min(alpha, 128))
            // region render
            var startX = x
            var startY = y
            var currentColor = color0
            val scale = scale0 * scaleFactor
            if (scale != 1f) {
                translatef(x, y, 0f)
                scalef(scale, scale, 1f)
                startX = 0f
                startY = 0f
            }
            var chunk = Int.MIN_VALUE
            var shouldSkip = false
            if (!disableCache && ClientSettings.staticGlyphCache
                && (MinecraftWrapper.mc.screen == NullClickGui || !ClientSettings.onlyInClickGui)
                && '§' !in text && text.length > 1) {
                val hashcode = text.hashCode()
                val data = stringDataArray.computeIfAbsent(hashcode, Int2ObjectFunction {
                    initString(text)
                })
                val renderColor = if (shadow) shadowColor else currentColor
                val endX = startX + data.width
                val endY = startY + data.height
                data.texture.useTexture {
                    GL_TRIANGLE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
                        universal(endX, startY, data.u1, data.v, ColorRGBA(renderColor))
                        universal(startX, startY, data.u, data.v, ColorRGBA(renderColor))
                        universal(endX, endY, data.u1, data.v1, ColorRGBA(renderColor))
                        universal(startX, endY, data.u, data.v1, ColorRGBA(renderColor))
                    }
                }
            } else {
                glActiveTexture(GL_TEXTURE0)
                var firstChar = true
                var currentTexture: MipmapTexture? = null
                text.forEachIndexed { index, c ->
                    if (shouldSkip) {
                        shouldSkip = false
                        return@forEachIndexed
                    }
                    when (c) {
                        // next line
                        '\n' -> {
                            startY += height
                            startX = x
                            return@forEachIndexed
                        }
                        // skip color code
                        '§', '&' -> {
                            if (index + 1 < text.length) {
                                val next = text[index + 1]
                                //Color
                                val newColor = getColor(next, color0)
                                if (newColor != null) {
                                    currentColor = Color(newColor.red, newColor.green, newColor.blue, alpha)
                                    shouldSkip = true
                                    return@forEachIndexed
                                }
                            }
                        }
                    }
                    val currentChunk = c.code / chunkSize
                    if (firstChar) require(currentChunk != chunk)
                    if (currentChunk != chunk || currentTexture == null) {
                        val tex: Texture
                        chunk = currentChunk
                        val texture = atlas[chunk]
                        if (texture == null) {
                            // If this is a bad chunk then we skip it
                            if (badChunks[chunk] == 1) return@forEachIndexed
                            var newTexture: MipmapTexture? = null
                            try {
                                newTexture = initChunk(chunk)
                            } catch (ignore: Exception) {
                                badChunks[chunk] = 1
                            }
                            if (newTexture == null) {
                                return@forEachIndexed
                            } else {
                                atlas[chunk] = newTexture
                                tex = newTexture
                            }
                        } else tex = texture
                        if (currentTexture != tex) {
                            currentTexture = tex
                            tex.bindTexture()
                        }
                    }
                    val data =
                        if (c.code >= charDataArray.size) return@forEachIndexed
                        else charDataArray[c.code] ?: return@forEachIndexed
                    val renderColor = if (shadow) shadowColor else currentColor
                    val endX = startX + data.width
                    val endY = startY + data.height
                    GL_TRIANGLE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
                        universal(endX, startY, data.u1, data.v, ColorRGBA(renderColor))
                        universal(startX, startY, data.u, data.v, ColorRGBA(renderColor))
                        universal(endX, endY, data.u1, data.v1, ColorRGBA(renderColor))
                        universal(startX, endY, data.u, data.v1, ColorRGBA(renderColor))
                    }
                    startX = endX
                    firstChar = false
                }
            }
            // endregion
        }
    }

    private fun getColor(colorCode: Char, prev: Color): Color? = when (colorCode) {
        '0' -> Color(0, 0, 0)
        '1' -> Color(0, 0, 170)
        '2' -> Color(0, 170, 0)
        '3' -> Color(0, 170, 170)
        '4' -> Color(170, 0, 0)
        '5' -> Color(170, 0, 170)
        '6' -> Color(255, 170, 0)
        '7' -> Color(170, 170, 170)
        '8' -> Color(85, 85, 85)
        '9' -> Color(85, 85, 255)
        'a' -> Color(85, 255, 85)
        'b' -> Color(85, 255, 255)
        'c' -> Color(255, 85, 85)
        'd' -> Color(255, 85, 255)
        'e' -> Color(255, 255, 85)
        'f' -> Color(255, 255, 255)
        'r' -> prev
        else -> null
    }

    override fun refresh() {
        atlas.forEach { it?.deleteTexture() }
        stringDataArray.forEach { (_, data) -> data.texture.deleteTexture() }
        shadowedStringArray.forEach { (_, data) -> data.texture.deleteTexture() }
        charDataArray = Array(65536) { null }
        atlas = Array(65536 / chunkSize) { null }
        (0..<(256 / chunkSize)).forEach { initChunk(it) }
        stringDataArray.clear()
        shadowedStringArray.clear()
    }

    data class CharData(
        val width: Int,
        val height: Int,
        var u: Float = 0f,
        var v: Float = 0f,
        var u1: Float = 0f,
        var v1: Float = 0f
    )

    data class StringData(
        var u: Float = 0f,
        var v: Float = 0f,
        var u1: Float = 0f,
        var v1: Float = 0f,
        val width: Int,
        val height: Int,
        val texture: MipmapTexture
    )

    companion object {
        val fontsCache = mutableListOf<UnicodeFontRenderer>()

        fun refresh() {
            fontsCache.forEach(UnicodeFontRenderer::refresh)
        }

        fun fromPath(path: String, size: Float, antiAlias: Boolean, fractionalMetrics: Boolean) =
            fromPath(path, size, 512, 64, 1f, antiAlias, fractionalMetrics)

        fun fromPath(path: String, size: Float, imgSize: Int, chunkSize: Int,
                     scaleFactor: Float, antiAlias: Boolean, fractionalMetrics: Boolean) =
            fromPath(path, size, imgSize, chunkSize, scaleFactor, antiAlias, fractionalMetrics, true)

        fun fromPath(path: String, size: Float, imgSize: Int, chunkSize: Int,
                     scaleFactor: Float, antiAlias: Boolean, fractionalMetrics: Boolean, linearMag: Boolean) =
            fromStream(TrollHackMod::class.java.getResourceAsStream(path)!!, size, imgSize, chunkSize,
                scaleFactor, antiAlias, fractionalMetrics, linearMag)

        fun fromFile(file: File, size: Float, antiAlias: Boolean, fractionalMetrics: Boolean) =
            fromFile(file, size, 512, 64, 1f, antiAlias, fractionalMetrics)

        fun fromFile(file: File, size: Float, imgSize: Int, chunkSize: Int,
                     scaleFactor: Float, antiAlias: Boolean, fractionalMetrics: Boolean) =
            fromStream(ByteArrayInputStream(file.readBytes()), size, imgSize, chunkSize,
                scaleFactor, antiAlias, fractionalMetrics)

        fun fromStream(stream: InputStream, size: Float, imgSize: Int, chunkSize: Int,
                       scaleFactor: Float, antiAlias: Boolean, fractionalMetrics: Boolean) =
            fromStream(stream, size, imgSize, chunkSize, scaleFactor, antiAlias, fractionalMetrics, true)

        fun fromStream(stream: InputStream, size: Float, imgSize: Int, chunkSize: Int,
                       scaleFactor: Float, antiAlias: Boolean,
                       fractionalMetrics: Boolean, linearMag: Boolean): UnicodeFontRenderer {
            return try {
                val data = stream.readBytes()
                val font = Font.createFont(Font.TRUETYPE_FONT, ByteArrayInputStream(data))
                UnicodeFontRenderer(font.deriveFont(size).deriveFont(Font.PLAIN), data, size, antiAlias,
                    fractionalMetrics, imgSize, chunkSize, linearMag, scaleFactor)
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }
        }
    }

    private var imGuiInitialized = false
    override lateinit var imGuiFont: ImFont

    override fun initForImGui() {
        if (imGuiInitialized) return
        val io = ImGui.getIO()
        val config = ImFontConfig()
        config.fontDataOwnedByAtlas = false
        config.setName(font.name.toCharArray().joinToString("", limit = 35).trim())
        require(bytes.isNotEmpty())
        TrollHackMod.LOGGER.info("Initializing font ${font.name} for ImGui, data size: ${bytes.size}, font size: ${height * 1.3f}px")
        imGuiFont = io.fonts.addFontFromMemoryTTF(bytes, height * 1.3f, config, io.fonts.glyphRangesChineseFull)
        config.destroy()
    }

    override fun drawText0(text: CharSequence, x: Double, y: Double, color: ColorRGBA, shadow: Boolean) {
        if (shadow) drawStringWithShadow(text.toString(), x.toFloat(), y.toFloat(), color.awt)
        else drawString(text.toString(), x.toFloat(), y.toFloat(), color.awt)
    }
}