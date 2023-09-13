package dev.luna5ama.trollhack.module.modules.player

import dev.fastmc.common.TickTimer
import dev.fastmc.common.ceilToInt
import dev.fastmc.common.sq
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.*
import dev.luna5ama.trollhack.event.events.player.HotbarUpdateEvent
import dev.luna5ama.trollhack.event.events.player.InteractEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeConcurrentListener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.InventoryTaskManager
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.translation.TranslateType
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.SwingMode
import dev.luna5ama.trollhack.util.extension.synchronized
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.inventory.InventoryTask
import dev.luna5ama.trollhack.util.inventory.findBestTool
import dev.luna5ama.trollhack.util.inventory.isTool
import dev.luna5ama.trollhack.util.inventory.slot.allSlots
import dev.luna5ama.trollhack.util.inventory.slot.hotbarIndex
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.math.isInSight
import dev.luna5ama.trollhack.util.math.scale
import dev.luna5ama.trollhack.util.math.vector.distanceSqToCenter
import dev.luna5ama.trollhack.util.math.vector.toVec3dCenter
import dev.luna5ama.trollhack.util.threads.runSafe
import dev.luna5ama.trollhack.util.world.canBreakBlock
import dev.luna5ama.trollhack.util.world.getMiningSide
import dev.luna5ama.trollhack.util.world.isAir
import net.minecraft.block.state.IBlockState
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.init.Blocks
import net.minecraft.init.Enchantments
import net.minecraft.init.MobEffects
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.server.SPacketBlockChange
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import kotlin.collections.set

internal object PacketMine : Module(
    name = "Packet Mine",
    alias = arrayOf("InstantMine"),
    category = Category.PLAYER,
    description = "Break block with packet",
    modulePriority = 200
) {
    private val miningMode by setting("Mining Mode", MiningMode.NORMAL_RETRY)
    private val autoSwitch by setting("Auto Switch", true)
    private val ghostSwitchBypass by setting("Ghost Switch Bypass", HotbarSwitchManager.Override.SWAP)
    private val cancelOnSwitch by setting("Cancel On Switch", false)
    private val rotation by setting("Rotation", false)
    private val rotateTime by setting("Rotate Time", 100, 0..1000, 10, ::rotation)
    private val startPacketOnClick by setting("Start Packet On Click", true)
    private val endPacketOnBreak by setting("End Packet On Break", true)
    private val startPacketAfterBreak by setting("Start Packet After Break", false, { miningMode.continous })
    private val endPacketAfterBreak by setting("End Packet After Break", false, { miningMode.continous })
    private val spamPackets0 = setting("Spam Packets", true)
    private val spamPackets by spamPackets0
    val noSwing by setting("No Swing", false)
    private val noAnimation by setting("No Animation", false)
    private val swingMode by setting("Swing Mode", SwingMode.CLIENT)
    private val packetDelay by setting("Packet Delay", 100, 0..1000, 5)
    private val breakTimeMultiplier by setting("Break Time Multiplier", 0.8f, 0.5f..2.0f, 0.01f)
    private val breakTimeBias by setting("Break Time Bias", 0, -5000..5000, 50)
    private val miningTaskTimeout by setting("Mining Task Timeout", 3000, 0..10000, 50)
    private val range by setting("Range", 4.5f, 0.0f..10.0f, 0.1f)
    private val removeOutOfRange by setting("Remove Out Of Range", false)

    private val clickTimer = TickTimer()
    private val renderer = ESPRenderer().apply { aFilled = 31; aOutline = 233 }
    private val packetTimer = TickTimer()
    private var miningInfo0: MiningInfo? = null
    private var breakConfirm: BreakConfirmInfo? = null
    val miningInfo: IMiningInfo? get() = miningInfo0

    private val miningQueue = HashMap<AbstractModule, MiningTask>().synchronized()

    private var lastTask: InventoryTask? = null

    private enum class MiningMode(override val displayName: CharSequence, val continous: Boolean) : DisplayEnum {
        NORMAL_ONCE(TranslateType.SPECIFIC key "Normal Once", false),
        NORMAL_RETRY(TranslateType.SPECIFIC key "Normal Retry", false),
        CONTINUOUS_NORMAL(TranslateType.SPECIFIC key "Continuous Normal", true),
        CONTINUOUS_INSTANT(TranslateType.SPECIFIC key "Continuous Instant", true),
    }

    override fun isActive(): Boolean {
        return isEnabled && miningInfo0 != null
    }

    init {
        onDisable {
            lastTask = null
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
                miningInfo.miningTimeout = System.currentTimeMillis()

                val newBlockState = event.packet.blockState
                val currentState = world.getBlockState(miningInfo.pos)
                val current = currentState.block
                val new = newBlockState.block

                if (new != Blocks.AIR) {
                    breakConfirm = null
                    if (new != current) {
                        miningInfo.isAir = false

                        if (miningMode == MiningMode.CONTINUOUS_INSTANT && miningInfo.mined) {
                            finishMining(newBlockState, miningInfo)
                        } else if (miningMode == MiningMode.CONTINUOUS_NORMAL) {
                            miningInfo.updateLength(this)
                            if (System.currentTimeMillis() < miningInfo.endTime) {
                                reset(PacketMine)
                                mineBlock(miningInfo.owner, miningInfo.pos, miningInfo.owner.modulePriority)
                            }
                        }
                    }
                } else if (new != current) {
                    breakConfirm = BreakConfirmInfo(
                        miningInfo.pos,
                        System.currentTimeMillis() + 50L,
                        miningInfo.mined || currentState.getBlockHardness(world, miningInfo.pos) > 0.0f
                    )
                }
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (rotation) {
                miningInfo0?.let {
                    if (!it.isAir
                        && (miningMode == MiningMode.CONTINUOUS_INSTANT && it.mined
                            || it.endTime - System.currentTimeMillis() <= rotateTime)
                    ) {
                        sendPlayerPacket {
                            rotate(getRotationTo(it.pos.toVec3dCenter()))
                        }
                    }
                }
            }
        }

        listener<InteractEvent.Block.LeftClick> {
            mineBlock(PacketMine, it.pos, modulePriority)
            if (miningInfo0?.pos == it.pos) it.cancel()
        }

        listener<InteractEvent.Block.Damage> {
            mineBlock(PacketMine, it.pos, modulePriority)
            if (it.pos == miningInfo0?.pos) it.cancel()
        }

        safeConcurrentListener<TickEvent.Post> {
            updateMining()
        }

        listener<HotbarUpdateEvent> {
            if (cancelOnSwitch) {
                reset()
            }
        }

        safeListener<RunGameLoopEvent.Tick> {
            val miningInfo = miningInfo0 ?: return@safeListener
            val breakConfirm = breakConfirm

            if (breakConfirm != null && System.currentTimeMillis() < breakConfirm.time) {
                miningInfo.isAir = true
                miningInfo.mined = breakConfirm.instantMineable

                if (!miningMode.continous) {
                    reset()
                    miningQueue.remove(miningInfo.owner)
                } else if (miningMode.continous) {
                    if (!breakConfirm.instantMineable) {
                        miningInfo.updateLength(this)
                        connection.sendPacket(
                            CPacketPlayerDigging(
                                CPacketPlayerDigging.Action.START_DESTROY_BLOCK,
                                miningInfo.pos,
                                miningInfo.side
                            )
                        )
                    } else {
                        if (miningMode == MiningMode.CONTINUOUS_NORMAL) {
                            miningInfo.startTime = System.currentTimeMillis()
                        }
                        if (startPacketAfterBreak) {
                            connection.sendPacket(
                                CPacketPlayerDigging(
                                    CPacketPlayerDigging.Action.START_DESTROY_BLOCK,
                                    miningInfo.pos,
                                    miningInfo.side
                                )
                            )
                        }
                        if (endPacketAfterBreak) {
                            connection.sendPacket(
                                CPacketPlayerDigging(
                                    CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                                    miningInfo.pos,
                                    miningInfo.side
                                )
                            )
                        }
                    }
                }

                this@PacketMine.breakConfirm = null
            }

            if (player.distanceSqToCenter(miningInfo.pos) > range.sq) {
                reset()
                return@safeListener
            }

            val blockState = world.getBlockState(miningInfo.pos)
            miningInfo.isAir = blockState.block == Blocks.AIR

            if (isFinished(miningInfo, blockState) && checkRotation(miningInfo)) {
                if (packetTimer.tick(packetDelay)) {
                    if (finishMining(blockState, miningInfo)) {
                        if (!miningMode.continous) {
                            reset(miningInfo.owner)
                            if (miningMode == MiningMode.NORMAL_RETRY) {
                                mineBlock(miningInfo.owner, miningInfo.pos, miningInfo.owner.modulePriority)
                            }
                        }
                    }
                }
            } else if (spamPackets) {
                if (packetTimer.tick(packetDelay)) {
                    sendMiningPacket(miningInfo, false)
                }
            } else {
                packetTimer.reset(-114514)
            }
        }
    }

    private fun SafeClientEvent.finishMining(blockState: IBlockState, miningInfo: MiningInfo): Boolean {
        val bestTool = findBestTool(blockState) ?: return false

        synchronized(InventoryTaskManager) {
            if (bestTool.hotbarIndex == HotbarSwitchManager.serverSideHotbar) {
                sendMiningPacket(miningInfo, true)
                return true
            } else if (autoSwitch) {
                ghostSwitch(ghostSwitchBypass, bestTool) {
                    sendMiningPacket(miningInfo, true)
                }
                return true
            }
        }

        return false
    }

    fun mineBlock(module: AbstractModule, pos: BlockPos, priority: Int, once: Boolean = false) {
        runSafe {
            val prev = miningQueue[module]
            if (prev == null || prev.pos != pos || prev.priority != priority) {
                miningQueue[module] = MiningTask(module, pos, priority, once)
            }
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
        var maxPriorityTask: MiningTask? = null

        miningInfo0?.let {
            val currentTime = System.currentTimeMillis()
            if (!world.isAir(it.pos)) {
                it.miningTimeout = currentTime
            } else if (currentTime > it.miningTimeout) {
                miningQueue.remove(it.owner)
            }
        }

        synchronized(miningQueue) {
            if (miningQueue.isEmpty()) return@synchronized
            val sorted = miningQueue.values.toTypedArray()
            sorted.sort()

            val rangeSq = range * range

            for (task in sorted) {
                if (player.distanceSqToCenter(task.pos) > rangeSq) {
                    if (removeOutOfRange) {
                        miningQueue.remove(task.owner)
                    } else {
                        continue
                    }
                }

                if (!world.canBreakBlock(task.pos)) {
                    miningQueue.remove(task.owner) // Remove invalid tasks
                    continue
                }
                if ((!miningMode.continous || task.once) && world.isAir(task.pos)) {
                    miningQueue.remove(task.owner) // Remove finished tasks
                    continue
                }

                maxPriorityTask = task
                break
            }
        }

        maxPriorityTask?.let {
            if (it.pos != miningInfo0?.pos) {
                startMining(it)
            } else {
                miningInfo0?.updateLength(this)
            }
        } ?: run {
            reset()
        }
    }

    private fun SafeClientEvent.startMining(task: MiningTask) {
        val breakTime = calcBreakTime(task.pos)
        if (breakTime == -1) return

        reset()
        val side = getMiningSide(task.pos) ?: run {
            val vector = player.eyePosition.subtract(task.pos.x + 0.5, task.pos.y + 0.5, task.pos.z + 0.5)
            EnumFacing.getFacingFromVector(vector.x.toFloat(), vector.y.toFloat(), vector.z.toFloat())
        }
        miningInfo0 = MiningInfo(this, task.owner, task.pos, side)
        packetTimer.reset(-69420)

        if (startPacketOnClick || breakTime == 0) connection.sendPacket(
            CPacketPlayerDigging(
                CPacketPlayerDigging.Action.START_DESTROY_BLOCK,
                task.pos,
                side
            )
        )
        connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, task.pos, side))

        if (noAnimation) connection.sendPacket(
            CPacketPlayerDigging(
                CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK,
                task.pos,
                side
            )
        )
        if (!noSwing) swingMode.swingHand(this, EnumHand.MAIN_HAND)
    }

    private fun SafeClientEvent.checkRotation(miningInfo: MiningInfo): Boolean {
        val eyeHeight = player.getEyeHeight().toDouble()
        return !rotation || AxisAlignedBB(miningInfo.pos).isInSight(
            PlayerPacketManager.position.add(
                0.0,
                eyeHeight,
                0.0
            ), rotation = PlayerPacketManager.rotation
        )
    }

    private fun isFinished(miningInfo: MiningInfo, blockState: IBlockState): Boolean {
        return (!miningInfo.isAir || blockState.block != Blocks.AIR)
            && (miningMode == MiningMode.CONTINUOUS_INSTANT && miningInfo.mined || System.currentTimeMillis() > miningInfo.endTime)
    }

    private fun SafeClientEvent.calcBreakTime(pos: BlockPos): Int {
        val blockState = world.getBlockState(pos)

        val hardness = blockState.getBlockHardness(world, pos)
        val breakSpeed = getBreakSpeed(blockState)

        if (hardness == 0.0f) {
            return 0
        }

        val relativeDamage = breakSpeed / hardness / 30.0f
        val ticks = (0.7f / relativeDamage).ceilToInt()

        if (ticks <= 0) {
            return 0
        }

        return (ticks * 50 * breakTimeMultiplier + breakTimeBias).ceilToInt()
    }

    private fun SafeClientEvent.getBreakSpeed(blockState: IBlockState): Float {
        var maxSpeed = 1.0f

        for (slot in player.allSlots) {
            val stack = slot.stack

            if (stack.isEmpty || !stack.item.isTool) {
                continue
            } else {
                var speed = stack.getDestroySpeed(blockState)

                if (speed > 1.0f) {
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

        player.getActivePotionEffect(MobEffects.SPEED)?.let {
            maxSpeed += maxSpeed * (it.amplifier + 1.0f) * 0.2f
        }

        return maxSpeed
    }

    private fun SafeClientEvent.sendMiningPacket(miningInfo: MiningInfo, end: Boolean) {
        if (endPacketOnBreak && end) connection.sendPacket(
            CPacketPlayerDigging(
                CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                miningInfo.pos,
                miningInfo.side
            )
        )
        if (noAnimation && !miningInfo.mined) connection.sendPacket(
            CPacketPlayerDigging(
                CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK,
                miningInfo.pos,
                miningInfo.side
            )
        )
        packetTimer.reset()
        if (end) swingMode.swingHand(this, EnumHand.MAIN_HAND)
    }

    private fun reset() {
        packetTimer.reset(-69420)
        miningInfo0?.let {
            miningInfo0 = null
        }
    }

    fun isInstantMining(pos: BlockPos): Boolean {
        return isEnabled
            && miningMode == MiningMode.CONTINUOUS_INSTANT
            && miningInfo0?.let { it.mined && it.pos == pos } ?: false
    }

    private class MiningTask(val owner: AbstractModule, val pos: BlockPos, val priority: Int, val once: Boolean) :
        Comparable<MiningTask> {
        override fun compareTo(other: MiningTask): Int {
            return -priority.compareTo(other.priority)
        }
    }

    interface IMiningInfo {
        val pos: BlockPos
        val side: EnumFacing
    }

    private class MiningInfo(
        event: SafeClientEvent,
        val owner: AbstractModule,
        override val pos: BlockPos,
        override val side: EnumFacing,
    ) : IMiningInfo {
        var length = event.calcBreakTime(pos); private set
        var startTime = System.currentTimeMillis()
            set(value) {
                field = value
                miningTimeout = value
            }
        val endTime get() = startTime + length
        var isAir = false
        var mined = false

        var miningTimeout = startTime
            set(value) {
                field = value + miningTaskTimeout
            }

        fun updateLength(event: SafeClientEvent) {
            length = event.calcBreakTime(pos)
        }
    }

    private data class BreakConfirmInfo(val pos: BlockPos, val time: Long, val instantMineable: Boolean)
}