package dev.luna5ama.trollhack.manager.managers

import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import dev.luna5ama.trollhack.RS
import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.manager.AbstractManager
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.Profiler
import dev.luna5ama.trollhack.graphics.font.ArrayedUnicodeFontRenderer
import dev.luna5ama.trollhack.graphics.font.FontRenderer
import dev.luna5ama.trollhack.graphics.font.UnicodeFontRenderer
import dev.luna5ama.trollhack.utils.threads.RenderThreadCoroutine

object UnicodeFontManager : AbstractManager(), AlwaysListening {
    val CURRENT_FONT: FontRenderer get() = ClientSettings.guiFont.font()
    lateinit var MSYAHEI_9: FontRenderer
    lateinit var MSYAHEI_12: FontRenderer
    lateinit var MSYAHEI_15: FontRenderer
    lateinit var MSYAHEI_20: FontRenderer
    lateinit var ICON_FONT: FontRenderer
    lateinit var LEGACY_9: FontRenderer
    lateinit var GENSHIN_9: FontRenderer
    lateinit var GENSHIN_11: FontRenderer
    lateinit var GENSHIN_18: FontRenderer

    override fun load(profilerScope: Profiler.ProfilerScope) {
        RenderSystem.assertOnRenderThread()
        MSYAHEI_9 = create("/assets/trollhack/MicrosoftYahei.ttf", 9f)
        MSYAHEI_12 = create("/assets/trollhack/MicrosoftYahei.ttf", 9f)
        MSYAHEI_15 = create("/assets/trollhack/MicrosoftYahei.ttf", 15f)
        MSYAHEI_20 = create("/assets/trollhack/MicrosoftYahei.ttf", 20f)
        ICON_FONT = create("/assets/trollhack/IconFont.ttf", 10f)
        LEGACY_9 = create("/assets/trollhack/LexendDeca-Regular.ttf", 9f)
        GENSHIN_9 = create("/assets/trollhack/Genshin.ttf", 9f, superSamplingLevel = 4)
        GENSHIN_11 = create("/assets/trollhack/Genshin.ttf", 11f)
        GENSHIN_18 = create("/assets/trollhack/Genshin.ttf", 18f, superSamplingLevel = 8)

    }

    private fun create(
        path: String, size: Float, antiAlias: Boolean = true,
        fractionalMetrics: Boolean = true, superSamplingLevel: Int = 4
    ): FontRenderer {
        val compatibility = RS.compatibility
        if (compatibility.intelGraphics || !compatibility.arbSparseTexture)
            return createCompatible(path, size, antiAlias, fractionalMetrics, superSamplingLevel)
        return createArrayed(path, size, antiAlias, fractionalMetrics, superSamplingLevel)
    }

    private fun createArrayed(
        path: String, size: Float, antiAlias: Boolean = true,
        fractionalMetrics: Boolean = true, superSamplingLevel: Int = 4
    ): ArrayedUnicodeFontRenderer {
        TrollHackMod.LOGGER.debug("Creating arrayed font")
        if (RenderSystem.isOnRenderThread()) {
            return ArrayedUnicodeFontRenderer.fromPath(
                path,
                size * superSamplingLevel,
                640 * (superSamplingLevel / 4.0).coerceIn(1.0, Double.MAX_VALUE).toInt(),
                128,
                1f / superSamplingLevel,
                antiAlias,
                fractionalMetrics,
                true
            )
        } else {
            return runBlocking {
                RenderThreadCoroutine.async {
                    ArrayedUnicodeFontRenderer.fromPath(
                        path,
                        size * superSamplingLevel,
                        640 * (superSamplingLevel / 4.0).coerceIn(1.0, Double.MAX_VALUE).toInt(),
                        128,
                        1f / superSamplingLevel,
                        antiAlias,
                        fractionalMetrics,
                        true
                    )
                }.await()
            }
        }
    }

    /** May be called out of render thread after initialization **/
    private fun createCompatible(
        path: String, size: Float, antiAlias: Boolean = true,
        fractionalMetrics: Boolean = true, superSamplingLevel: Int = 4
    ): UnicodeFontRenderer {
        if (RenderSystem.isOnRenderThread()) {
            return UnicodeFontRenderer.fromPath(
                path,
                size * superSamplingLevel,
                1200 * (superSamplingLevel / 4.0).coerceIn(1.0, Double.MAX_VALUE).toInt(),
                128,
                1f / superSamplingLevel,
                antiAlias,
                fractionalMetrics,
                true
            )
        } else {
            return runBlocking {
                RenderThreadCoroutine.async {
                    UnicodeFontRenderer.fromPath(
                        path,
                        size * superSamplingLevel,
                        1200 * (superSamplingLevel / 4.0).coerceIn(1.0, Double.MAX_VALUE).toInt(),
                        128,
                        1f / superSamplingLevel,
                        antiAlias,
                        fractionalMetrics,
                        true
                    )
                }.await()
            }
        }
    }

    enum class GuiFont(val font: () -> FontRenderer) : Displayable {
        PING_FANG({ MSYAHEI_9 }),
        GENSHIN({ GENSHIN_9 })
    }
}
