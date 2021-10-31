package cum.xiaro.trollhack.module.modules.movement

import cum.xiaro.trollhack.util.extension.fastFloor
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.AddCollisionBoxEvent
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.player.PlayerTravelEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.BaritoneUtils
import cum.xiaro.trollhack.util.accessor.moving
import cum.xiaro.trollhack.util.accessor.y
import cum.xiaro.trollhack.util.world.getBlock
import net.minecraft.block.BlockLiquid
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityBoat
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

internal object Jesus : Module(
    name = "Jesus",
    description = "Allows you to walk on water",
    category = Category.MOVEMENT
) {
    private val mode by setting("Mode", Mode.SOLID)

    private enum class Mode {
        SOLID, DOLPHIN
    }

    private val waterWalkBox = AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.99, 1.0)

    init {
        onToggle {
            BaritoneUtils.settings?.assumeWalkOnWater?.value = it
        }

        safeListener<PlayerTravelEvent> {
            if (mc.gameSettings.keyBindSneak.isKeyDown || player.fallDistance > 3.0f || !isInWater(player)) return@safeListener

            if (mode == Mode.DOLPHIN) {
                player.motionY += 0.03999999910593033 // regular jump speed
            } else {
                player.motionY = 0.1

                player.ridingEntity?.let {
                    if (it !is EntityBoat) it.motionY = 0.3
                }
            }
        }

        safeListener<PacketEvent.Send> {
            if (it.packet !is CPacketPlayer || !it.packet.moving) return@safeListener
            if (mc.gameSettings.keyBindSneak.isKeyDown || player.ticksExisted % 2 != 0) return@safeListener

            val entity = player.ridingEntity ?: player

            if (isAboveLiquid(entity, entity.entityBoundingBox, true) && !isInWater(entity)) {
                it.packet.y += 0.02
            }
        }

        safeListener<AddCollisionBoxEvent> {
            if (mode == Mode.DOLPHIN) return@safeListener
            if (mc.gameSettings.keyBindSneak.isKeyDown) return@safeListener
            if (it.entity == null || it.entity is EntityBoat) return@safeListener
            if (it.block !is BlockLiquid) return@safeListener

            if (player.fallDistance > 3.0f) return@safeListener

            if (it.entity != player && it.entity != player.ridingEntity) return@safeListener
            if (isInWater(it.entity) || it.entity.posY < it.pos.y) return@safeListener
            if (!isAboveLiquid(it.entity, it.entityBox, false)) return@safeListener

            it.collidingBoxes.add(waterWalkBox.offset(it.pos))
        }
    }

    private fun SafeClientEvent.isInWater(entity: Entity): Boolean {
        val box = entity.entityBoundingBox
        val y = (box.minY + 0.01).fastFloor()
        val pos = BlockPos.PooledMutableBlockPos.retain()

        for (x in box.minX.fastFloor()..box.maxX.fastFloor()) {
            for (z in box.minZ.fastFloor()..box.maxZ.fastFloor()) {
                if (world.getBlock(pos.setPos(x, y, z)) is BlockLiquid) {
                    pos.release()
                    return true
                }
            }
        }

        pos.release()
        return false
    }

    private fun SafeClientEvent.isAboveLiquid(entity: Entity, box: AxisAlignedBB, packet: Boolean): Boolean {
        val offset = when {
            packet -> 0.03
            entity is EntityPlayer -> 0.2
            else -> 0.5
        }

        val y = (box.minY - offset).fastFloor()
        val pos = BlockPos.PooledMutableBlockPos.retain()

        for (x in box.minX.fastFloor()..box.maxX.fastFloor()) {
            for (z in box.minZ.fastFloor()..box.maxZ.fastFloor()) {
                if (world.getBlock(pos.setPos(x, y, z)) is BlockLiquid) {
                    pos.release()
                    return true
                }
            }
        }

        pos.release()
        return false
    }

}