package dev.luna5ama.trollhack.gui.hud.impl

import dev.fastmc.common.sort.ObjectIntrosort
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import dev.luna5ama.trollhack.RS
import dev.luna5ama.trollhack.event.api.nonNullParallelHandler
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.gui.HudModule
import dev.luna5ama.trollhack.manager.managers.GuiManager
import dev.luna5ama.trollhack.manager.managers.ModuleManager
import dev.luna5ama.trollhack.manager.managers.UnicodeFontManager
import dev.luna5ama.trollhack.modules.AbstractModule
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.impl.client.Colors
import dev.luna5ama.trollhack.utils.ChatUtils
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.delegates.AsyncCachedValue
import dev.luna5ama.trollhack.graphics.animations.Easing
import dev.luna5ama.trollhack.graphics.buffer.Render2DUtils
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.color.ColorUtils
import dev.luna5ama.trollhack.graphics.font.TextComponent
import dev.luna5ama.trollhack.graphics.matrix.MatrixLayerStack
import dev.luna5ama.trollhack.graphics.matrix.scope
import dev.luna5ama.trollhack.graphics.matrix.translatef
import dev.luna5ama.trollhack.utils.math.vectors.HAlign
import dev.luna5ama.trollhack.utils.math.vectors.VAlign
import dev.luna5ama.trollhack.utils.state.FrameFloat
import dev.luna5ama.trollhack.utils.state.TimedFlag
import dev.luna5ama.trollhack.utils.timing.TimeUnit
import org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN
import kotlin.math.max

object ActiveModules : HudModule(
    name = "Active Modules",
    description = "List of enabled modules"
) {
    private var dockingH by setting("Docking H", HAlign.LEFT)
    private var dockingV by setting("Docking V", VAlign.TOP)
    private val mode by setting("Mode", Mode.LEFT_TAG)
    private val sortingMode by setting("Sorting Mode", SortingMode.LENGTH)
    private val showInvisible by setting("Show Invisible", false)
    private val bindOnly by setting("Bind Only", true, { !showInvisible })
    private val rainbow0 = setting("Rainbow", true)
    private val rainbow by rainbow0
    private val rainbowLength by setting("Rainbow Length", 10.0f, 1.0f..20.0f, 0.5f, { rainbow0.value })
    private val indexedHue by setting("Indexed Hue", 0.5f, 0.0f..1.0f, 0.05f, { rainbow0.value })
    private val saturation by setting("Saturation", 0.5f, 0.0f..1.0f, 0.01f, { rainbow0.value })
    private val brightness by setting("Brightness", 1.0f, 0.0f..1.0f, 0.01f, { rainbow0.value })

    private enum class Mode : Displayable {
        LEFT_TAG,
        RIGHT_TAG,
        FRAME
    }

    @Suppress("UNUSED")
    private enum class SortingMode(
        override val displayName: CharSequence,
        val keySelector: (AbstractModule) -> Comparable<*>
    ) : Displayable {
        LENGTH("Length", { -it.textLine.getWidth() }),
        ALPHABET("Alphabet", { it.nameAsString }),
        CATEGORY("Category", { Category.entries.indexOf(it.category) })
    }

    override val width by FrameFloat {
        ModuleManager.modules.maxOfOrNull {
            if (toggleMap[it.moduleId]?.value == true) it.textLine.getWidth() + 4.0f
            else 20.0f
        }?.let {
            max(it, 20.0f)
        } ?: 20.0f
    }

    override val height by FrameFloat {
        max(toggleMap.values.sumOf { it.displayHeight.toDouble() }.toFloat(), 20.0f)
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

    private var prevToggleMap = Int2ObjectOpenHashMap<ModuleToggleFlag>()
    private val toggleMap by AsyncCachedValue(1L, TimeUnit.SECONDS) {
        Int2ObjectOpenHashMap<ModuleToggleFlag>().apply {
            ModuleManager.modules.forEach {
                this[it.moduleId] = prevToggleMap[it.moduleId] ?: ModuleToggleFlag(it)
            }
            prevToggleMap = this
        }
    }

    init {
        nonNullParallelHandler<TickEvent.Post> {
            for ((id, flag) in toggleMap) {
                flag.update()
                if (flag.progress <= 0.0f) continue
                textLineMap[id] = flag.module.newTextLine()
            }
        }
    }

    override fun onRender2D(x: Float, y: Float) {
        RS.matrixLayer.scope {
            translatef(x, y, 0f)
            translatef(width * dockingH.multiplier, 0.0f, 0.0f)

            if (dockingV == VAlign.BOTTOM) {
                translatef(0.0f, height - (UnicodeFontManager.CURRENT_FONT.height + 2.0f), 0.0f)
            } else if (dockingV == VAlign.TOP) {
                translatef(0.0f, -1.0f, 0.0f)
            }

            if (dockingH == HAlign.LEFT) {
                translatef(-1.0f, 0.0f, 0.0f)
            }

            when (mode) {
                Mode.LEFT_TAG -> {
                    if (dockingH == HAlign.LEFT) {
                        translatef(2.0f, 0.0f, 0.0f)
                    }
                }
                Mode.RIGHT_TAG -> {
                    if (dockingH == HAlign.RIGHT) {
                        translatef(-2.0f, 0.0f, 0.0f)
                    }
                }
                else -> {
                    // 0x22 cute catgirl owo
                }
            }

            drawModuleList()
        }
    }

    context(MatrixLayerStack.MatrixScope)
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
                val timedFlag = toggleMap[module.moduleId] ?: continue
                val progress = timedFlag.progress

                if (progress <= 0.0f) continue

                var yOffset = timedFlag.displayHeight

                RS.matrixLayer.scope {
                    val hue = timedHue + indexedHue * 0.05f * index
                    val color = ColorUtils.hsbToRGB(hue, saturation, brightness, 1f)

                    val textLine = module.textLine
                    val textWidth = textLine.getWidth()
                    val animationXOffset = textWidth * dockingH.offset * (1.0f - progress)
                    val stringPosX = textWidth * dockingH.multiplier
                    val margin = 2.0f * dockingH.offset

                    this@scope.translatef(animationXOffset - margin - stringPosX, 0.0f, 0.0f)

                    when (mode) {
                        Mode.LEFT_TAG -> {
                            Render2DUtils.drawRect(-2.0f, 0.0f, textWidth + 2.0f, yOffset, GuiManager.backgroundRGBA)
                            Render2DUtils.drawRect(-4.0f, 0.0f, -2.0f, yOffset, color)
                        }
                        Mode.RIGHT_TAG -> {
                            Render2DUtils.drawRect(-2.0f, 0.0f, textWidth + 2.0f, yOffset, GuiManager.backgroundRGBA)
                            Render2DUtils.drawRect(textWidth + 2.0f, 0.0f, textWidth + 4.0f, yOffset, color)
                        }
                        Mode.FRAME -> {
                            Render2DUtils.drawRect(-2.0f, 0.0f, textWidth + 2.0f, yOffset, GuiManager.backgroundRGBA)
                        }
                    }

                    module.newTextLine(color).drawLine(progress, HAlign.LEFT)

                    if (dockingV == VAlign.BOTTOM) yOffset *= -1.0f
                }

                translatef(0.0f, yOffset, 0.0f)
                index++
            }
        } else {
            val color = Colors.primary
            for (pair in sortArray) {
                val module = pair.module
                val timedFlag = toggleMap[module.moduleId] ?: continue
                val progress = timedFlag.progress

                if (progress <= 0.0f) continue

                var yOffset = timedFlag.displayHeight

                RS.matrixLayer.scope {
                    val textLine = module.textLine
                    val textWidth = textLine.getWidth()
                    val animationXOffset = textWidth * dockingH.offset * (1.0f - progress)
                    val stringPosX = textWidth * dockingH.multiplier
                    val margin = 2.0f * dockingH.offset

                    this@scope.translatef(animationXOffset - margin - stringPosX, 0.0f, 0.0f)

                    when (mode) {
                        Mode.LEFT_TAG -> {
                            Render2DUtils.drawRect(-2.0f, 0.0f, textWidth + 2.0f, yOffset, GuiManager.backgroundRGBA)
                            Render2DUtils.drawRect(-4.0f, 0.0f, -2.0f, yOffset, color)
                        }
                        Mode.RIGHT_TAG -> {
                            Render2DUtils.drawRect(-2.0f, 0.0f, textWidth + 2.0f, yOffset, GuiManager.backgroundRGBA)
                            Render2DUtils.drawRect(textWidth + 2.0f, 0.0f, textWidth + 4.0f, yOffset, color)
                        }
                        Mode.FRAME -> {
                            Render2DUtils.drawRect(-2.0f, 0.0f, textWidth + 2.0f, yOffset, GuiManager.backgroundRGBA)
                        }
                    }

                    textLine.drawLine(progress, HAlign.LEFT)

                    if (dockingV == VAlign.BOTTOM) yOffset *= -1.0f
                }

                translatef(0.0f, yOffset, 0.0f)
            }
        }
    }

    private val AbstractModule.textLine
        get() = textLineMap.getOrPut(this.moduleId) {
            this.newTextLine()
        }

    private fun AbstractModule.newTextLine(color: ColorRGBA = Colors.primary) =
        TextComponent.TextLine(" ").apply {
            add(TextComponent.TextElement(nameAsString, color))
            (getDisplayInfo()?.toString() ?: "").let {
                if (it.isNotBlank()) {
                    add(
                        TextComponent.TextElement(
                            "${ChatUtils.GRAY}[${ChatUtils.RESET}${it}${ChatUtils.GRAY}]",
                            ColorRGBA(255, 255, 255)
                        )
                    )
                }
            }
            if (dockingH == HAlign.RIGHT) reverse()
        }

    private val TimedFlag<Boolean>.displayHeight
        get() = (UnicodeFontManager.CURRENT_FONT.height + 2.0f) * progress

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
        get() = this.isEnabled && (showInvisible || this.isVisible && (!bindOnly || this.bind.keyCode != GLFW_KEY_UNKNOWN))

    init {
//        relativePosX = -2.0f
//        relativePosY = 2.0f
        dockingH = HAlign.RIGHT
    }
}