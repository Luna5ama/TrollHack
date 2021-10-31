package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.util.math.MathUtils
import cum.xiaro.trollhack.event.events.ConnectionEvent
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.render.Render2DEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.module.modules.client.GuiSetting
import cum.xiaro.trollhack.process.PauseProcess.pauseBaritone
import cum.xiaro.trollhack.process.PauseProcess.unpauseBaritone
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.TimeUnit
import cum.xiaro.trollhack.util.WebUtils
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.graphics.Resolution
import cum.xiaro.trollhack.util.graphics.font.renderer.MainFontRenderer
import cum.xiaro.trollhack.util.math.vector.Vec2f
import cum.xiaro.trollhack.util.text.MessageSendUtils
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11.glColor4f

/**
 * Thanks Brady and cooker and leij for helping me not be completely retarded
 */
internal object LagNotifier : Module(
    name = "LagNotifier",
    description = "Displays a warning when the server is lagging",
    category = Category.PLAYER
) {
    private val detectRubberBand by setting("Detect Rubber Band", true)
    private val pauseBaritone0 = setting("Pause Baritone", true)
    private val pauseBaritone by pauseBaritone0
    val pauseTakeoff by setting("Pause Elytra Takeoff", true)
    val pauseAutoWalk by setting("Pause Auto Walk", true)
    private val feedback by setting("Pause Feedback", true, pauseBaritone0.atTrue())
    private val timeout by setting("Timeout", 3.5f, 0.0f..10.0f, 0.5f)

    private val pingTimer = TickTimer(TimeUnit.SECONDS)
    private val lastPacketTimer = TickTimer()
    private val lastRubberBandTimer = TickTimer()
    private var text = ""

    var paused = false; private set

    init {
        onDisable {
            unpause()
        }

        listener<Render2DEvent.Troll> {
            if (text.isBlank()) return@listener

            val posX = Resolution.trollWidthF / 2.0f - MainFontRenderer.getWidth(text) / 2.0f
            val posY = 80.0f / GuiSetting.scaleFactorFloat

            /* 80px down from the top edge of the screen */
            MainFontRenderer.drawString(text, posX, posY, color = ColorRGB(255, 33, 33))
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        }

        safeListener<TickEvent.Post> {
            if (mc.isIntegratedServerRunning) {
                unpause()
                text = ""
            } else {
                val timeoutMillis = (timeout * 1000.0f).toLong()
                when {
                    lastPacketTimer.tick(timeoutMillis) -> {
                        if (pingTimer.tickAndReset(1L)) WebUtils.update()

                        text = if (WebUtils.isInternetDown) "Your internet is offline! "
                        else "Server Not Responding! "

                        text += timeDifference(lastPacketTimer.time)
                        pause()
                    }
                    detectRubberBand && !lastRubberBandTimer.tick(timeoutMillis) -> {
                        text = "RubberBand Detected! ${timeDifference(lastRubberBandTimer.time)}"
                        pause()
                    }
                    else -> {
                        unpause()
                    }
                }
            }
        }

        safeListener<PacketEvent.Receive>(2000) {
            lastPacketTimer.reset()

            if (!detectRubberBand || it.packet !is SPacketPlayerPosLook || player.ticksExisted < 20) return@safeListener

            val dist = Vec3d(it.packet.x, it.packet.y, it.packet.z).subtract(player.positionVector).length()
            val rotationDiff = Vec2f(it.packet.yaw, it.packet.pitch).minus(Vec2f(player)).length()

            if (dist in 0.5..64.0 || rotationDiff > 1.0) lastRubberBandTimer.reset()
        }

        listener<ConnectionEvent.Connect> {
            lastPacketTimer.reset(69420L)
            lastRubberBandTimer.reset(-69420L)
        }
    }

    private fun pause() {
        if (!paused && pauseBaritone && feedback) {
            MessageSendUtils.sendBaritoneMessage("Paused due to lag!")
        }

        pauseBaritone()
        paused = true
    }

    private fun unpause() {
        if (paused && pauseBaritone && feedback) {
            MessageSendUtils.sendBaritoneMessage("Unpaused!")
        }

        unpauseBaritone()
        paused = false
        text = ""
    }

    private fun timeDifference(timeIn: Long) = MathUtils.round((System.currentTimeMillis() - timeIn) / 1000.0, 1)
}
