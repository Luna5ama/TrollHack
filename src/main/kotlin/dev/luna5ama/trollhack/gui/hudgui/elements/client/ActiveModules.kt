package dev.luna5ama.trollhack.gui.hudgui.elements.client

import dev.fastmc.common.DoubleBuffered
import dev.fastmc.common.TimeUnit
import dev.fastmc.common.collection.FastIntMap
import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.common.sort.ObjectIntrosort
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.graphics.RenderUtils2D
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.color.ColorUtils
import dev.luna5ama.trollhack.graphics.font.TextComponent
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.gui.hudgui.HudElement
import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.module.ModuleManager
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.delegate.AsyncCachedValue
import dev.luna5ama.trollhack.util.delegate.FrameFloat
import dev.luna5ama.trollhack.util.extension.sumOfFloat
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.state.TimedFlag
import dev.luna5ama.trollhack.util.text.format
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.text.TextFormatting
import kotlin.math.max

internal object ActiveModules : HudElement(
    name = "Active Modules",
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

    private enum class Mode {
        LEFT_TAG,
        RIGHT_TAG,
        FRAME
    }

    @Suppress("UNUSED")
    private enum class SortingMode(
        override val displayName: CharSequence,
        val keySelector: (AbstractModule) -> Comparable<*>
    ) : DisplayEnum {
        LENGTH("Length", { -it.textLine.getWidth() }),
        ALPHABET("Alphabet", { it.nameAsString }),
        CATEGORY("Category", { it.category.ordinal })
    }

    override val hudWidth by FrameFloat {
        ModuleManager.modules.maxOfOrNull {
            if (toggleMap[it.id]?.value == true) it.textLine.getWidth() + 4.0f
            else 20.0f
        }?.let {
            max(it, 20.0f)
        } ?: 20.0f
    }

    override val hudHeight by FrameFloat {
        max(toggleMap.values.sumOfFloat { it.displayHeight }, 20.0f)
    }

    private val textLineMap = Int2ObjectOpenHashMap<TextComponent.TextLine>()
    private var lastSorted = makeKeyPair(ModuleManager.modules, null)

    private data class SortingPair(
        val module: AbstractModule,
        var key: Comparable<*> = sortingMode.keySelector(module)
    ) : Comparable<SortingPair> {
        fun update() {
            key = sortingMode.keySelector(module)
        }

        override fun compareTo(other: SortingPair): Int {
            @Suppress("UNCHECKED_CAST")
            return (key as Comparable<Comparable<*>>).compareTo(other.key)
        }
    }

    private fun makeKeyPair(modules: List<AbstractModule>, old: Array<SortingPair>?): Array<SortingPair> {
        if (old != null && modules.size == old.size) {
            return old
        }

        return Array(modules.size) {
            SortingPair(modules[it])
        }
    }

    private var prevToggleMap = FastIntMap<ModuleToggleFlag>()
    private val toggleMap by AsyncCachedValue(1L, TimeUnit.SECONDS) {
        FastIntMap<ModuleToggleFlag>().apply {
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
        }
    }

    override fun renderHud() {
        super.renderHud()
        GlStateManager.pushMatrix()

        GlStateManager.translate(width / scale * dockingH.multiplier, 0.0f, 0.0f)
        if (dockingV == dev.luna5ama.trollhack.graphics.VAlign.BOTTOM) {
            GlStateManager.translate(0.0f, height / scale - (MainFontRenderer.getHeight() + 2.0f), 0.0f)
        } else if (dockingV == dev.luna5ama.trollhack.graphics.VAlign.TOP) {
            GlStateManager.translate(0.0f, -1.0f, 0.0f)
        }

        if (dockingH == dev.luna5ama.trollhack.graphics.HAlign.LEFT) {
            GlStateManager.translate(-1.0f, 0.0f, 0.0f)
        }

        when (mode) {
            Mode.LEFT_TAG -> {
                if (dockingH == dev.luna5ama.trollhack.graphics.HAlign.LEFT) {
                    GlStateManager.translate(2.0f, 0.0f, 0.0f)
                }
            }
            Mode.RIGHT_TAG -> {
                if (dockingH == dev.luna5ama.trollhack.graphics.HAlign.RIGHT) {
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
        val sortArray = makeKeyPair(ModuleManager.modules, lastSorted)
        lastSorted = sortArray
        for (pair in sortArray) {
            pair.update()
        }
        ObjectIntrosort.sort(sortArray)

        if (rainbow) {
            val lengthMs = rainbowLength * 1000.0f
            val timedHue = System.currentTimeMillis() % lengthMs.toLong() / lengthMs
            var index = 0

            for (pair in sortArray) {
                val module = pair.module
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

                GlStateManager.translate(animationXOffset - margin - stringPosX, 0.0f, 0.0f)

                when (mode) {
                    Mode.LEFT_TAG -> {
                        RenderUtils2D.drawRectFilled(-2.0f, 0.0f, textWidth + 2.0f, yOffset, GuiSetting.backGround)
                        RenderUtils2D.drawRectFilled(-4.0f, 0.0f, -2.0f, yOffset, color)
                    }
                    Mode.RIGHT_TAG -> {
                        RenderUtils2D.drawRectFilled(-2.0f, 0.0f, textWidth + 2.0f, yOffset, GuiSetting.backGround)
                        RenderUtils2D.drawRectFilled(textWidth + 2.0f, 0.0f, textWidth + 4.0f, yOffset, color)
                    }
                    Mode.FRAME -> {
                        RenderUtils2D.drawRectFilled(-2.0f, 0.0f, textWidth + 2.0f, yOffset, GuiSetting.backGround)
                    }
                }

                module.newTextLine(color).drawLine(progress, dev.luna5ama.trollhack.graphics.HAlign.LEFT)

                if (dockingV == dev.luna5ama.trollhack.graphics.VAlign.BOTTOM) yOffset *= -1.0f
                GlStateManager.popMatrix()
                GlStateManager.translate(0.0f, yOffset, 0.0f)
                index++
            }
        } else {
            val color = GuiSetting.primary
            for (pair in sortArray) {
                val module = pair.module
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

                GlStateManager.translate(animationXOffset - margin - stringPosX, 0.0f, 0.0f)

                when (mode) {
                    Mode.LEFT_TAG -> {
                        RenderUtils2D.drawRectFilled(-2.0f, 0.0f, textWidth + 2.0f, yOffset, GuiSetting.backGround)
                        RenderUtils2D.drawRectFilled(-4.0f, 0.0f, -2.0f, yOffset, color)
                    }
                    Mode.RIGHT_TAG -> {
                        RenderUtils2D.drawRectFilled(-2.0f, 0.0f, textWidth + 2.0f, yOffset, GuiSetting.backGround)
                        RenderUtils2D.drawRectFilled(textWidth + 2.0f, 0.0f, textWidth + 4.0f, yOffset, color)
                    }
                    Mode.FRAME -> {
                        RenderUtils2D.drawRectFilled(-2.0f, 0.0f, textWidth + 2.0f, yOffset, GuiSetting.backGround)
                    }
                }

                textLine.drawLine(progress, dev.luna5ama.trollhack.graphics.HAlign.LEFT)

                if (dockingV == dev.luna5ama.trollhack.graphics.VAlign.BOTTOM) yOffset *= -1.0f
                GlStateManager.popMatrix()
                GlStateManager.translate(0.0f, yOffset, 0.0f)
            }
        }
    }

    private val AbstractModule.textLine
        get() = textLineMap.getOrPut(this.id) {
            this.newTextLine()
        }

    private fun AbstractModule.newTextLine(color: ColorRGB = GuiSetting.primary) =
        TextComponent.TextLine(" ").apply {
            add(TextComponent.TextElement(nameAsString, color))
            getHudInfo().let {
                if (it.isNotBlank()) {
                    add(
                        TextComponent.TextElement(
                            "${TextFormatting.GRAY format "["}${it}${TextFormatting.GRAY format "]"}",
                            ColorRGB(255, 255, 255)
                        )
                    )
                }
            }
            if (dockingH == dev.luna5ama.trollhack.graphics.HAlign.RIGHT) reverse()
        }

    private val TimedFlag<Boolean>.displayHeight
        get() = (MainFontRenderer.getHeight() + 2.0f) * progress

    private val TimedFlag<Boolean>.progress
        get() = if (value) {
            Easing.OUT_CUBIC.inc(Easing.toDelta(lastUpdateTime, 300L))
        } else {
            Easing.IN_CUBIC.dec(Easing.toDelta(lastUpdateTime, 300L))
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
        dockingH = dev.luna5ama.trollhack.graphics.HAlign.RIGHT
    }
}