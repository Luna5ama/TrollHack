package dev.luna5ama.trollhack.utils.world

import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager
import dev.luna5ama.trollhack.mixins.accessor.IClientLevelAccessor
import dev.luna5ama.trollhack.modules.AbstractModule
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.extension.pitch
import dev.luna5ama.trollhack.utils.extension.yaw
import dev.luna5ama.trollhack.utils.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.utils.math.vectors.distanceSqTo
import dev.luna5ama.trollhack.utils.math.vectors.distanceTo
import dev.luna5ama.trollhack.utils.math.vectors.toVec3
import dev.luna5ama.trollhack.utils.world.EntityUtils.canSee
import dev.luna5ama.trollhack.utils.world.EntityUtils.getEyesPos
import net.minecraft.client.Minecraft
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

object BlockUtils {
    private const val EPSILON = 0.00001f
    private var breakingBlockPos: BlockPos? = null
    private var breakingTime = 0
    val blackList = listOf(
        Blocks.ENDER_CHEST,
        Blocks.CHEST,
        Blocks.TRAPPED_CHEST,
        Blocks.CRAFTING_TABLE,
        Blocks.ANVIL,
        Blocks.BREWING_STAND,
        Blocks.HOPPER,
        Blocks.DROPPER,
        Blocks.DISPENSER,
        Blocks.OAK_TRAPDOOR,
        Blocks.SPRUCE_TRAPDOOR,
        Blocks.BIRCH_TRAPDOOR,
        Blocks.JUNGLE_TRAPDOOR,
        Blocks.ACACIA_TRAPDOOR,
        Blocks.CHERRY_TRAPDOOR,
        Blocks.DARK_OAK_TRAPDOOR,
        Blocks.MANGROVE_TRAPDOOR,
        Blocks.BAMBOO_TRAPDOOR,
        Blocks.ENCHANTING_TABLE
    )
    val shulkerList = listOf(
        Blocks.WHITE_SHULKER_BOX,
        Blocks.ORANGE_SHULKER_BOX,
        Blocks.MAGENTA_SHULKER_BOX,
        Blocks.LIGHT_BLUE_SHULKER_BOX,
        Blocks.YELLOW_SHULKER_BOX,
        Blocks.LIME_SHULKER_BOX,
        Blocks.PINK_SHULKER_BOX,
        Blocks.GRAY_SHULKER_BOX,
        Blocks.LIGHT_GRAY_SHULKER_BOX,
        Blocks.CYAN_SHULKER_BOX,
        Blocks.PURPLE_SHULKER_BOX,
        Blocks.BLUE_SHULKER_BOX,
        Blocks.BROWN_SHULKER_BOX,
        Blocks.GREEN_SHULKER_BOX,
        Blocks.RED_SHULKER_BOX,
        Blocks.BLACK_SHULKER_BOX
    )
    val shiftBlocks= listOf(
        Blocks.ENDER_CHEST,
        Blocks.CHEST,
        Blocks.TRAPPED_CHEST,
        Blocks.CRAFTING_TABLE,
        Blocks.BIRCH_TRAPDOOR,
        Blocks.BAMBOO_TRAPDOOR,
        Blocks.DARK_OAK_TRAPDOOR,
        Blocks.CHERRY_TRAPDOOR,
        Blocks.ANVIL,
        Blocks.BREWING_STAND,
        Blocks.HOPPER,
        Blocks.DROPPER,
        Blocks.DISPENSER,
        Blocks.ACACIA_TRAPDOOR,
        Blocks.ENCHANTING_TABLE,
        Blocks.WHITE_SHULKER_BOX,
        Blocks.ORANGE_SHULKER_BOX,
        Blocks.MAGENTA_SHULKER_BOX,
        Blocks.LIGHT_BLUE_SHULKER_BOX,
        Blocks.YELLOW_SHULKER_BOX,
        Blocks.LIME_SHULKER_BOX,
        Blocks.PINK_SHULKER_BOX,
        Blocks.GRAY_SHULKER_BOX,
        Blocks.CYAN_SHULKER_BOX,
        Blocks.PURPLE_SHULKER_BOX,
        Blocks.BLUE_SHULKER_BOX,
        Blocks.BROWN_SHULKER_BOX,
        Blocks.GREEN_SHULKER_BOX,
        Blocks.RED_SHULKER_BOX,
        Blocks.BLACK_SHULKER_BOX
    )

    fun getBreakingBlockPos(): BlockPos? {
        return breakingBlockPos
    }

    fun getBreakingTime(): Int {
        return breakingTime
    }

    fun toBlockPos(vec: Vec3): BlockPos {
        return BlockPos(vec.x.toInt(), vec.y.toInt(), vec.z.toInt())
    }

    fun toVec3(pos: Vec3i): Vec3 {
        return Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
    }

    fun isAir(world: Level, pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
        return state.isAir
    }

    fun NonNullContext.isAir(pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
        return state.isAir
    }

    fun isColliding(world: Level, box: AABB): Boolean {
        return world.noCollision(box)
    }

    fun canInteract(pos: BlockPos): Boolean {
        val player = Minecraft.getInstance().player ?: return false
        val checkPos: Vec3 = pos.toVec3().add(0.5, 0.5, 0.5)

        return player.distanceSqTo(checkPos.x, checkPos.y, checkPos.z) < 64
    }

//    fun canPlace(world: Level, face: BlockPlaceInfo, vararg options: PlaceOptions): Boolean {
//        val option = BlockPlaceOption(*options)
//        val pos: BlockPos = face.blockPos
//        val facing: Direction = face.blockFace.facing
//        val box = world.getBlockState(pos).getCollisionShape(world, pos).boundingBox.offset(pos)
//        val player = MinecraftClient.getInstance().player
//        if (player != null && pos.toVec3().add(0.5, 0.5, 0.5)
//                .squaredDistanceTo(player.pos ) > 36
//        ) return false
//        if (world.isSpaceEmpty(box) && !option.isBlockCollisionIgnored
//            && !option.isEntityCollisionIgnored
//        ) return false
//        return if (isAir(world, pos.offset(facing.opposite)) && !option.isAirPlaceIgnored
//        ) false else world.getBlockState(pos).block.canPlaceBlockOnSide(world, pos, facing.opposite)
//    }

    fun getPlayerPosRealY(player: Player): BlockPos {
        return BlockPos(floor(player.x).toInt(), player.y.roundToInt(), floor(player.z).toInt())
    }

    context(ctx: NonNullContext)
    fun getLegitRotations(vec: Vec3): FloatArray = ctx.run {
        val eyesPos: Vec3 = getEyesPos()
        val diffX = vec.x - eyesPos.x
        val diffY = vec.y - eyesPos.y
        val diffZ = vec.z - eyesPos.z
        val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)
        val yaw = Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90.0f
        val pitch = (-Math.toDegrees(atan2(diffY, diffXZ))).toFloat()
        return floatArrayOf(
            player.yaw + Mth.wrapDegrees(yaw - player.yaw),
            player.pitch + Mth.wrapDegrees(pitch - player.pitch)
        )
    }

    context(ctx: NonNullContext, module: AbstractModule)
    fun placeAnchor(
        pos: BlockPos,
        hand: InteractionHand = InteractionHand.MAIN_HAND,
        rotate: Boolean = true,
        packet: Boolean = false,
        anchor: Int,
        glowstone: Int,
        noglowstone: Int,
        setAir :Boolean
    ): Unit = ctx.run {
        if (world.getBlockState(pos).block == Blocks.RESPAWN_ANCHOR) {
            HotbarSwitchManager.ghostSwitch(glowstone) {
                rightClickBlock(pos, hand, getClickSideToCc(pos)!!, packet, rotate)
                HotbarSwitchManager.ghostSwitch(noglowstone) {
                    rightClickBlock(pos, hand, getClickSideToCc(pos)!!, packet, rotate)
                }
            }

        } else {
            HotbarSwitchManager.ghostSwitch(anchor) {
                place(pos, InteractionHand.MAIN_HAND, packet = true)
                HotbarSwitchManager.ghostSwitch(glowstone) {
                    rightClickBlock(pos, hand, getClickSideToCc(pos)!!, packet, rotate)
                    HotbarSwitchManager.ghostSwitch(noglowstone) {
                        rightClickBlock(pos, hand, getClickSideToCc(pos)!!, packet, rotate)
                    }
                }
            }
        }
        if (setAir) world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())
    }

    context(ctx: NonNullContext, module: AbstractModule)
    fun place(
        pos: BlockPos,
        hand: InteractionHand = InteractionHand.MAIN_HAND,
        rotate: Boolean = true,
        packet: Boolean = false,
        strict: Boolean = true,
        legit: Boolean = false,
        air: Boolean = false
    ): Unit = ctx.run {
        if (air) {
            for (direction in Direction.entries) {
                if (!world.isInWorldBounds(pos.relative(direction))) continue
                rightClickBlock(pos, hand, direction, packet, rotate)
                break
            }
        }
        val side: Direction = getPlaceSide(pos,strict,legit) ?: return
        rightClickBlock(pos.relative(side), hand, side.opposite, packet, rotate)
        val hitVec = pos.toVec3().add(0.5, 0.5, 0.5).add(side.unitVec3.scale(0.5))
        if (rotate) {
            val rotation = getRotationTo(hitVec)
            PlayerPacketManager.sendPlayerPacket(Int.MAX_VALUE) {
                cancelRotate()
                rotate(rotation)
            }
        }
    }


    context(ctx: NonNullContext)
    fun getPlaceSide(pos: BlockPos, strict: Boolean = true, legit: Boolean = false, air: Boolean = false): Direction? = ctx.run {
        var dis = 114514.0
        var side: Direction? = null
        for (i in Direction.entries) {
            if (canClick(pos.relative(i)) && !canReplace(pos.relative(i))) {
                if (legit) {
                    if (!canSee(pos.relative(i), i.opposite)) continue
                }
                if (strict) {
                    if (!isStrictDirection(pos.relative(i), i.opposite)) continue
                }
                val vecDis: Double = player.distanceToSqr(
                    pos.center.add(i.unitVec3.x * 0.5, i.unitVec3.y * 0.5, i.unitVec3.z * 0.5)
                )
                if (side == null || vecDis < dis) {
                    side = i
                    dis = vecDis
                }
            }
        }
        if (side == null && air) {
            for (i in Direction.entries) {
                if (world.isEmptyBlock(pos.relative(i))) {
                    return i
                }
            }
        }
        return side
    }

    context(ctx: NonNullContext)
    fun isStrictDirection(pos: BlockPos, side: Direction): Boolean = ctx.run {
        val blockState: BlockState = world.getBlockState(pos)
        val isFullBox = blockState.block == Blocks.AIR || blockState.isCollisionShapeFullBlock(world, pos)
                || getBlock(pos) == Blocks.COBWEB
        return isStrictDirection(pos, side, isFullBox)
    }

    context(ctx: NonNullContext)
    fun isStrictDirection(pos: BlockPos, side: Direction, isFullBox: Boolean): Boolean = ctx.run {
        if (player.y - pos.y >= 0 && side == Direction.DOWN) return false
        if (getBlock(pos.relative(side)) == Blocks.OBSIDIAN || getBlock(
                pos.relative(side)
            ) == Blocks.BEDROCK || getBlock(pos.relative(side)) == Blocks.RESPAWN_ANCHOR
        ) return false
        val eyePos: Vec3 = player.eyePosition
        val blockCenter = pos.center
        val validAxis = ArrayList<Direction>()
        validAxis.addAll(
            checkAxis(
                eyePos.x - blockCenter.x,
                Direction.WEST,
                Direction.EAST,
                !isFullBox
            )
        )
        validAxis.addAll(
            checkAxis(
                eyePos.y - blockCenter.y,
                Direction.DOWN,
                Direction.UP,
                true
            )
        )
        validAxis.addAll(
            checkAxis(
                eyePos.z - blockCenter.z,
                Direction.NORTH,
                Direction.SOUTH,
                !isFullBox
            )
        )
        return validAxis.contains(side)
    }

    fun checkAxis(
        diff: Double,
        negativeSide: Direction,
        positiveSide: Direction,
        bothIfInRange: Boolean
    ): ArrayList<Direction> {
        val valid = ArrayList<Direction>()
        if (diff < -0.5) {
            valid.add(negativeSide)
        }
        if (diff > 0.5) {
            valid.add(positiveSide)
        }
        if (bothIfInRange) {
            if (!valid.contains(negativeSide)) valid.add(negativeSide)
            if (!valid.contains(positiveSide)) valid.add(positiveSide)
        }
        return valid
    }

    context(ctx: NonNullContext, module: AbstractModule)
    fun doPlace(pos: BlockPos, hand: InteractionHand, webSlot: Int, rotate: Boolean, packet: Boolean): Unit = ctx.run {
        if (world.isEmptyBlock(pos)) {
            HotbarSwitchManager.ghostSwitch(webSlot) {
                place(pos, hand, rotate, packet)
            }
        }
    }

    fun NonNullContext.getMaxHeight(box: AABB): Double {
        val collisions = world.getCollisions(null, box.move(0.0, -1.0, 0.0))
        var updated = false
        var maxY = 0.0
        for (collision in collisions) {
            if (collision.bounds().maxY > maxY) {
                updated = true
                maxY = collision.bounds().maxY
            }
        }
        return if (updated) maxY else Double.NaN
    }

    context(ctx: NonNullContext)
    fun canClick(pos: BlockPos): Boolean = ctx.run {
        return world.getBlockState(pos).isSolid && (!(blackList.contains(getBlock(pos)) ||
                shulkerList.contains(getBlock(pos)) || getBlock(pos) is BedBlock) || player.isShiftKeyDown)
    }

    context(ctx: NonNullContext)
    fun getBlock(pos: BlockPos): Block = ctx.run {
        return getState(pos).block
    }

    context(ctx: NonNullContext)
    fun getState(pos: BlockPos): BlockState = ctx.run {
        return world.getBlockState(pos)
    }

    context(ctx: NonNullContext)
    fun canReplace(pos: BlockPos): Boolean = ctx.run {
        return getState(pos).canBeReplaced()
    }
//    context(ctx: NonNullContext)
//    private fun getPossibleSides(pos: BlockPos): List<Direction> =
//        Direction.entries.filter {
//            val neighbour = pos.offset(it)
//            neighbour.block.canCollideCheck(neighbour.state, false)
//                    && !neighbour.material.isReplaceable
//        }

//    context(ctx: NonNullContext)
//    private fun getFirstFacing(pos: BlockPos): Direction? = getPossibleSides(pos).firstOrNull()

    context(ctx: NonNullContext, module: AbstractModule)
    fun rightClickBlock(pos: BlockPos, hand: InteractionHand, direction: Direction, packet: Boolean, rotate: Boolean): Unit = ctx.run {
        val hitVec = pos.toVec3().add(0.5, 0.5, 0.5).add(direction.unitVec3.scale(0.5))
//        val directionVec = Vec3(
//            pos.x  + direction.vector.x * 0.5,
//            pos.y  + direction.vector.y * 0.5,
//            pos.z  + direction.vector.z * 0.5
//        ).add(0.5,0.5,0.5)

        if (rotate) {
            val rotation = getRotationTo(hitVec)
            PlayerPacketManager.sendPlayerPacket(114514) {
                cancelRotate()
                rotate(rotation)
            }
        }
        val hit = BlockHitResult(hitVec, direction, pos, false)
        if (packet) {
            netHandler.send(
                ServerboundUseItemOnPacket(
                    hand,
                    hit,
                    getWorldActionId(world)
                )
            )
        } else {
            interaction.useItemOn(
                player,
                hand,
                hit
            )
        }
        player.swing(hand)
    }

    context(ctx: NonNullContext)
    fun getClickSideStrict(pos: BlockPos, strict: Boolean = false): Direction? = ctx.run {
        var side: Direction? = null
        var range = 100.0
        for (i in Direction.entries) {
            if (!canSee(pos, i)) continue
            if (sqrt(player.distanceToSqr(pos.relative(i).center).toFloat()) > range) continue
            side = i
            range = sqrt(player.distanceToSqr(pos.relative(i).center).toFloat()).toDouble()
        }
        if (side != null) return side
        side = null
        for (i in Direction.entries) {
            if (strict) {
                if (!isStrictDirection(pos, i)) continue
                if (!isAir(pos.relative(i))) continue
            }
            if (sqrt(player.distanceToSqr(pos.relative(i).center).toFloat()) > range) continue
            side = i
            range = sqrt(player.distanceToSqr(pos.relative(i).center).toFloat()).toDouble()
        }
        return side
    }

    context(ctx: NonNullContext)
    fun getClickSideToCc(pos: BlockPos): Direction? = ctx.run {
        var i = 114514.0
        var d: Direction? = null
        for (direction in Direction.entries) {
            if (!world.isEmptyBlock(pos.relative(direction))) continue
            val hitVec = pos.relative(direction)
            val s = player.eyePosition.distanceTo(hitVec.center)
            if (s < i) {
                d = direction
                i = s
            }
        }
        return d
    }

    context(ctx: NonNullContext)
    fun getClickSide(pos: BlockPos, strict: Boolean = false): Direction? = ctx.run {
        var side: Direction? = null
        var range = 100.0
        for (i in Direction.entries) {
            if (!canSee(pos, i)) continue
            if (sqrt(player.distanceToSqr(pos.relative(i).center).toFloat()) > range) continue
            side = i
            range = sqrt(player.distanceToSqr(pos.relative(i).center).toFloat()).toDouble()
        }
        if (side != null) return side
        side = null
        for (i in Direction.entries) {
            if (strict) {
                if (!isStrictDirection(pos, i)) continue
                if (!isAir(pos.relative(i))) continue
            }
            if (sqrt(player.distanceToSqr(pos.relative(i).center).toFloat()) > range) continue
            side = i
            range = sqrt(player.distanceToSqr(pos.relative(i).center).toFloat()).toDouble()
        }
        return side
    }

    fun getWorldActionId(world: ClientLevel): Int {
        val pum = getUpdateManager(world)
        val p = pum.currentSequence()
        pum.close()
        return p
    }

    fun getUpdateManager(world: ClientLevel): BlockStatePredictionHandler {
        return (world as IClientLevelAccessor).acquirePendingUpdateManager()
    }

    context(ctx: NonNullContext)
    fun sendYawAndPitch(yaw: Float, pitch: Float): Unit = ctx.run {
        netHandler.send(ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround(), player.horizontalCollision))
    }

    context(ctx: NonNullContext)
    fun hasEntityBlockCrystal(pos: BlockPos, ignoreCrystal: Boolean, ignoreItem: Boolean): Boolean = ctx.run {
        for (entity in world.getEntitiesOfClass(Entity::class.java, AABB(pos))) {
            if (!entity.isAlive || ignoreItem && entity is ItemEntity || ignoreCrystal && entity is EndCrystal) continue
            return true
        }
        return false
    }

    context(ctx: NonNullContext)
    fun distanceToXZ(x: Double, z: Double): Double = ctx.run {
        val dx: Double = player.x - x
        val dz: Double = player.z - z
        return sqrt(dx * dx + dz * dz)
    }

    context(ctx: NonNullContext)
    fun canPlace(pos: BlockPos): Boolean = ctx.run {
        return canPlace(pos, false)
    }

    context(ctx: NonNullContext)
    fun canPlace(pos: BlockPos, ignoreCrystal: Boolean): Boolean = ctx.run {
        if (!canReplace(pos)) return false
        return !EntityUtils.hasEntity(pos, ignoreCrystal)
    }

    context(ctx: NonNullContext)
    fun canPlace(pos: BlockPos, distance: Double): Boolean = ctx.run {
        if (getPlaceSide(pos, distance) == null) return false
        if (!canReplace(pos)) return false
        return !EntityUtils.hasEntity(pos, false)
    }

    context(ctx: NonNullContext)
    fun canPlace(pos: BlockPos, distance: Double, ignoreCrystal: Boolean): Boolean = ctx.run {
        if (getPlaceSide(pos, distance) == null) {
            return false
        }
        if (!canReplace(pos)) {
            return false
        }
        return !EntityUtils.hasEntity(pos, ignoreCrystal)
    }

    context(ctx: NonNullContext)
    fun getPlaceSide(pos: BlockPos, distance: Double): Direction? = ctx.run {
        var dis = 0.0
        var side: Direction? = null
        for (i in Direction.entries) {
            if (canClick(pos.relative(i)) && !canReplace(pos.relative(i))) {
                if (ClientSettings.placeMode == ClientSettings.PlaceMode.LEGIT) {
                    if (!canSee(pos.relative(i), i.opposite)) continue
                } else if (ClientSettings.placeMode == ClientSettings.PlaceMode.STRICT) {
                    if (!isStrictDirection(pos.relative(i), i.opposite)) continue
                }
                val vecDis: Double = player.distanceToSqr(
                    pos.center.add(i.unitVec3.x * 0.5, i.unitVec3.y * 0.5, i.unitVec3.z * 0.5)
                )
                if (sqrt(vecDis.toFloat()) > distance) {
                    continue
                }
                if (side == null || vecDis < dis) {
                    side = i
                    dis = vecDis
                }
            }
        }
        if (side == null && ClientSettings.placeMode == ClientSettings.PlaceMode.AIR_PLACE) {
            for (i in Direction.entries) {
                if (world.isEmptyBlock(pos.relative(i))) {
                    return i
                }
            }
        }
        return side
    }

    context(ctx: NonNullContext)
    fun getBestNeighboring(pos: BlockPos, facing: Direction): Direction? = ctx.run {
        for (i in Direction.entries) {
            if (pos.relative(i) == pos.relative(facing, -1) || i == Direction.DOWN) continue
            if (getPlaceSide(pos, false, true) != null) return i
        }
        var bestFacing: Direction? = null
        var distance = 0.0
        for (i in Direction.entries) {
            if (pos.relative(i) == pos.relative(facing, -1) || i == Direction.DOWN) continue
            if (getPlaceSide(pos) != null) {
                if (bestFacing == null || player.distanceToSqr(pos.relative(i).center) < distance) {
                    bestFacing = i
                    distance = player.distanceToSqr(pos.relative(i).center)
                }
            }
        }
        return bestFacing
    }

    context(ctx: NonNullContext)
    fun clickBlock(pos: BlockPos, side: Direction, rotate: Boolean): Unit = ctx.run {
        clickBlock(pos, side, rotate, InteractionHand.MAIN_HAND)
    }

    context(ctx: NonNullContext)
    fun clickBlock(pos: BlockPos, side: Direction, rotate: Boolean, hand: InteractionHand): Unit = ctx.run {
        clickBlock(pos, side, rotate, hand, ClientSettings.packetPlace)
    }

    context(ctx: NonNullContext)
    fun clickBlock(pos: BlockPos, side: Direction, rotate: Boolean, packet: Boolean): Unit = ctx.run {
        clickBlock(pos, side, rotate, InteractionHand.MAIN_HAND, packet)
    }

    context(ctx: NonNullContext)
    fun clickBlock(pos: BlockPos, side: Direction, rotate: Boolean, hand: InteractionHand, packet: Boolean): Unit = ctx.run {
        val directionVec = Vec3(
            pos.x + 0.5 + side.unitVec3.x * 0.5,
            pos.y + 0.5 + side.unitVec3.y * 0.5, pos.z + 0.5 + side.unitVec3.z * 0.5
        )
        if (rotate) {
            EntityUtils.faceVector(directionVec)
        }
        player.swing(hand)
        val result = BlockHitResult(directionVec, side, pos, false)
        if (packet) {
            netHandler.send(
                ServerboundUseItemOnPacket(
                    InteractionHand.MAIN_HAND,
                    result,
                    getWorldActionId(world)
                )
            )
        } else {
            interaction.useItemOn(player, hand, result)
        }
    }

    fun needSneak(block: Block): Boolean {
        return shiftBlocks.contains(block)
    }

//
//    context(ctx: NonNullContext, module: AbstractModule)
//    fun placeBlock(pos: BlockPos, hand: Hand, rotate: Boolean, packet: Boolean, isSneaking: Boolean = player.isSneaking): Boolean = ctx.run {
//        var sneaking = false
//        val side = getFirstFacing(pos) ?: return false
//        val neighbour = pos.offset(side)
//        val opposite = side.opposite
//        val hitVec = Vec3(neighbour.).add(0.5, 1.0, 0.5).add(Vec3(opposite.directionVec).scale(0.5))
//        val neighbourBlock = neighbour.block
//        if (!player.isSneaking && (neighbourBlock in blackList || neighbourBlock in shulkerList)) {
//            netHandler.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))
//            player.isSneaking = true
//            sneaking = true
//        }
//        if (rotate) {
//            val rotation = this@NonNullContext.getRotationTo(hitVec)
//            this@AbstractModule.sendPlayerPacket {
//                cancelAll()
//                rotate(rotation)
//            }
//        }
//        rightClickBlock(neighbour, hitVec, hand, opposite, packet)
//        return sneaking || isSneaking
//    }
}
