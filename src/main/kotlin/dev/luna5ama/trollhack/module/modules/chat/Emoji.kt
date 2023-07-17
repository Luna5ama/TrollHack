package dev.luna5ama.trollhack.module.modules.chat

import dev.fastmc.common.ceilToInt
import dev.luna5ama.trollhack.graphics.GlStateUtils
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.graphics.texture.MipmapTexture
import dev.luna5ama.trollhack.manager.managers.EmojiManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.client.CustomFont
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11.*

internal object Emoji : Module(
    name = "Emoji",
    description = "Add emojis to chat.",
    category = Category.CHAT
) {
    private val regex = ":(.+?):".toRegex()

    @JvmStatic
    fun renderText(
        inputText: String,
        fontHeight: Int,
        shadow: Boolean,
        posX: Float,
        posY: Float,
        alpha: Float
    ): String {
        var text = inputText
        val blend = glGetBoolean(GL_BLEND)
        val replacement = getReplacement(fontHeight)

        GlStateManager.color(1.0f, 1.0f, 1.0f, alpha)
        GlStateUtils.blend(true)
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE)

        for (result in regex.findAll(inputText)) {
            val emojiName = result.groupValues.getOrNull(1) ?: continue
            val texture = EmojiManager.getEmoji(emojiName) ?: continue
            val emojiText = result.value

            if (!shadow) {
                val index = text.indexOf(emojiText)
                if (index == -1) continue

                val x = getStringWidth(text.substring(0, index)) + fontHeight / 4
                drawEmoji(texture, (posX + x).toDouble(), posY.toDouble(), fontHeight.toFloat())
            }

            text = text.replaceFirst(emojiText, replacement)
        }

        GlStateUtils.blend(blend)
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        return text
    }

    @JvmStatic
    fun getStringWidthCustomFont(inputText: String): Int {
        var text = inputText
        val replacement = getReplacementCustomFont()

        for (result in regex.findAll(inputText)) {
            val emojiName = result.groupValues.getOrNull(1) ?: continue
            if (!EmojiManager.isEmoji(emojiName)) continue
            val emojiText = result.value
            text = text.replaceFirst(emojiText, replacement)
        }

        return MainFontRenderer.getWidth(text).ceilToInt()
    }

    @JvmStatic
    fun getStringWidth(inputWidth: Int, inputText: String, fontHeight: Int): Int {
        var reducedWidth = inputWidth
        val replacementWidth = getStringWidth(getReplacement(fontHeight))

        for (result in regex.findAll(inputText)) {
            val emojiName = result.groupValues.getOrNull(1) ?: continue
            if (!EmojiManager.isEmoji(emojiName)) continue
            val emojiText = result.value
            val emojiTextWidth = getStringWidth(emojiText)
            reducedWidth -= emojiTextWidth - replacementWidth
        }

        return reducedWidth
    }

    private fun getReplacementCustomFont(): String {
        val emojiWidth = (MainFontRenderer.getHeight() / MainFontRenderer.getWidth(' ')).ceilToInt()
        val spaces = CharArray(emojiWidth) { ' ' }
        return String(spaces)
    }

    private fun getReplacement(fontHeight: Int): String {
        val emojiWidth = (fontHeight / mc.fontRenderer.getCharWidth(' ').toFloat()).ceilToInt()
        val spaces = CharArray(emojiWidth) { ' ' }
        return String(spaces)
    }

    fun getStringWidth(text: String): Int {
        if (CustomFont.isEnabled && CustomFont.overrideMinecraft) {
            return getStringWidthCustomFont(text)
        }

        var i = 0
        var flag = false
        var j = 0

        while (j < text.length) {
            var c0 = text[j]
            var k: Int = mc.fontRenderer.getCharWidth(c0)
            if (k < 0 && j < text.length - 1) {
                ++j
                c0 = text[j]
                if (c0 != 'l' && c0 != 'L') {
                    if (c0 == 'r' || c0 == 'R') {
                        flag = false
                    }
                } else {
                    flag = true
                }
                k = 0
            }
            i += k
            if (flag && k > 0) {
                ++i
            }
            ++j
        }

        return i
    }

    /* This is created because vanilla one doesn't take double position input */
    private fun drawEmoji(texture: MipmapTexture, x: Double, y: Double, size: Float) {
        val tessellator = Tessellator.getInstance()
        val bufBuilder = tessellator.buffer

        texture.bindTexture()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)

        bufBuilder.begin(GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_TEX)
        bufBuilder.pos(x, y + size, 0.0).tex(0.0, 1.0).endVertex()
        bufBuilder.pos(x + size, y + size, 0.0).tex(1.0, 1.0).endVertex()
        bufBuilder.pos(x, y, 0.0).tex(0.0, 0.0).endVertex()
        bufBuilder.pos(x + size, y, 0.0).tex(1.0, 0.0).endVertex()

        tessellator.draw()
    }
}