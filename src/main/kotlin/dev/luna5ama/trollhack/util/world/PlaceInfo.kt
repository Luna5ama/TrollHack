package dev.luna5ama.trollhack.util.world

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.math.vector.Vec3f
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

class PlaceInfo(
    val pos: BlockPos,
    val direction: EnumFacing,
    val dist: Double,
    val hitVecOffset: Vec3f,
    val hitVec: Vec3d,
    val placedPos: BlockPos
) {
    companion object {
        fun SafeClientEvent.newPlaceInfo(pos: BlockPos, side: EnumFacing): PlaceInfo {
            val hitVecOffset = getHitVecOffset(side)
            val hitVec = getHitVec(pos, side)

            return PlaceInfo(pos, side, player.eyePosition.distanceTo(hitVec), hitVecOffset, hitVec, pos.offset(side))
        }
    }
}