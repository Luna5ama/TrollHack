package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.util.extension.fastCeil
import cum.xiaro.trollhack.util.extension.lastEntryOrNull
import cum.xiaro.trollhack.util.extension.sq
import cum.xiaro.trollhack.util.extension.synchronized
import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.util.graphics.Easing
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.*
import cum.xiaro.trollhack.event.events.player.InteractEvent
import cum.xiaro.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import cum.xiaro.trollhack.event.events.render.Render3DEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeConcurrentListener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.HotbarManager.spoofHotbar
import cum.xiaro.trollhack.manager.managers.PlayerPacketManager
import cum.xiaro.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import cum.xiaro.trollhack.module.AbstractModule
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.EntityUtils.eyePosition
import cum.xiaro.trollhack.util.SwingMode
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.graphics.ESPRenderer
import cum.xiaro.trollhack.util.inventory.findBestTool
import cum.xiaro.trollhack.util.inventory.slot.hotbarSlots
import cum.xiaro.trollhack.util.items.isTool
import cum.xiaro.trollhack.util.math.RotationUtils.getRotationTo
import cum.xiaro.trollhack.util.math.isInSight
import cum.xiaro.trollhack.util.math.scale
import cum.xiaro.trollhack.util.math.vector.toVec3dCenter
import cum.xiaro.trollhack.util.threads.runSafe
import cum.xiaro.trollhack.util.world.canBreakBlock
import cum.xiaro.trollhack.util.world.getBlock
import cum.xiaro.trollhack.util.world.getMiningSide
import cum.xiaro.trollhack.util.world.isAir
import net.minecraft.block.state.IBlockState
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.init.Blocks
import net.minecraft.init.Enchantments
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.server.SPacketBlockChange
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import java.util.*
import kotlin.collections.set

internal object PacketMine : Module(
    name = "PacketMine",
    alias = arrayOf("InstantMine"),
    category = Category.PLAYER,
    description = "Break block with packet",
    modulePriority = 200
) {
    private val instantMine0 = setting("Instant Mine", false)
    private val instantMine by instantMine0
    private val rotation by setting("Rotation", false)
    private val rotateTime by setting("Rotate Time", 100, 0..1000, 10, ::rotation)
    private val spamPackets0 = setting("Spam Packets", true)
    private val spamPackets by spamPackets0
    val noSwing by setting("No Swing", false)
    private val noAnimation by setting("No Animation", false)
    private val swingMode by setting("Swing Mode", SwingMode.CLIENT)
    private val packetDelay by setting("Packet Delay", 50, 0..1000, 5)
    private val breakOffset by setting("Break Offset", 200, -5000..5000, 50)
    private val range by setting("Range", 8.0f, 0.0f..10.0f, 0.25f)

    private val clickTimer = TickTimer()
    private val renderer = ESPRenderer().apply { aFilled = 31; aOutline = 233 }
    private val packetTimer = TickTimer()
    private var miningInfo0: MiningInfo? = null
    val miningInfo: IMiningInfo?
        get() = miningInfo0

    private val miningQueue = TreeMap<AbstractModule, BlockPos>().synchronized()

    override fun isActive(): Boolean {
        return isEnabled && miningInfo0 != null
    }

    init {
        onDisable {
            miningQueue.clear()
            reset()
        }

        listener<ConnectionEvent.Disconnect> {
            miningQueue.clear()
            reset()
        }

        listener<InputEvent.Mouse> {
            if (it.button == 0 && it.state && mc.currentScreen == null) {
                if (!clickTimer.tickAndReset(250L)) reset(this)
            }
        }

        listener<Render3DEvent> {
            miningInfo0?.let {
                val multiplier = Easing.OUT_CUBIC.inc(Easing.toDelta(it.startTime, it.length))
                val box = AxisAlignedBB(it.pos).scale(multiplier.toDouble())
                val color = if (it.isAir) ColorRGB(32, 255, 32) else ColorRGB(255, 32, 32)

                renderer.add(box, color)
                renderer.render(true)
            }
        }

        safeListener<PacketEvent.Receive> { event ->
            val miningInfo = miningInfo0 ?: return@safeListener

            if (event.packet is SPacketBlockChange && event.packet.blockPosition == miningInfo.pos) {
                val newBlockState = event.packet.blockState
                val current = world.getBlock(miningInfo.pos)
                val new = newBlockState.block

                if (new != current) {
                    if (new == Blocks.AIR) {
                        miningInfo.isAir = true
                        miningInfo.mined = true

                        if (!instantMine) {
                            reset()
                            miningQueue.values.remove(miningInfo.pos)
                        }
                    } else {
                        miningInfo.isAir = false

                        if (instantMine) {
                            findBestTool(newBlockState)?.let {
                                spoofHotbar(it) {
                                    sendMiningPacket(miningInfo)
                                }
                            }
                        }
                    }
                }
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (rotation) {
                miningInfo0?.let {
                    if (!it.isAir && (instantMine && it.mined || it.endTime - System.currentTimeMillis() <= rotateTime)) {
                        sendPlayerPacket {
                            rotate(getRotationTo(it.pos.toVec3dCenter()))
                        }
                    }
                }
            }
        }

        listener<InteractEvent.Block.LeftClick> {
            mineBlock(this, it.pos)
            if (miningInfo0?.pos == it.pos) it.cancel()
        }

        listener<InteractEvent.Block.Damage> {
            mineBlock(PacketMine, it.pos)
            if (it.pos == miningInfo0?.pos) it.cancel()
        }

        safeConcurrentListener<TickEvent.Post> {
            updateMining()
        }

        safeListener<RunGameLoopEvent.Tick> {
            val miningInfo = miningInfo0 ?: return@safeListener
            if (player.getDistanceSqToCenter(miningInfo.pos) > range.sq) {
                reset()
                return@safeListener
            }

            val blockState = world.getBlockState(miningInfo.pos)
            miningInfo.isAir = blockState.block == Blocks.AIR

            if (spamPackets) {
                if (packetTimer.tick(packetDelay)) {
                    val slot = findBestTool(blockState)
                    if (slot != null && isFinished(miningInfo, blockState) && checkRotation(miningInfo)) {
                        spoofHotbar(slot) {
                            sendMiningPacket(miningInfo)
                        }
                        swingMode.swingHand(this, EnumHand.MAIN_HAND)
                    } else {
                        sendMiningPacket(miningInfo)
                    }
                }
            } else {
                if (isFinished(miningInfo, blockState) && checkRotation(miningInfo)) {
                    findBestTool(blockState)?.let {
                        spoofHotbar(it) {
                            sendMiningPacket(miningInfo)
                            swingMode.swingHand(this, EnumHand.MAIN_HAND)
                        }
                    }
                    reset(miningInfo.owner)
                }
            }
        }
    }

    fun mineBlock(module: AbstractModule, pos: BlockPos) {
        runSafe {
            miningQueue[module] = pos
            updateMining()
        }
    }

    fun reset(module: AbstractModule) {
        runSafe {
            miningQueue.remove(module)
            updateMining()
        }
    }

    private fun SafeClientEvent.updateMining() {
        var lastPair = miningQueue.lastEntryOrNull()

        while (lastPair != null && (lastPair.key.isDisabled || !instantMine && world.isAir(lastPair.value))) {
            lastPair = miningQueue.pollLastEntry()
        }

        lastPair?.let {
            mineBlock(it.key, it.value)
        } ?: run {
            reset()
        }
    }

    private fun SafeClientEvent.mineBlock(owner: AbstractModule, pos: BlockPos) {
        if (world.canBreakBlock(pos) && pos != miningInfo0?.pos) {
            if (player.getDistanceSqToCenter(pos) > range * range) return

            val breakTime = calcBreakTime(pos)
            if (breakTime == -1L) return

            reset()
            val side = getMiningSide(pos) ?: run {
                val vector = player.eyePosition.subtract(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
                EnumFacing.getFacingFromVector(vector.x.toFloat(), vector.y.toFloat(), vector.z.toFloat())
            }
            miningInfo0 = MiningInfo(owner, pos, side, breakTime + breakOffset)
            packetTimer.reset(-69420)

            connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, side))
            connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, side))
            if (noAnimation) connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, pos, side))
            if (!noSwing) swingMode.swingHand(this, EnumHand.MAIN_HAND)
        }
    }

    private fun SafeClientEvent.checkRotation(miningInfo: MiningInfo): Boolean {
        val eyeHeight = player.getEyeHeight().toDouble()
        return !rotation || AxisAlignedBB(miningInfo.pos).isInSight(PlayerPacketManager.position.add(0.0, eyeHeight, 0.0), rotation = PlayerPacketManager.rotation) != null
    }

    private fun isFinished(miningInfo: MiningInfo, blockState: IBlockState): Boolean {
        return (!miningInfo.isAir || blockState.block != Blocks.AIR)
            && (instantMine && miningInfo.mined || System.currentTimeMillis() > miningInfo.endTime)
    }

    private fun SafeClientEvent.calcBreakTime(pos: BlockPos): Long {
        val blockState = world.getBlockState(pos)

        val hardness = blockState.getBlockHardness(world, pos)
        val breakSpeed = getBreakSpeed(blockState)
        if (breakSpeed == -1.0f) {
            return -1L
        }

        val relativeDamage = breakSpeed / hardness / 30.0f
        val ticks = (0.7f / relativeDamage).fastCeil()

        return ticks * 50L
    }

    private fun SafeClientEvent.getBreakSpeed(blockState: IBlockState): Float {
        var maxSpeed = 1.0f

        for (slot in player.hotbarSlots) {
            val stack = slot.stack

            if (stack.isEmpty || !stack.item.isTool) {
                continue
            } else {
                var speed = stack.getDestroySpeed(blockState)

                if (speed <= 1.0f) {
                    continue
                } else {
                    val efficiency = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack)
                    if (efficiency > 0) {
                        speed += efficiency * efficiency + 1.0f
                    }
                }

                if (speed > maxSpeed) {
                    maxSpeed = speed
                }
            }
        }

        return maxSpeed
    }

    private fun SafeClientEvent.sendMiningPacket(miningInfo: MiningInfo) {
        connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, miningInfo.pos, miningInfo.side))
        if (noAnimation && !miningInfo.mined) connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, miningInfo.pos, miningInfo.side))
        packetTimer.reset()
    }

    private fun reset() {
        packetTimer.reset(-69420)
        miningInfo0?.let {
            miningInfo0 = null
        }
    }

    interface IMiningInfo {
        val pos: BlockPos
        val side: EnumFacing
    }

    private class MiningInfo(
        val owner: AbstractModule,
        override val pos: BlockPos,
        override val side: EnumFacing,
        val length: Long
    ) : IMiningInfo {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + length
        var isAir = false
        var mined = false
    }
}