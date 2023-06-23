package dev.luna5ama.trollhack.module.modules.movement

import dev.fastmc.common.ceilToInt
import dev.fastmc.common.sq
import dev.luna5ama.trollhack.event.events.player.InputUpdateEvent
import dev.luna5ama.trollhack.event.events.player.PlayerMoveEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.TimerManager.modifyTimer
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.MovementUtils.applySpeedPotionEffects
import dev.luna5ama.trollhack.util.MovementUtils.isCentered
import dev.luna5ama.trollhack.util.math.vector.Vec2d
import dev.luna5ama.trollhack.util.threads.runSafe
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

internal object AutoCenter : Module(
    name = "Auto Center",
    description = "Moves player to the center of a block",
    category = Category.MOVEMENT
) {
    private val timer by setting("Timer", 2.0f, 1.0f..4.0f, 0.01f)
    private val postTimer by setting("Post Timer", 0.25f, 0.01f..1.0f, 0.01f, { timer > 1.0f })
    private val maxPostTicks by setting("Max Post Ticks", 20, 0..50, 1, { timer > 1.0f && postTimer < 1.0f })
    private val timeoutTicks by setting("Timeout Ticks", 5, 0..20, 1)

    private var ticks = 0
    private var center: Vec2d? = null

    fun centerPlayer(blockPos: BlockPos) {
        center = Vec2d(blockPos.x + 0.5, blockPos.z + 0.5)
        enable()
    }

    fun centerPlayer(vec3d: Vec3d) {
        center = Vec2d(vec3d.x, vec3d.z)
        enable()
    }

    init {
        onEnable {
            if (center == null) {
                runSafe {
                    center = Vec2d(floor(player.posX) + 0.5, floor(player.posZ) + 0.5)
                }
            }
        }

        onDisable {
            if (timer > 0.0f && postTimer < 1.0f && ticks > 0) {
                val x = (postTimer * ticks - timer * postTimer * ticks) / (timer * (postTimer - 1.0f))
                val postTicks = min(maxPostTicks, x.ceilToInt())
                modifyTimer(50.0f / postTimer, postTicks)
            }

            ticks = 0
            center = null
        }

        safeListener<InputUpdateEvent> {
            it.movementInput.moveForward = 0.0f
            it.movementInput.moveStrafe = 0.0f
            it.movementInput.jump = false
            it.movementInput.sneak = false
        }

        safeListener<PlayerMoveEvent.Pre>(-2000) {
            val center = center
            if (center == null || ticks++ >= timeoutTicks) {
                disable()
                return@safeListener
            }

            if (!player.isCentered(center.x, center.y)) {
                var x = center.x - player.posX
                var z = center.y - player.posZ

                val speed = sqrt(x.sq + z.sq)
                val baseSpeed = if (player.isSneaking) 0.05746 else 0.2873
                val maxSpeed = player.applySpeedPotionEffects(baseSpeed)

                if (speed > maxSpeed) {
                    val multiplier = maxSpeed / speed
                    x *= multiplier
                    z *= multiplier
                }

                player.motionX = 0.0
                player.motionZ = 0.0
                it.x = x
                it.z = z
                modifyTimer(25.0f)
            } else if (player.onGround) {
                disable()
            }
        }
    }
}