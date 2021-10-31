package cum.xiaro.trollhack.module.modules.movement

import cum.xiaro.trollhack.util.extension.fastFloor
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.mixin.entity.MixinEntity
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.module.modules.player.Scaffold
import cum.xiaro.trollhack.util.BaritoneUtils
import cum.xiaro.trollhack.util.Wrapper
import cum.xiaro.trollhack.util.threads.runSafeOrFalse
import net.minecraft.util.math.BlockPos

/**
 * @see MixinEntity.moveInvokeIsSneakingPre
 * @see MixinEntity.moveInvokeIsSneakingPost
 */
internal object SafeWalk : Module(
    name = "SafeWalk",
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
            !player.isSneaking && player.entityId == entityID
                && (isEnabled || isEnabled && Scaffold.safeWalk || isEnabled)
                && (!checkFallDist && !BaritoneUtils.isPathing || !isEdgeSafe(motionX, motionZ))
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
        val startY = (player.posY - 0.5).fastFloor()
        val pos = BlockPos.PooledMutableBlockPos.retain(posX.fastFloor(), startY, posZ.fastFloor())

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