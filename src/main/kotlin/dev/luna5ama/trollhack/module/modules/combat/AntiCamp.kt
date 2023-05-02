package dev.luna5ama.trollhack.module.modules.combat

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.HoleManager
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.flooredPosition
import dev.luna5ama.trollhack.util.TickTimer
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.inventory.operation.swapToBlock
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.math.vector.toVec3d
import dev.luna5ama.trollhack.util.pause.MainHandPause
import dev.luna5ama.trollhack.util.pause.withPause
import dev.luna5ama.trollhack.util.threads.defaultScope
import dev.luna5ama.trollhack.util.threads.onMainThreadSafe
import dev.luna5ama.trollhack.util.threads.runSafe
import dev.luna5ama.trollhack.util.world.*
import dev.luna5ama.trollhack.util.world.PlaceInfo.Companion.newPlaceInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.block.BlockShulkerBox
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketCloseWindow
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.server.SPacketOpenWindow
import net.minecraft.tileentity.TileEntityShulkerBox
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.abs

@CombatManager.CombatModule
internal object AntiCamp : Module(
    name = "AntiCamp",
    category = Category.COMBAT,
    description = "Push enemy out of hole/burrow",
    modulePriority = 160
) {
    private val mode by setting("Mode", Mode.SHULKER)
    private val placeDelay by setting("Place Delay", 1, 0..20, 1)
    private val openDelay by setting("Open Delay", 10, 0..20, 1)
    private val closeDelay by setting("Close Delay", 2, 0..20, 1)
    private val range by setting("Range", 5.0f, 0.0f..6.0f, 0.25f)

    private enum class Mode {
        SHULKER, PISTON
    }

    private val timeoutTimer = TickTimer()
    private val timer = TickTimer()
    private var placeInfo: Pair<BlockPos, EnumFacing>? = null
    private var lastHitVec: Vec3d? = null
    private var windowID = -1
    private var shulkerState = ShulkerState.OBBY

    private enum class ShulkerState(override val displayName: CharSequence) : DisplayEnum {
        OBBY("Obby"),
        SHULKER("Shulker"),
        OPEN("Open"),
        OPENING("Opening"),
        CLOSE("Close")
    }

    override fun getHudInfo(): String {
        return when (mode) {
            Mode.SHULKER -> shulkerState.displayString
            Mode.PISTON -> ""
        }
    }

    init {
        onEnable {
            timeoutTimer.reset()
            timer.reset()

            placeInfo = CombatManager.target?.let {
                runSafe {
                    getPlacingPos(it)
                }
            } ?: run {
                disable()
                null
            }
        }

        onDisable {
            placeInfo = null
            lastHitVec = null
            windowID = -1
            shulkerState = ShulkerState.OBBY
        }

        listener<PacketEvent.Receive> {
            when (it.packet) {
                is SPacketOpenWindow -> {
                    if (shulkerState == ShulkerState.OPEN) {
                        it.cancel()
                        windowID = it.packet.windowId
                        shulkerState = ShulkerState.OPENING
                        timer.reset()
                    }
                }
            }
        }

        listener<WorldEvent.ServerBlockUpdate> {
            val (pos, side) = placeInfo ?: return@listener

            when (shulkerState) {
                ShulkerState.OBBY -> {
                    if (it.pos == pos.offset(side.opposite) && it.newState.block != Blocks.AIR) {
                        shulkerState = ShulkerState.SHULKER
                    }
                }
                ShulkerState.SHULKER -> {
                    if (it.pos == pos && it.newState.block is BlockShulkerBox) {
                        shulkerState = ShulkerState.OPEN
                    }
                }
                else -> {

                }
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (timeoutTimer.tickAndReset(5000L)) {
                disable()
                return@safeListener
            }

            if (timer.tick(0L)) {
                placeInfo?.let { (pos, side) ->
                    MainHandPause.withPause(AntiCamp, 100) {
                        when (mode) {
                            Mode.SHULKER -> {
                                shulkerMode(pos, side)
                            }
                            Mode.PISTON -> {

                            }
                        }
                    }
                } ?: disable()
            }

            lastHitVec?.let {
                sendPlayerPacket {
                    rotate(getRotationTo(it))
                }
            }
        }
    }

    private fun SafeClientEvent.getPlacingPos(target: EntityLivingBase): Pair<BlockPos, EnumFacing>? {
        if (player.getDistanceSq(target) > range * range) {
            return null
        }

        val feetPos = target.flooredPosition
        val xOffset = target.posX - (feetPos.x + 0.5)
        val zOffset = target.posZ - (feetPos.z + 0.5)

        if (world.isAir(feetPos) && !HoleManager.getHoleInfo(target).isHole) {
            return null
        }

        val headPos = feetPos.up()
        val x = if (xOffset >= 0.0) EnumFacing.EAST else EnumFacing.WEST
        val z = if (zOffset >= 0.0) EnumFacing.SOUTH else EnumFacing.NORTH

        val xPos = checkAxis(headPos, x)
        val zPos = checkAxis(headPos, z)

        return if (xPos != null && zPos != null) {
            if (abs(xOffset) < abs(zOffset)) {
                xPos
            } else {
                zPos
            }
        } else {
            xPos ?: zPos
        }
    }

    private fun SafeClientEvent.checkAxis(headPos: BlockPos, sideA: EnumFacing): Pair<BlockPos, EnumFacing>? {
        val sideB = sideA.opposite

        val posA = headPos.offset(sideA)
        val posB = headPos.offset(sideB)
        val blockStateA = world.getBlockState(posA)
        val blockStateB = world.getBlockState(posB)

        val airA = blockStateA.block == Blocks.AIR
        val airB = blockStateB.block == Blocks.AIR
        val shulkerA = isValidShulker(blockStateA, sideB)
        val shulkerB = isValidShulker(blockStateB, sideA)
        val entityA = world.checkNoEntityCollision(AxisAlignedBB(posA))
        val entityB = world.checkNoEntityCollision(AxisAlignedBB(posB))

        return when {
            airA && airB -> {
                when {
                    entityA -> {
                        posA to sideB
                    }
                    entityB -> {
                        posB to sideA
                    }
                    else -> {
                        null
                    }
                }
            }
            airA && shulkerB -> {
                posB to sideA
            }
            airB && shulkerA -> {
                posA to sideB
            }
            else -> {
                null
            }
        }
    }

    private fun isValidShulker(blockState: IBlockState, side: EnumFacing): Boolean {
        val block = blockState.block
        return block is BlockShulkerBox && blockState.properties[BlockShulkerBox.FACING] == side
    }

    private fun SafeClientEvent.shulkerMode(pos: BlockPos, side: EnumFacing) {
        val obbyPos = pos.offset(side.opposite)

        when (shulkerState) {
            ShulkerState.OBBY -> {
                placeObby(obbyPos)
            }
            ShulkerState.SHULKER -> {
                placeShulker(pos, obbyPos, side)
            }
            ShulkerState.OPEN -> {
                openShulker(pos)
            }
            ShulkerState.OPENING -> {
                openingShulker(pos)
            }
            ShulkerState.CLOSE -> {
                closeShulker()
            }
        }
    }

    private fun SafeClientEvent.placeObby(obbyPos: BlockPos) {
        if (!world.getBlockState(obbyPos).isReplaceable) {
            shulkerState = ShulkerState.SHULKER
            return
        }

        getPlacement(
            obbyPos,
            PlacementSearchOption.range(6.0),
            PlacementSearchOption.ENTITY_COLLISION
        )?.let {
            if (!swapToBlock(Blocks.OBSIDIAN)) {
                disable()
                return
            }

            timer.reset(placeDelay * 50L)
            lastHitVec = it.hitVec
            defaultScope.launch {
                delay(50L)
                onMainThreadSafe {
                    placeBlock(it)
                }
            }
        } ?: run {
            disable()
        }
    }

    private fun SafeClientEvent.placeShulker(pos: BlockPos, obbyPos: BlockPos, side: EnumFacing) {
        if (world.getBlock(pos) is BlockShulkerBox) {
            shulkerState = ShulkerState.OPEN
            return
        }

        if (!swapToBlock<BlockShulkerBox>()) {
            disable()
            return
        }

        val placeInfo = newPlaceInfo(obbyPos, side)
        timer.reset(placeDelay * 50L)
        lastHitVec = placeInfo.hitVec
        defaultScope.launch {
            delay(50L)
            onMainThreadSafe {
                placeBlock(placeInfo)
            }
        }
    }

    private fun SafeClientEvent.openShulker(pos: BlockPos) {
        val openSide = getMiningSide(pos) ?: EnumFacing.UP
        val offset = getHitVecOffset(openSide)

        timer.reset(openDelay * 50L)
        lastHitVec = pos.toVec3d(offset)
        defaultScope.launch {
            delay(50L)
            onMainThreadSafe {
                connection.sendPacket(
                    CPacketPlayerTryUseItemOnBlock(
                        pos,
                        openSide,
                        EnumHand.MAIN_HAND,
                        offset.x,
                        offset.y,
                        offset.z
                    )
                )
            }
        }
    }

    private fun SafeClientEvent.openingShulker(pos: BlockPos) {
        (world.getTileEntity(pos) as? TileEntityShulkerBox?)?.let {
            if (it.animationStatus == TileEntityShulkerBox.AnimationStatus.OPENED) {
                shulkerState = ShulkerState.CLOSE
                timer.reset(closeDelay * 50L)
            }
        }
    }

    private fun SafeClientEvent.closeShulker() {
        val id = windowID
        if (id != -1) {
            connection.sendPacket(CPacketCloseWindow(id))
        }
        disable()
    }
}