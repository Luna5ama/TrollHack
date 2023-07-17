package dev.luna5ama.trollhack.gui.hudgui.elements.client

import dev.luna5ama.trollhack.event.events.ModuleToggleEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.graphics.GlStateUtils
import dev.luna5ama.trollhack.graphics.RenderUtils2D
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.gui.hudgui.HudElement
import dev.luna5ama.trollhack.gui.hudgui.TrollHudGui
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.delegate.FrameFloat
import dev.luna5ama.trollhack.util.text.format
import it.unimi.dsi.fastutil.HashCommon
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.text.TextFormatting
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

internal object Notification : HudElement(
    name = "Notification",
    category = Category.CLIENT,
    description = "Client notifications",
    enabledByDefault = true
) {
    private val moduleToggle by setting("Module Toggle", true)
    private val moduleToggleMessageTimeout by setting(
        "Module Toggle Message Timeout",
        3000,
        0..10000,
        100,
        { moduleToggle })
    private val defaultTimeout by setting("Default Timeout", 5000, 0..10000, 100)
    private val nvidia by setting("Nvidia Theme", false)
    private val backgroundAlpha by setting("Background Alpha", 180, 0..255, 1, { nvidia })

    override val hudWidth by Message.Companion::minWidth

    override val hudHeight by Message.Companion::height

    private val notifications = CopyOnWriteArrayList<Message>()
    private val map = Long2ObjectMaps.synchronize(Long2ObjectOpenHashMap<Message>())

    init {
        safeListener<ModuleToggleEvent> {
            if (moduleToggle) {
                val message = it.module.nameAsString +
                    if (it.module.isEnabled) TextFormatting.RED format " disabled"
                    else TextFormatting.GREEN format " enabled"
                send(Notification.hashCode() * 31 + it.module.hashCode(), message, moduleToggleMessageTimeout.toLong())
            }
        }
    }

    override fun renderHud() {
        if (mc.currentScreen == TrollHudGui && notifications.isEmpty()) {
            Message.run {
                if (dockingH == dev.luna5ama.trollhack.graphics.HAlign.LEFT) {
                    RenderUtils2D.drawRectFilled(minWidth - padding, 0.0f, minWidth, height, color)
                } else {
                    RenderUtils2D.drawRectFilled(0.0f, 0.0f, padding, height, color)
                }

                MainFontRenderer.drawString("Example Notification", stringPosX, stringPosY)
            }
        }
    }

    fun render() {
        if (!visible) return

        GlStateUtils.pushMatrixAll()
        GlStateUtils.rescaleTroll()
         GlStateManager.translate(renderPosX, renderPosY, 0.0f)
        GlStateManager.scale(scale, scale, 0.0f)

        notifications.removeIf {
            GlStateManager.pushMatrix()
            val y = it.render()
            GlStateManager.popMatrix()

            if (y == -1.0f) {
                synchronized(map) {
                    if (map[it.id] === it) map.remove(it.id)
                }
                true
            } else {
                 GlStateManager.translate(0.0f, y, 0.0f)
                false
            }
        }

        GlStateUtils.popMatrixAll()
    }

    fun send(message: String, length: Long = defaultTimeout.toLong()) {
        send(message.hashCode(), message, length)
    }

    fun send(identifier: Any, message: String, length: Long = defaultTimeout.toLong()) {
        send(identifier.hashCode().toLong(), message, length)
    }

    fun send(id: Long, message: String, length: Long = defaultTimeout.toLong()) {
        synchronized(map) {
            val existing = map[id]
            if (existing != null && !existing.isTimeout) {
                existing.update(message, length)
            } else {
                val new = Message(message, length, id)
                map[id] = new
                notifications.add(new)
            }
        }
    }

    private class Message(
        private var message: String,
        private var length: Long,
        val id: Long
    ) {
        private val startTime by lazy { System.currentTimeMillis() }
        val isTimeout get() = System.currentTimeMillis() - startTime > length

        private val width0 = FrameFloat {
            max(minWidth, padding + padding + MainFontRenderer.getWidth(message) + padding)
        }
        private val width by width0

        fun update(message: String, length: Long) {
            this.message = message
            this.length = length + (System.currentTimeMillis() - startTime)
            width0.updateLazy()
        }

        fun render(): Float {
            if (dockingH != dev.luna5ama.trollhack.graphics.HAlign.LEFT && width > hudWidth) {
                 GlStateManager.translate(hudWidth - width, 0.0f, 0.0f)
            }

            return when (val deltaTotal = Easing.toDelta(startTime)) {
                in 0L..299L -> {
                    val delta = deltaTotal / 300.0f
                    val progress = Easing.OUT_CUBIC.inc(delta)
                    renderStage1(progress)
                }
                in 300L..500L -> {
                    val delta = (deltaTotal - 300L) / 200.0f
                    val progress = Easing.OUT_CUBIC.inc(delta)
                    renderStage2(progress)
                }
                else -> {
                    if (deltaTotal < length) {
                        renderStage3()
                    } else {
                        when (val endDelta = deltaTotal - length) {
                            in 0L..199L -> {
                                val delta = (endDelta) / 200.0f
                                val progress = Easing.OUT_CUBIC.dec(delta)
                                renderStage2(progress)
                            }
                            in 200L..500L -> {
                                val delta = (endDelta - 200L) / 300.0f
                                val progress = Easing.OUT_CUBIC.dec(delta)
                                renderStage1(progress)
                            }
                            else -> {
                                -1.0f
                            }
                        }
                    }
                }
            }
        }

        private fun renderStage1(progress: Float): Float {
            if (dockingH == dev.luna5ama.trollhack.graphics.HAlign.LEFT) {
                RenderUtils2D.drawRectFilled(0.0f, 0.0f, width * progress, height, color)
            } else {
                RenderUtils2D.drawRectFilled(minWidth * (1.0f - progress), 0.0f, width, height, color)
            }

            return (height + space) * progress
        }

        private fun renderStage2(progress: Float): Float {
            RenderUtils2D.drawRectFilled(0.0f, 0.0f, width, height, backGroundColor)

            val textColor = ColorRGB(255, 255, 255, (255.0f * progress).toInt())
            MainFontRenderer.drawString(message, stringPosX, stringPosY, color = textColor)

            if (dockingH == dev.luna5ama.trollhack.graphics.HAlign.LEFT) {
                RenderUtils2D.drawRectFilled((width - padding) * progress, 0.0f, width, height, color)
            } else {
                RenderUtils2D.drawRectFilled(0.0f, 0.0f, padding + (width - padding) * (1.0f - progress), height, color)
            }

            return height + space
        }

        private fun renderStage3(): Float {
            RenderUtils2D.drawRectFilled(0.0f, 0.0f, width, height, backGroundColor)

            if (dockingH == dev.luna5ama.trollhack.graphics.HAlign.LEFT) {
                RenderUtils2D.drawRectFilled(width - padding, 0.0f, width, height, color)
            } else {
                RenderUtils2D.drawRectFilled(0.0f, 0.0f, padding, height, color)
            }

            MainFontRenderer.drawString(message, stringPosX, stringPosY)

            return height + space
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Message

            return id == other.id
        }

        override fun hashCode(): Int {
            return HashCommon.long2int(id)
        }

        companion object {
            val color get() = if (nvidia) ColorRGB(118, 185, 0) else GuiSetting.primary.alpha(255)
            val backGroundColor get() = if (nvidia) ColorRGB(0, 0, 0, backgroundAlpha) else GuiSetting.backGround

            val minWidth get() = 150.0f

            val height get() = MainFontRenderer.getHeight() * 4.0f
            val space get() = 4.0f

            val padding get() = 4.0f
            val stringPosX get() = if (dockingH == dev.luna5ama.trollhack.graphics.HAlign.LEFT) padding else padding + padding
            val stringPosY get() = height * 0.5f - 1.0f - MainFontRenderer.getHeight() * 0.5f
        }
    }
}