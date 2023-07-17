package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.ceilToInt
import dev.fastmc.common.distanceSq
import dev.fastmc.common.sq
import dev.fastmc.common.toRadians
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.player.InputUpdateEvent
import dev.luna5ama.trollhack.event.events.player.PlayerMoveEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.RenderUtils3D
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.manager.managers.HoleManager
import dev.luna5ama.trollhack.manager.managers.TimerManager.modifyTimer
import dev.luna5ama.trollhack.manager.managers.TimerManager.resetTimer
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.movement.Speed
import dev.luna5ama.trollhack.module.modules.movement.Step
import dev.luna5ama.trollhack.util.EntityUtils
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.EntityUtils.isFlying
import dev.luna5ama.trollhack.util.MovementUtils.applySpeedPotionEffects
import dev.luna5ama.trollhack.util.MovementUtils.isCentered
import dev.luna5ama.trollhack.util.MovementUtils.resetMove
import dev.luna5ama.trollhack.util.MovementUtils.speed
import dev.luna5ama.trollhack.util.combat.HoleInfo
import dev.luna5ama.trollhack.util.math.RotationUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.MovementInputFromOptions
import org.lwjgl.opengl.GL11.*
import kotlin.math.*

internal object HoleSnap : Module(
    name = "Hole Snap",
    description = "Move you into the hole nearby",
    category = Category.COMBAT,
    modulePriority = 120
) {
    private val downRange by setting("Down Range", 5, 1..8, 1)
    private val upRange by setting("Up Range", 1, 1..8, 1)
    private val hRange by setting("H Range", 4.0f, 1.0f..8.0f, 0.25f)
    private val timer by setting("Timer", 1.5f, 1.0f..4.0f, 0.01f)
    private val postTimer by setting("Post Timer", 0.8f, 0.01f..1.0f, 0.01f, { timer > 1.0f })
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

                GlStateManager.glLineWidth(2.0f)
                glDisable(GL_DEPTH_TEST)
                RenderUtils3D.draw(GL_LINES)
                GlStateManager.glLineWidth(1.0f)
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

        safeListener<PlayerMoveEvent.Pre>(-10) { event ->
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
                    val postTicks = min(maxPostTicks, x.ceilToInt())
                    modifyTimer(50.0f / postTimer, postTicks)
                }
                return@safeListener
            }

            hole = (HolePathFinder.hole ?: findHole())?.let {
                enabledTicks = 0

                if (checkYRange(player.posY.toInt(), it.origin.y)
                    && distanceSq(player.posX, player.posZ, it.center.x, it.center.z) <= hRange.sq
                ) {
                    modifyTimer(50.0f / timer, 5)
                    ranTicks++
                    if (disableStrafe) Speed.disable()
                    if (disableStep) Step.disable()

                    val playerPos = player.positionVector
                    val yawRad = RotationUtils.getRotationTo(playerPos, it.center).x.toRadians()
                    val dist = hypot(it.center.x - playerPos.x, it.center.z - playerPos.z)
                    val baseSpeed = player.applySpeedPotionEffects(0.2873)
                    val speed = if (player.onGround) baseSpeed else max(currentSpeed + 0.02, baseSpeed)
                    val cappedSpeed = min(speed, dist)

                    player.motionX = 0.0
                    player.motionZ = 0.0
                    event.x = -sin(yawRad) * cappedSpeed
                    event.z = cos(yawRad) * cappedSpeed

                    if (player.collidedHorizontally && HolePathFinder.isDisabled) stuckTicks++
                    else stuckTicks = 0

                    it
                } else {
                    null
                }
            }
        }
    }

    private fun SafeClientEvent.shouldDisable(currentSpeed: Double) =
        hole?.let { player.posY < it.origin.y } ?: false
            || stuckTicks > 5 && currentSpeed < 0.05
            || player.onGround && HoleManager.getHoleInfo(player).let {
            it.isHole && player.isCentered(it.center)
        }

    private fun SafeClientEvent.findHole(): HoleInfo? {
        val playerPos = player.betterPosition
        val hRangeSq = hRange * hRange

        return HoleManager.holeInfos.asSequence()
            .filterNot { it.isTrapped }
            .filter { checkYRange(playerPos.y, it.origin.y) }
            .filter { distanceSq(player.posX, player.posZ, it.center.x, it.center.z) <= hRangeSq }
            .filter { it.canEnter(world, playerPos) }
            .minByOrNull { distanceSq(player.posX, player.posZ, it.center.x, it.center.z) }
    }

    private fun checkYRange(playerY: Int, holeY: Int): Boolean {
        return if (playerY >= holeY) {
            playerY - holeY <= downRange
        } else {
            holeY - playerY <= -upRange
        }
    }
}