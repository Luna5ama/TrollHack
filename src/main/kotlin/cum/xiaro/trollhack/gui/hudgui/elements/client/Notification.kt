package cum.xiaro.trollhack.gui.hudgui.elements.client

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.util.graphics.Easing
import cum.xiaro.trollhack.event.events.ModuleToggleEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.gui.hudgui.HudElement
import cum.xiaro.trollhack.gui.hudgui.TrollHudGui
import cum.xiaro.trollhack.module.modules.client.GuiSetting
import cum.xiaro.trollhack.util.delegate.CachedValue
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.graphics.HAlign
import cum.xiaro.trollhack.util.graphics.RenderUtils2D
import cum.xiaro.trollhack.util.graphics.font.renderer.MainFontRenderer
import cum.xiaro.trollhack.util.math.vector.Vec2f
import cum.xiaro.trollhack.util.text.format
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.util.text.TextFormatting
import org.lwjgl.opengl.GL11.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

internal object Notification : HudElement(
    name = "Notification",
    category = Category.CLIENT,
    description = "Client notifications",
    enabledByDefault = true
) {
    private val moduleToggle by setting("Module Toggle", true)
    private val nvidia by setting("Nvidia Theme", false)
    private val backgroundAlpha by setting("Background Alpha", 180, 0..255, 1, { nvidia })

    override val hudWidth: Float
        get() = Message.minWidth

    override val hudHeight: Float
        get() = Message.height

    private val notifications = CopyOnWriteArrayList<Message>()
    private val map = Int2ObjectMaps.synchronize(Int2ObjectOpenHashMap<Message>())

    init {
        safeListener<ModuleToggleEvent> {
            if (moduleToggle) {
                val message = it.module.nameAsString +
                    if (it.module.isEnabled) TextFormatting.RED format " disabled"
                    else TextFormatting.GREEN format " enabled"
                send(Notification.hashCode() * 31 + it.module.hashCode(), message, 1500L)
            }
        }
    }

    override fun renderHud() {
        if (mc.currentScreen == TrollHudGui && notifications.isEmpty()) {
            Message.run {
                if (dockingH == HAlign.LEFT) {
                    RenderUtils2D.drawRectFilled(Vec2f(minWidth - padding, 0.0f), Vec2f(minWidth, height), color)
                } else {
                    RenderUtils2D.drawRectFilled(Vec2f.ZERO, Vec2f(padding, height), color)
                }

                MainFontRenderer.drawString("Example Notification", stringPosX, stringPosY)
            }
        }
    }

    fun render() {
        if (!visible) return

        GlStateUtils.pushMatrixAll()
        GlStateUtils.rescaleTroll()
        glTranslatef(renderPosX, renderPosY, 0.0f)
        glScalef(scale, scale, 0.0f)

        notifications.removeIf {
            glPushMatrix()
            val y = it.render()
            glPopMatrix()

            if (y == -1.0f) {
                synchronized(map) {
                    if (map[it.id] === it) map.remove(it.id)
                }
                true
            } else {
                glTranslatef(0.0f, y, 0.0f)
                false
            }
        }

        GlStateUtils.popMatrixAll()
    }

    fun send(message: String, length: Long = 3000L) {
        send(message.hashCode(), message, length)
    }

    fun send(identifier: Any, message: String, length: Long = 3000L) {
        send(identifier.hashCode(), message, length)
    }

    fun send(id: Int, message: String, length: Long = 3000L) {
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
        val id: Int
    ) {
        private val startTime by lazy { System.currentTimeMillis() }
        val isTimeout get() = System.currentTimeMillis() - startTime > length

        private val width0 = CachedValue {
            max(minWidth, padding + padding + MainFontRenderer.getWidth(message) + padding)
        }
        private val width by width0.wrapped(50L)

        fun update(message: String, length: Long) {
            this.message = message
            this.length = length + (System.currentTimeMillis() - startTime)
            width0.updateLazy()
        }

        fun render(): Float {
            if (dockingH != HAlign.LEFT && width > hudWidth) {
                glTranslatef(hudWidth - width, 0.0f, 0.0f)
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
            if (dockingH == HAlign.LEFT) {
                RenderUtils2D.drawRectFilled(Vec2f.ZERO, Vec2f(width * progress, height), color)
            } else {
                RenderUtils2D.drawRectFilled(Vec2f(minWidth * (1.0f - progress), 0.0f), Vec2f(width, height), color)
            }

            return (height + space) * progress
        }

        private fun renderStage2(progress: Float): Float {
            RenderUtils2D.drawRectFilled(Vec2f.ZERO, Vec2f(width, height), backGroundColor)

            val textColor = ColorRGB(255, 255, 255, (255.0f * progress).toInt())
            MainFontRenderer.drawString(message, stringPosX, stringPosY, color = textColor)

            if (dockingH == HAlign.LEFT) {
                RenderUtils2D.drawRectFilled(Vec2f((width - padding) * progress, 0.0f), Vec2f(width, height), color)
            } else {
                RenderUtils2D.drawRectFilled(Vec2f.ZERO, Vec2f(padding + (width - padding) * (1.0f - progress), height), color)
            }

            return height + space
        }

        private fun renderStage3(): Float {
            RenderUtils2D.drawRectFilled(Vec2f.ZERO, Vec2f(width, height), backGroundColor)

            if (dockingH == HAlign.LEFT) {
                RenderUtils2D.drawRectFilled(Vec2f(width - padding, 0.0f), Vec2f(width, height), color)
            } else {
                RenderUtils2D.drawRectFilled(Vec2f.ZERO, Vec2f(padding, height), color)
            }

            MainFontRenderer.drawString(message, stringPosX, stringPosY)

            return height + space
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Message

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id
        }

        companion object {
            val color get() = if (nvidia) ColorRGB(118, 185, 0) else GuiSetting.primary.alpha(255)
            val backGroundColor get() = if (nvidia) ColorRGB(0, 0, 0, backgroundAlpha) else GuiSetting.backGround

            val minWidth get() = 128.0f

            val height get() = 24.0f
            val space get() = 4.0f

            val padding get() = 4.0f
            val stringPosX get() = if (dockingH == HAlign.LEFT) padding else padding + padding
            val stringPosY get() = height * 0.5f - 1.0f - MainFontRenderer.getHeight() * 0.5f
        }
    }
}