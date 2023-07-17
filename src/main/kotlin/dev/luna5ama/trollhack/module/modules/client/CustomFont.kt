package dev.luna5ama.trollhack.module.modules.client

import dev.fastmc.common.TimeUnit
import dev.fastmc.common.ceilToInt
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.graphics.font.GlyphCache
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.delegate.AsyncCachedValue
import dev.luna5ama.trollhack.util.threads.onMainThread
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.util.*

internal object CustomFont : Module(
    name = "Custom Font",
    description = "Use the better font instead of the stupid Minecraft font",
    visible = false,
    category = Category.CLIENT,
    alwaysEnabled = true
) {
    private const val DEFAULT_FONT_NAME = "Lexend Deca"

    val fontName = setting("Font", DEFAULT_FONT_NAME, consumer = { prev: String, value ->
        getMatchingFontName(value) ?: getMatchingFontName(prev) ?: DEFAULT_FONT_NAME
    })
    val overrideMinecraft by setting("Override Minecraft", true)
    private val sizeSetting = setting("Size", 1.0f, 0.5f..2.0f, 0.05f)
    private val charGapSetting = setting("Char Gap", 0.0f, -10f..10f, 0.5f)
    private val lineSpaceSetting = setting("Line Space", 0.0f, -10f..10f, 0.05f)
    private val baselineOffsetSetting = setting("Baseline Offset", 0.0f, -10.0f..10.0f, 0.05f)
    private val lodBiasSetting = setting("Lod Bias", 0.0f, -10.0f..10.0f, 0.05f)

    val isDefaultFont get() = fontName.value.equals(DEFAULT_FONT_NAME, true)
    val size get() = sizeSetting.value * 0.140625f
    val charGap get() = charGapSetting.value * 0.5f
    val lineSpace get() = size * (lineSpaceSetting.value * 0.05f + 0.75f)
    val lodBias get() = lodBiasSetting.value * 0.25f - 0.5375f
    val baselineOffset get() = baselineOffsetSetting.value * 2.0f - 9.5f

    init {
        listener<TickEvent.Post>(true) {
            mc.fontRenderer.FONT_HEIGHT = if (overrideMinecraft) {
                MainFontRenderer.getHeight().ceilToInt()
            } else {
                9
            }
        }
    }

    /** Available fonts on the system */
    val availableFonts: Map<String, String> by AsyncCachedValue(5L, TimeUnit.SECONDS) {
        HashMap<String, String>().apply {
            val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()

            environment.availableFontFamilyNames.forEach {
                this[it.lowercase(Locale.ROOT)] = it
            }

            environment.allFonts.forEach {
                val family = it.family
                if (family != Font.DIALOG) {
                    this[it.name.lowercase(Locale.ROOT)] = family
                }
            }
        }
    }

    private fun getMatchingFontName(name: String): String? {
        return if (name.equals(DEFAULT_FONT_NAME, true)) DEFAULT_FONT_NAME
        else availableFonts[name.lowercase(Locale.ROOT)]
    }

    init {
        fontName.valueListeners.add { prev, it ->
            if (prev == it) return@add
            GlyphCache.delete(Font(prev, Font.PLAIN, 64))
            onMainThread {
                MainFontRenderer.reloadFonts()
            }
        }
    }
}