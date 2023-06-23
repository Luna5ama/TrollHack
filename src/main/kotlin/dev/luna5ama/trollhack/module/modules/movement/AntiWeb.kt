package dev.luna5ama.trollhack.module.modules.movement

import dev.fastmc.common.floorToInt
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.AddCollisionBoxEvent
import dev.luna5ama.trollhack.event.events.player.PlayerMoveEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.isFlying
import dev.luna5ama.trollhack.util.accessor.isInWeb
import dev.luna5ama.trollhack.util.world.getBlock
import net.minecraft.init.Blocks
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

internal object AntiWeb : Module(
    name = "Anti Web",
    description = "Prevents walking into web",
    category = Category.MOVEMENT
) {
    private val speedMultiplier by setting("Speed Multiplier", 0.8f, 0.1f..1.0f, 0.1f)

    init {
        safeListener<AddCollisionBoxEvent> {
            if (it.entity == player && it.block == Blocks.WEB) {
                it.collidingBoxes.add(
                    AxisAlignedBB(
                        it.pos.x.toDouble(), it.pos.y.toDouble(), it.pos.z.toDouble(),
                        it.pos.x + 1.0, it.pos.y + 1.0, it.pos.z + 1.0
                    )
                )
            }
        }

        safeListener<PlayerMoveEvent.Pre>(-2000) {
            if (!player.isFlying && player.onGround && player.motionY <= 0.0 && player.motionY >= -0.08 && !player.isInWeb && isAboveWeb()) {
                it.x = player.motionX * speedMultiplier
                it.z = player.motionZ * speedMultiplier
            }
        }
    }

    private fun SafeClientEvent.isAboveWeb(): Boolean {
        val box = player.entityBoundingBox
        val pos = BlockPos.PooledMutableBlockPos.retain()
        val y = (player.posY - 0.08).floorToInt()

        for (x in box.minX.floorToInt()..box.maxX.floorToInt()) {
            for (z in box.minZ.floorToInt()..box.maxZ.floorToInt()) {
                if (world.getBlock(pos.setPos(x, y, z)) != Blocks.WEB) {
                    pos.release()
                    return false
                }
            }
        }

        pos.release()
        return true
    }
}