package cum.xiaro.trollhack.gui.hudgui.elements.client

import cum.xiaro.trollhack.util.collections.ArrayMap
import cum.xiaro.trollhack.util.extension.sumOfFloat
import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.util.graphics.Easing
import cum.xiaro.trollhack.util.interfaces.DisplayEnum
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeParallelListener
import cum.xiaro.trollhack.gui.hudgui.HudElement
import cum.xiaro.trollhack.module.AbstractModule
import cum.xiaro.trollhack.module.ModuleManager
import cum.xiaro.trollhack.module.modules.client.Hud
import cum.xiaro.trollhack.util.TimeUnit
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.delegate.AsyncCachedValue
import cum.xiaro.trollhack.util.graphics.HAlign
import cum.xiaro.trollhack.util.graphics.RenderUtils2D
import cum.xiaro.trollhack.util.graphics.VAlign
import cum.xiaro.trollhack.util.graphics.color.ColorUtils
import cum.xiaro.trollhack.util.graphics.font.TextComponent
import cum.xiaro.trollhack.util.graphics.font.renderer.MainFontRenderer
import cum.xiaro.trollhack.util.math.vector.Vec2f
import cum.xiaro.trollhack.util.state.TimedFlag
import cum.xiaro.trollhack.util.text.format
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.text.TextFormatting
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

internal object ActiveModules : HudElement(
    name = "ActiveModules",
    category = Category.CLIENT,
    description = "List of enabled modules",
    enabledByDefault = true
) {
    private val mode by setting("Mode", Mode.LEFT_TAG)
    private val sortingMode by setting("Sorting Mode", SortingMode.LENGTH)
    private val showInvisible by setting("Show Invisible", false)
    private val bindOnly by setting("Bind Only", true, { !showInvisible })
    private val rainbow0 = setting("Rainbow", true)
    private val rainbow by rainbow0
    private val rainbowLength by setting("Rainbow Length", 10.0f, 1.0f..20.0f, 0.5f, rainbow0.atTrue())
    private val indexedHue by setting("Indexed Hue", 0.5f, 0.0f..1.0f, 0.05f, rainbow0.atTrue())
    private val saturation by setting("Saturation", 0.5f, 0.0f..1.0f, 0.01f, rainbow0.atTrue())
    private val brightness by setting("Brightness", 1.0f, 0.0f..1.0f, 0.01f, rainbow0.atTrue())
    private val frameColor by setting("Frame Color", ColorRGB(12, 16, 20, 127), true)

    private enum class Mode {
        LEFT_TAG,
        RIGHT_TAG,
        FRAME
    }

    @Suppress("UNUSED")
    private enum class SortingMode(
        override val displayName: CharSequence,
        val comparator: Comparator<AbstractModule>
    ) : DisplayEnum {
        LENGTH("Length", compareByDescending { it.textLine.getWidth() }),
        ALPHABET("Alphabet", compareBy { it.nameAsString }),
        CATEGORY("Category", compareBy { it.category.ordinal })
    }

    private var cacheWidth = 20.0f
    private var cacheHeight = 20.0f
    override val hudWidth: Float get() = cacheWidth
    override val hudHeight: Float get() = cacheHeight

    private val textLineMap = Int2ObjectOpenHashMap<TextComponent.TextLine>()
    private var lastSorted = CopyOnWriteArrayList(ModuleManager.modules)

    private val sortedModuleList by AsyncCachedValue(5L) {
        val modules = ModuleManager.modules
        if (modules.size != lastSorted.size) {
            lastSorted = CopyOnWriteArrayList(lastSorted)
        }

        lastSorted.sortWith(sortingMode.comparator)
        lastSorted
    }

    private var prevToggleMap = ArrayMap<ModuleToggleFlag>()
    private val toggleMap by AsyncCachedValue(1L, TimeUnit.SECONDS) {
        ArrayMap<ModuleToggleFlag>().apply {
            ModuleManager.modules.forEach {
                this[it.id] = prevToggleMap[it.id] ?: ModuleToggleFlag(it)
            }
            prevToggleMap = this
        }
    }

    init {
        safeParallelListener<TickEvent.Post> {
            for ((id, flag) in toggleMap) {
                flag.update()
                if (flag.progress <= 0.0f) continue
                textLineMap[id] = flag.module.newTextLine()
            }

            cacheWidth = sortedModuleList.maxOfOrNull {
                if (toggleMap[it.id]?.value == true) it.textLine.getWidth() + 4.0f
                else 20.0f
            }?.let {
                max(it, 20.0f)
            } ?: 20.0f

            cacheHeight = max(toggleMap.values.sumOfFloat { it.displayHeight }, 20.0f)
        }
    }

    override fun renderHud() {
        super.renderHud()
        GlStateManager.pushMatrix()

        GlStateManager.translate(width / scale * dockingH.multiplier, 0.0f, 0.0f)
        if (dockingV == VAlign.BOTTOM) {
            GlStateManager.translate(0.0f, height / scale - (MainFontRenderer.getHeight() + 2.0f), 0.0f)
        } else if (dockingV == VAlign.TOP) {
            GlStateManager.translate(0.0f, -1.0f, 0.0f)
        }

        if (dockingH == HAlign.LEFT) {
            GlStateManager.translate(-1.0f, 0.0f, 0.0f)
        }

        when (mode) {
            Mode.LEFT_TAG -> {
                if (dockingH == HAlign.LEFT) {
                    GlStateManager.translate(2.0f, 0.0f, 0.0f)
                }
            }
            Mode.RIGHT_TAG -> {
                if (dockingH == HAlign.RIGHT) {
                    GlStateManager.translate(-2.0f, 0.0f, 0.0f)
                }
            }
            else -> {
                // 0x22 cute catgirl owo
            }
        }

        drawModuleList()

        GlStateManager.popMatrix()
    }

    private fun drawModuleList() {
        if (rainbow) {
            val lengthMs = rainbowLength * 1000.0f
            val timedHue = System.currentTimeMillis() % lengthMs.toLong() / lengthMs
            var index = 0

            for (module in sortedModuleList) {
                val timedFlag = toggleMap[module.id] ?: continue
                val progress = timedFlag.progress

                if (progress <= 0.0f) continue

                GlStateManager.pushMatrix()

                val hue = timedHue + indexedHue * 0.05f * index
                val color = ColorUtils.hsbToRGB(hue, saturation, brightness)

                val textLine = module.textLine
                val textWidth = textLine.getWidth()
                val animationXOffset = textWidth * dockingH.offset * (1.0f - progress)
                val stringPosX = textWidth * dockingH.multiplier
                val margin = 2.0f * dockingH.offset

                var yOffset = timedFlag.displayHeight
                if (dockingV == VAlign.BOTTOM) yOffset *= -1.0f

                GlStateManager.translate(animationXOffset - margin - stringPosX, 0.0f, 0.0f)

                when (mode) {
                    Mode.LEFT_TAG -> {
                        RenderUtils2D.drawRectFilled(Vec2f(-2.0f, 0.0f), Vec2f(textWidth + 2.0f, yOffset), frameColor)
                        RenderUtils2D.drawRectFilled(Vec2f(-4.0f, 0.0f), Vec2f(-2.0f, yOffset), color)
                    }
                    Mode.RIGHT_TAG -> {
                        RenderUtils2D.drawRectFilled(Vec2f(-2.0f, 0.0f), Vec2f(textWidth + 2.0f, yOffset), frameColor)
                        RenderUtils2D.drawRectFilled(Vec2f(textWidth + 2.0f, 0.0f), Vec2f(textWidth + 4.0f, yOffset), color)
                    }
                    Mode.FRAME -> {
                        RenderUtils2D.drawRectFilled(Vec2f(-2.0f, 0.0f), Vec2f(textWidth + 2.0f, yOffset), frameColor)
                    }
                }

                module.newTextLine(color).drawLine(progress, HAlign.LEFT)
                GlStateManager.popMatrix()
                GlStateManager.translate(0.0f, yOffset, 0.0f)
                index++
            }
        } else {
            val color = secondaryColor
            for (module in sortedModuleList) {
                val timedFlag = toggleMap[module.id] ?: continue
                val progress = timedFlag.progress

                if (progress <= 0.0f) continue

                GlStateManager.pushMatrix()

                val textLine = module.textLine
                val textWidth = textLine.getWidth()
                val animationXOffset = textWidth * dockingH.offset * (1.0f - progress)
                val stringPosX = textWidth * dockingH.multiplier
                val margin = 2.0f * dockingH.offset

                var yOffset = timedFlag.displayHeight
                if (dockingV == VAlign.BOTTOM) yOffset *= -1.0f

                GlStateManager.translate(animationXOffset - margin - stringPosX, 0.0f, 0.0f)

                when (mode) {
                    Mode.LEFT_TAG -> {
                        RenderUtils2D.drawRectFilled(Vec2f(-2.0f, 0.0f), Vec2f(textWidth + 2.0f, yOffset), frameColor)
                        RenderUtils2D.drawRectFilled(Vec2f(-4.0f, 0.0f), Vec2f(-2.0f, yOffset), color)
                    }
                    Mode.RIGHT_TAG -> {
                        RenderUtils2D.drawRectFilled(Vec2f(-2.0f, 0.0f), Vec2f(textWidth + 2.0f, yOffset), frameColor)
                        RenderUtils2D.drawRectFilled(Vec2f(textWidth + 2.0f, 0.0f), Vec2f(textWidth + 4.0f, yOffset), color)
                    }
                    Mode.FRAME -> {
                        RenderUtils2D.drawRectFilled(Vec2f(-2.0f, 0.0f), Vec2f(textWidth + 2.0f, yOffset), frameColor)
                    }
                }

                textLine.drawLine(progress, HAlign.LEFT)
                GlStateManager.popMatrix()
                GlStateManager.translate(0.0f, yOffset, 0.0f)
            }
        }
    }

    private val AbstractModule.textLine
        get() = textLineMap.getOrPut(this.id) {
            this.newTextLine()
        }

    private fun AbstractModule.newTextLine(color: ColorRGB = Hud.secondaryColor) =
        TextComponent.TextLine(" ").apply {
            add(TextComponent.TextElement(nameAsString, color))
            getHudInfo().let {
                if (it.isNotBlank()) {
                    add(TextComponent.TextElement("${TextFormatting.GRAY format "["}${it}${TextFormatting.GRAY format "]"}", ColorRGB(255, 255, 255)))
                }
            }
            if (dockingH == HAlign.RIGHT) reverse()
        }

    private val TimedFlag<Boolean>.displayHeight
        get() = (MainFontRenderer.getHeight() + 2.0f) * progress

    private val TimedFlag<Boolean>.progress
        get() = if (value) {
            Easing.OUT_CUBIC.inc(Easing.toDelta(lastUpdateTime, 200L))
        } else {
            Easing.IN_CUBIC.dec(Easing.toDelta(lastUpdateTime, 200L))
        }

    private class ModuleToggleFlag(val module: AbstractModule) : TimedFlag<Boolean>(module.state) {
        fun update() {
            value = module.state
        }
    }

    private val AbstractModule.state: Boolean
        get() = this.isEnabled && (showInvisible || this.isVisible && (!bindOnly || !this.bind.value.isEmpty))

    init {
        relativePosX = -2.0f
        relativePosY = 2.0f
        dockingH = HAlign.RIGHT
    }

}