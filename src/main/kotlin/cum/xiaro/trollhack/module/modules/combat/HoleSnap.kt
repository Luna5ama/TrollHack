package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.util.extension.fastCeil
import cum.xiaro.trollhack.util.extension.toRadian
import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.player.InputUpdateEvent
import cum.xiaro.trollhack.event.events.player.PlayerMoveEvent
import cum.xiaro.trollhack.event.events.render.Render3DEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.HoleManager
import cum.xiaro.trollhack.manager.managers.TimerManager.modifyTimer
import cum.xiaro.trollhack.manager.managers.TimerManager.resetTimer
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.module.modules.movement.Speed
import cum.xiaro.trollhack.module.modules.movement.Step
import cum.xiaro.trollhack.util.EntityUtils
import cum.xiaro.trollhack.util.EntityUtils.betterPosition
import cum.xiaro.trollhack.util.EntityUtils.isFlying
import cum.xiaro.trollhack.util.MovementUtils.applySpeedPotionEffects
import cum.xiaro.trollhack.util.MovementUtils.isCentered
import cum.xiaro.trollhack.util.MovementUtils.resetMove
import cum.xiaro.trollhack.util.MovementUtils.speed
import cum.xiaro.trollhack.util.combat.HoleInfo
import cum.xiaro.trollhack.util.graphics.RenderUtils3D
import cum.xiaro.trollhack.util.math.RotationUtils
import cum.xiaro.trollhack.util.math.vector.distanceSq
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.MovementInputFromOptions
import org.lwjgl.opengl.GL11.*
import kotlin.math.*

internal object HoleSnap : Module(
    name = "HoleSnap",
    description = "Move you into the hole nearby",
    category = Category.COMBAT,
    modulePriority = 120
) {
    private val vRange by setting("V Range", 5, 1..8, 1)
    private val hRange by setting("H Range", 4.0f, 1.0f..8.0f, 0.25f)
    private val timer by setting("Timer", 2.0f, 1.0f..4.0f, 0.01f)
    private val postTimer by setting("Post Timer", 0.25f, 0.01f..1.0f, 0.01f, { timer > 1.0f })
    private val maxPostTicks by setting("Max Post Ticks", 20, 0..50, 1, { timer > 1.0f && postTimer < 1.0f })
    private val timeoutTicks by setting("Timeout Ticks", 10, 0..100, 5)
    private val disableStrafe by setting("Disable Speed", false)
    private val disableStep by setting("Disable Step", false)

    var hole: HoleInfo? = null; private set
    private var stuckTicks = 0
    private var ranTicks = 0
    private var enabledTicks = 0

    override fun isActive(): Boolean {
        return isEnabled && hole != null
    }

    init {
        onDisable {
            hole = null
            stuckTicks = 0
            ranTicks = 0
            enabledTicks = 0
            resetTimer()
        }

        safeListener<Render3DEvent>(1) {
            hole?.let {
                val posFrom = EntityUtils.getInterpolatedPos(player, RenderUtils3D.partialTicks)
                val color = ColorRGB(32, 255, 32, 255)

                RenderUtils3D.putVertex(posFrom.x, posFrom.y, posFrom.z, color)
                RenderUtils3D.putVertex(it.center.x, it.center.y, it.center.z, color)

                glLineWidth(2.0f)
                glDisable(GL_DEPTH_TEST)
                RenderUtils3D.draw(GL_LINES)
                glLineWidth(1.0f)
                glEnable(GL_DEPTH_TEST)
            }
        }

        listener<PacketEvent.Receive> {
            if (it.packet is SPacketPlayerPosLook) disable()
        }

        listener<InputUpdateEvent>(-69) {
            if (it.movementInput is MovementInputFromOptions && isActive()) {
                it.movementInput.resetMove()
            }
        }

        safeListener<PlayerMoveEvent.Pre>(-10) {
            if (!HolePathFinder.isActive() && ++enabledTicks > timeoutTicks) {
                disable()
                return@safeListener
            }

            if (!player.isEntityAlive || player.isFlying) return@safeListener

            val currentSpeed = player.speed

            if (shouldDisable(currentSpeed)) {
                val ticks = ranTicks
                disable()

                if (timer > 0.0f && postTimer < 1.0f && ticks > 0) {
                    val x = (postTimer * ticks - timer * postTimer * ticks) / (timer * (postTimer - 1.0f))
                    val postTicks = min(maxPostTicks, x.fastCeil())
                    modifyTimer(50.0f / postTimer, postTicks)
                }
                return@safeListener
            }

            hole = findHole()?.also {
                enabledTicks = 0

                modifyTimer(50.0f / timer)
                ranTicks++
                if (disableStrafe) Speed.disable()
                if (disableStep) Step.disable()

                val playerPos = player.positionVector
                val yawRad = RotationUtils.getRotationTo(playerPos, it.center).x.toRadian()
                val dist = hypot(it.center.x - playerPos.x, it.center.z - playerPos.z)
                val baseSpeed = player.applySpeedPotionEffects(0.2873)
                val speed = if (player.onGround) baseSpeed else max(currentSpeed + 0.02, baseSpeed)
                val cappedSpeed = min(speed, dist)

                player.motionX = -sin(yawRad) * cappedSpeed
                player.motionZ = cos(yawRad) * cappedSpeed

                if (player.collidedHorizontally) stuckTicks++
                else stuckTicks = 0
            }
        }
    }

    private fun SafeClientEvent.shouldDisable(currentSpeed: Double) =
        hole?.let { player.posY < it.origin.y } ?: false
            || stuckTicks > 5 && currentSpeed < 0.05
            || HoleManager.getHoleInfo(player).let {
            it.isHole && player.isCentered(it.center)
        }

    private fun SafeClientEvent.findHole(): HoleInfo? {
        val playerPos = player.betterPosition
        val hRangeSq = hRange * hRange

        return HoleManager.holeInfos.asSequence()
            .filterNot { it.isTrapped }
            .filter { playerPos.y > it.origin.y }
            .filter { playerPos.y - it.origin.y <= vRange }
            .filter { distanceSq(player.posX, player.posZ, it.center.x, it.center.z) <= hRangeSq }
            .filter { it.canEnter(world, playerPos) }
            .minByOrNull { distanceSq(player.posX, player.posZ, it.center.x, it.center.z) }
    }
}