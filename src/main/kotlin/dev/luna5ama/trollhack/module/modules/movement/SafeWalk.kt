package dev.luna5ama.trollhack.module.modules.movement

import dev.fastmc.common.floorToInt
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.mixins.core.entity.MixinEntity
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.player.Scaffold
import dev.luna5ama.trollhack.util.BaritoneUtils
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.threads.runSafeOrFalse
import net.minecraft.util.math.BlockPos

/**
 * @see MixinEntity.moveInvokeIsSneakingPre
 * @see MixinEntity.moveInvokeIsSneakingPost
 */
internal object SafeWalk : Module(
    name = "Safe Walk",
    category = Category.MOVEMENT,
    description = "Keeps you from walking off edges"
) {
    private val checkFallDist by setting("Check Fall Distance", true, description = "Check fall distance from edge")

    init {
        onToggle {
            BaritoneUtils.settings?.assumeSafeWalk?.value = it
        }
    }

    @JvmStatic
    fun shouldSafewalk(entityID: Int, motionX: Double, motionZ: Double): Boolean {
        return runSafeOrFalse {
            if (entityID != player.entityId) return false
            if (player.isSneaking) return false

            if (Scaffold.shouldSafeWalk) return true
            if (isEnabled && (!checkFallDist && !BaritoneUtils.isPathing || !isEdgeSafe(motionX, motionZ))) return true

            false
        }
    }


    @JvmStatic
    fun setSneaking(state: Boolean) {
        Wrapper.player?.movementInput?.sneak = state
    }

    private fun isEdgeSafe(motionX: Double, motionZ: Double): Boolean {
        return runSafeOrFalse {
            checkFallDist(player.posX, player.posZ)
                && checkFallDist(player.posX + motionX, player.posZ + motionZ)
        }
    }

    private fun SafeClientEvent.checkFallDist(posX: Double, posZ: Double): Boolean {
        val startY = (player.posY - 0.5).floorToInt()
        val pos = BlockPos.PooledMutableBlockPos.retain(posX.floorToInt(), startY, posZ.floorToInt())

        for (y in startY downTo startY - 2) {
            pos.y = y
            if (world.getBlockState(pos).getCollisionBoundingBox(world, pos) != null) {
                pos.release()
                return true
            }
        }

        pos.y = startY - 3
        val box = world.getBlockState(pos).getCollisionBoundingBox(world, pos)
        pos.release()

        return box != null && box.maxY >= 1.0
    }
}