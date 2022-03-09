package me.luna.trollhack.module.modules.movement

import me.luna.trollhack.event.events.player.PlayerMoveEvent
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.manager.managers.TimerManager.modifyTimer
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.MovementUtils.applySpeedPotionEffects
import me.luna.trollhack.util.MovementUtils.isCentered
import me.luna.trollhack.util.extension.fastCeil
import me.luna.trollhack.util.math.vector.Vec2d
import me.luna.trollhack.util.threads.runSafe
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min

internal object AutoCenter : Module(
    name = "AutoCenter",
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
                val postTicks = min(maxPostTicks, x.fastCeil())
                modifyTimer(50.0f / postTimer, postTicks)
            }

            ticks = 0
            center = null
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

                val speed = hypot(x, z)
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