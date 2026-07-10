package dev.luna5ama.trollhack.gui.hud.impl

import it.unimi.dsi.fastutil.HashCommon
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import dev.luna5ama.trollhack.RS
import dev.luna5ama.trollhack.gui.HudModule
import dev.luna5ama.trollhack.gui.NullHudEditor
import dev.luna5ama.trollhack.manager.managers.GuiManager
import dev.luna5ama.trollhack.manager.managers.UnicodeFontManager
import dev.luna5ama.trollhack.modules.impl.client.Colors
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.graphics.animations.Easing
import dev.luna5ama.trollhack.graphics.buffer.Render2DUtils
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.matrix.MatrixLayerStack
import dev.luna5ama.trollhack.graphics.matrix.scope
import dev.luna5ama.trollhack.graphics.matrix.translatef
import dev.luna5ama.trollhack.utils.math.vectors.HAlign
import dev.luna5ama.trollhack.utils.state.FrameFloat
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

object Notification : HudModule(
    name = "Notification",
    description = "Client notifications",
) {
    private var dockingH by setting("Docking H", HAlign.LEFT)
    val moduleToggle by setting("Module Toggle", true)
    val moduleToggleMessageTimeout by setting(
        "Module Toggle Message Timeout",
        3000,
        0..10000,
        100,
        { moduleToggle })
    private val defaultTimeout by setting("Default Timeout", 5000, 0..10000, 100)
    private val nvidia by setting("Nvidia Theme", false)
    private val backgroundAlpha by setting("Background Alpha", 180, 0..255, 1, { nvidia })

    override val width by Message.Companion::minWidth

    override val height by Message.Companion::height

    private val notifications = CopyOnWriteArrayList<Message>()
    private val map = Long2ObjectMaps.synchronize(Long2ObjectOpenHashMap<Message>())

    init {
//        handler<ModuleToggleEvent> {
//            if (moduleToggle) {
//                val message = it.module.nameAsString +
//                    if (it.module.isEnabled) "${ChatUtils.RED} enabled${ChatUtils.RESET}"
//                    else "${ChatUtils.GREEN} enabled${ChatUtils.RESET}"
//                send(Notification.hashCode() * 31 + it.module.hashCode(), message, moduleToggleMessageTimeout.toLong())
//            }
//        }
    }

    override fun onRender2D(x: Float, y: Float) {
        RS.matrixLayer.scope {
            translatef(x, y, 0f)
            if (mc.screen == NullHudEditor && notifications.isEmpty()) {
                Message.run {
                    Render2DUtils.drawRect(0.0f, 0.0f, width, height, backGroundColor)
                    if (dockingH == HAlign.LEFT) {
                        Render2DUtils.drawRect(minWidth - padding, 0.0f, minWidth, height, color)
                    } else {
                        Render2DUtils.drawRect(0.0f, 0.0f, padding, height, color)
                    }

                    UnicodeFontManager.CURRENT_FONT.drawStringWithShadow("Example Notification", stringPosX, stringPosY)
                }
            } else render()
        }
    }

    fun render() {
//        if (!visible) return

        RS.matrixLayer.scope {
            notifications.removeIf {
                var y = 0f
                RS.matrixLayer.scope {
                    y = it.render()
                }

                if (y == -1.0f) {
                    synchronized(map) {
                        if (map[it.id] === it) map.remove(it.id)
                    }
                    true
                } else {
                    translatef(0.0f, y, 0.0f)
                    false
                }
            }
        }
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
            max(minWidth, padding + padding + UnicodeFontManager.CURRENT_FONT.getWidth(message) + padding)
        }
        private val width by width0

        fun update(message: String, length: Long) {
            this.message = message
            this.length = length + (System.currentTimeMillis() - startTime)
            width0.updateLazy()
        }

        context(matrixScope: MatrixLayerStack.MatrixScope)
        fun render(): Float = matrixScope.run {
            if (dockingH != HAlign.LEFT && width > width) {
                 translatef(width - width, 0.0f, 0.0f)
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
                Render2DUtils.drawRect(0.0f, 0.0f, width * progress, height, color)
            } else {
                Render2DUtils.drawRect(minWidth * (1.0f - progress), 0.0f, width, height, color)
            }

            return (height + space) * progress
        }

        private fun renderStage2(progress: Float): Float {
            Render2DUtils.drawRect(0.0f, 0.0f, width, height, backGroundColor)

            val textColor = ColorRGBA(255, 255, 255, (255.0f * progress).toInt())
            UnicodeFontManager.CURRENT_FONT.drawStringWithShadow(message, stringPosX, stringPosY, color = textColor.awt)

            if (dockingH == HAlign.LEFT) {
                Render2DUtils.drawRect((width - padding) * progress, 0.0f, width, height, color)
            } else {
                Render2DUtils.drawRect(0.0f, 0.0f, padding + (width - padding) * (1.0f - progress), height, color)
            }

            return height + space
        }

        private fun renderStage3(): Float {
            Render2DUtils.drawRect(0.0f, 0.0f, width, height, backGroundColor)

            if (dockingH == HAlign.LEFT) {
                Render2DUtils.drawRect(width - padding, 0.0f, width, height, color)
            } else {
                Render2DUtils.drawRect(0.0f, 0.0f, padding, height, color)
            }

            UnicodeFontManager.CURRENT_FONT.drawStringWithShadow(message, stringPosX, stringPosY)

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
            val color get() = if (nvidia) ColorRGBA(118, 185, 0) else Colors.color.alpha(255)
            val backGroundColor get() = if (nvidia) ColorRGBA(0, 0, 0, backgroundAlpha) else GuiManager.backgroundRGBA

            val minWidth get() = 150.0f

            val height get() = UnicodeFontManager.CURRENT_FONT.height * 2.7f
            val space get() = 4.0f

            val padding get() = 4.0f
            val stringPosX get() = if (dockingH == HAlign.LEFT) padding else padding + padding
            val stringPosY get() = height * 0.5f - 1.0f - UnicodeFontManager.CURRENT_FONT.height * 0.5f
        }
    }

    enum class NotificationType(val icon: Char) {
        INFO('I'),
        WARN('?'),
        ERROR('!')
    }
}
