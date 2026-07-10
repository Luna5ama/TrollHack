package dev.luna5ama.trollhack.utils.combat


import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.extension.block
import dev.luna5ama.trollhack.utils.extension.state
import dev.luna5ama.trollhack.utils.math.vectors.distanceTo
import dev.luna5ama.trollhack.utils.math.vectors.toBlockPos
import dev.luna5ama.trollhack.utils.math.vectors.toVec3
import dev.luna5ama.trollhack.utils.world.BlockUtils.isAir
import dev.luna5ama.trollhack.utils.world.EntityUtils.hasEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.*
import kotlin.math.roundToInt

object BurrowUtils {
    fun getFlooredPosition(entity: Entity): BlockPos {
        return BlockPos(
            floor(entity.position().x).toInt(), entity.y.roundToInt().toInt(),
            floor(entity.position().z).toInt()
        )
    }

    fun block2VecCenter(pos: BlockPos): Vec3 {
        return pos.toVec3().add(0.5, 0.5, 0.5)
    }

    context(ctx: NonNullContext)
    fun getFeetBlock(): List<BlockPos> = ctx.run {
        return getFeetBlock(0).toList()
    }

    context(ctx: NonNullContext)
    fun getFeetBlock(yOff: Int): Set<BlockPos> = ctx.run {
        val pos = player.boundingBox
        return buildSet {
            add(player.blockPosition())
            add(Vec3(pos.maxX - 0.05, player.y + yOff, pos.maxZ - 0.05).toBlockPos())
            add(Vec3(pos.maxX - 0.05, player.y + yOff, pos.minZ + 0.05).toBlockPos())
            add(Vec3(pos.minX + 0.05, player.y + yOff, pos.maxZ - 0.05).toBlockPos())
            add(Vec3(pos.minX + 0.05, player.y + yOff, pos.minZ + 0.05).toBlockPos())
        }
    }

    context(ctx: NonNullContext)
    fun Player.getFeetBlock(yOff: Int): Set<BlockPos> {
        val target = this
        val pos = target.boundingBox
        return buildSet {
            add(getFlooredPosition(target).relative(Direction.UP, yOff))
            add(Vec3(pos.maxX - 0.1, target.y + yOff, pos.maxZ - 0.1).toBlockPos())
            add(Vec3(pos.maxX - 0.1, target.y + yOff, pos.minZ + 0.1).toBlockPos())
            add(Vec3(pos.minX + 0.1, target.y + yOff, pos.maxZ - 0.1).toBlockPos())
            add(Vec3(pos.minX + 0.1, target.y + yOff, pos.minZ + 0.1).toBlockPos())
        }
    }

    fun normalizeAngle(angleIn: Double): Double {
        var angle = angleIn % 360.0
        if (angle >= 180.0) angle -= 360.0
        if (angle < -180.0) angle += 360.0
        return angle
    }

    context(ctx: NonNullContext)
    fun getLegitRotations(eyeVec: Vec3, vec: Vec3): FloatArray = ctx.run {
        val diffX = vec.x - eyeVec.x
        val diffY = vec.y - eyeVec.y
        val diffZ = vec.z - eyeVec.z
        val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)
        val yaw = Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90.0f
        val pitch = (-Math.toDegrees(atan2(diffY, diffXZ))).toFloat()
        return floatArrayOf(
            player.yRot + Mth.wrapDegrees(yaw - player.yRot),
            player.xRot + Mth.wrapDegrees(pitch - player.xRot)
        )
    }

    context(ctx: NonNullContext)
    fun getFeetBlock(yOff: Int, headFill: Boolean): List<BlockPos> = ctx.run {
        // new calculation
        val set = getFeetBlock(yOff).toMutableSet()
        if (headFill && yOff == 0) {
            set.addAll(getFeetBlock(1, headFill))
        }
        set.removeIf { i: BlockPos ->
            world.collidesWithSuffocatingBlock(player, AABB(i)) || hasEntity(AABB(i))
        }
        return set.toList()
    }

    fun NonNullContext.fakeBBoxCheckFeet(player: Player, offset: Vec3): Boolean {
        val futurePos: Vec3 = player.position().add(offset)
        return (isAir(futurePos.add(0.3, 0.0, 0.3).toBlockPos())
                && isAir(futurePos.add(-0.3, 0.0, 0.3).toBlockPos())
                && isAir(futurePos.add(0.3, 0.0, -0.3).toBlockPos())
                && isAir(futurePos.add(-0.3, 0.0, 0.3).toBlockPos()))
    }

    fun NonNullContext.get2BurFjPos(burBlockPos: BlockPos): Vec3 {
        val blockCenter = burBlockPos.toVec3().add(0.5, 0.5, 0.5)
        val playerPos = getFlooredPosition(player)
        val playerToBlock = player.position().subtract(blockCenter)
        var offset = Vec3.ZERO

        if (playerToBlock.x.absoluteValue >= playerToBlock.z.absoluteValue && playerToBlock.x.absoluteValue > 0.2) {
            offset = if (playerToBlock.x > 0) Vec3(0.8 - playerToBlock.x, 0.0, 0.0)
            else Vec3(-0.8 - playerToBlock.x, 0.0, 0.0)
        } else if (playerToBlock.z.absoluteValue >= playerToBlock.x.absoluteValue && playerToBlock.z.absoluteValue > 0.2) {
            offset = if (playerToBlock.z > 0) Vec3(0.0, 0.0, 0.8 - playerToBlock.z)
            else Vec3(0.0, 0.0, -0.8 - playerToBlock.z)
        } else if (burBlockPos == playerPos) {
            val validDirections = Direction.entries.filter { it.axis.isHorizontal }
                .filter { playerPos.relative(it).isPassable() && playerPos.relative(it).relative(Direction.UP).isPassable() }
                .sortedBy { dir ->
                    val offVec = blockCenter.add(dir.unitVec3.scale(0.5))
                    player.distanceTo(offVec.x, player.y, offVec.z)
                }

            if (validDirections.isNotEmpty()) offset = validDirections.first().unitVec3
        }

        return offset
    }

    context(ctx: NonNullContext)
    private fun BlockPos.isPassable(): Boolean {
        val state = this.state
        return state.canBeReplaced() && state.block != Blocks.COBWEB
    }

    private fun NonNullContext.visibleDirections(pos: BlockPos): List<Direction> {
        val list: MutableList<Direction> = java.util.ArrayList<Direction>()
        if (player.y + player.getEyeHeight(player.pose).toDouble() < pos.y) list.add(Direction.DOWN)
        if (player.y + player.getEyeHeight(player.pose).toDouble() > pos.y + 1) list.add(Direction.UP)
        if (player.x < pos.x) list.add(Direction.WEST)
        if (player.x > pos.x + 1) list.add(Direction.EAST)
        if (player.z < pos.z) list.add(Direction.NORTH)
        if (player.z > pos.z + 1) list.add(Direction.SOUTH)
        return list
    }

    fun NonNullContext.getFirstFacing(pos: BlockPos): Direction? {
        val iterator: Iterator<Direction> =
            getPossibleSides(pos).iterator()
        if (iterator.hasNext()) {
            val facing: Direction = iterator.next()
            return facing
        }
        return null
    }

    private fun NonNullContext.getPossibleSides(pos: BlockPos): List<Direction> {
        val facings = ArrayList<Direction>()
        try {
            for (side in Direction.entries) {
                val neighbour = pos.relative(side)
                if (!neighbour.block.hasCollision || neighbour.state.canBeReplaced()) continue
                facings.add(side)
                return facings
            }
        } catch (ignore: Exception) {}
        return facings
    }

    fun NonNullContext.getHelpBlockFacing(
        pos: BlockPos,
        strictFacing: Boolean,
        noStrUpPlace: Boolean,
        placeDistance: Double,
        ignoreDirections: List<Direction>? = null
    ): List<Direction> {
        val list = visibleDirections(pos)
        val availableDirections = ArrayList<Direction>() // must be inverted when sending packet
        val posVec: Vec3 = block2VecCenter(pos)
        for (facing in Direction.entries) {
            if (strictFacing && list.contains(facing)) {
                continue
            }
            val clickVec = posVec.add(facing.unitVec3.scale(0.5))
            if (player.distanceTo(clickVec.x, clickVec.y, clickVec.z) < placeDistance
                && (!isAir(pos.relative(facing)) || ignoreDirections != null && (ignoreDirections.contains(facing))))
                availableDirections.add(facing)
        }

        if (!availableDirections.contains(Direction.DOWN))
            if (noStrUpPlace && player.y - pos.y >= -2 && player.y - pos.y <= 2
                && (!isAir(pos.relative(Direction.DOWN))
                        || (ignoreDirections != null && ignoreDirections.contains(Direction.DOWN)))) {
                availableDirections.add(Direction.DOWN)
            }
        return availableDirections
    }
}
