package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import cum.xiaro.trollhack.event.events.player.PlayerTravelEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.HotbarManager.spoofHotbar
import cum.xiaro.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import cum.xiaro.trollhack.mixin.entity.MixinEntity
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.EntityUtils.lastTickPos
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.TimeUnit
import cum.xiaro.trollhack.util.accessor.syncCurrentPlayItem
import cum.xiaro.trollhack.util.inventory.slot.HotbarSlot
import cum.xiaro.trollhack.util.inventory.slot.firstItem
import cum.xiaro.trollhack.util.inventory.slot.hotbarSlots
import cum.xiaro.trollhack.util.math.RotationUtils.getRotationTo
import cum.xiaro.trollhack.util.math.vector.toBlockPos
import cum.xiaro.trollhack.util.threads.defaultScope
import cum.xiaro.trollhack.util.threads.onMainThreadSafeSuspend
import cum.xiaro.trollhack.util.world.PlaceInfo
import cum.xiaro.trollhack.util.world.getNeighbor
import cum.xiaro.trollhack.util.world.placeBlock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * @see MixinEntity.moveInvokeIsSneakingPre
 * @see MixinEntity.moveInvokeIsSneakingPost
 */
internal object Scaffold : Module(
    name = "Scaffold",
    category = Category.PLAYER,
    description = "Places blocks under you",
    modulePriority = 500
) {
    private val tower by setting("Tower", true)
    val safeWalk by setting("Safe Walk", true)
    private val strictDirection by setting("Strict Direction", false)
    private val delay by setting("Delay", 2, 1..10, 1)
    private val maxRange by setting("Max Range", 1, 0..3, 1)

    private var lastHitVec: Vec3d? = null
    private var placeInfo: PlaceInfo? = null
    private var inactiveTicks = 69

    private val placeTimer = TickTimer(TimeUnit.TICKS)
    private val rubberBandTimer = TickTimer(TimeUnit.TICKS)

    override fun isActive(): Boolean {
        return isEnabled && inactiveTicks <= 5
    }

    init {
        onDisable {
            placeInfo = null
            inactiveTicks = 69
        }

        listener<PacketEvent.Receive> {
            if (it.packet !is SPacketPlayerPosLook) return@listener
            rubberBandTimer.reset()
        }

        safeListener<PlayerTravelEvent> {
            if (!tower || !mc.gameSettings.keyBindJump.isKeyDown || inactiveTicks > 5) return@safeListener
            if (rubberBandTimer.tick(10)) {
                if (shouldTower) player.motionY = 0.41999998688697815
            } else if (player.fallDistance <= 2.0f) {
                player.motionY = -0.169
            }
        }
    }

    private val SafeClientEvent.shouldTower: Boolean
        get() = !player.onGround
            && player.posY - floor(player.posY) <= 0.1

    init {
        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            inactiveTicks++
            placeInfo = calcNextPos()?.let {
                getNeighbor(it, 1, visibleSideCheck = strictDirection, sides = arrayOf(EnumFacing.DOWN))
                    ?: getNeighbor(it, 3, visibleSideCheck = strictDirection, sides = EnumFacing.HORIZONTALS)
            }

            placeInfo?.let {
                lastHitVec = it.hitVec
                swapAndPlace(it)
            }

            if (inactiveTicks <= 5) {
                lastHitVec?.let {
                    sendPlayerPacket {
                        rotate(getRotationTo(it))
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.calcNextPos(): BlockPos? {
        val posVec = player.positionVector
        val blockPos = posVec.toBlockPos()
        return checkPos(blockPos)
            ?: run {
                val realMotion = posVec.subtract(player.lastTickPos)
                val nextPos = blockPos.add(roundToRange(realMotion.x), 0, roundToRange(realMotion.z))
                checkPos(nextPos)
            }
    }

    private fun SafeClientEvent.checkPos(blockPos: BlockPos): BlockPos? {
        val center = Vec3d(blockPos.x + 0.5, blockPos.y.toDouble(), blockPos.z + 0.5)
        val rayTraceResult = world.rayTraceBlocks(
            center,
            center.subtract(0.0, 0.5, 0.0),
            false,
            true,
            false
        )
        return blockPos.down().takeIf { rayTraceResult?.typeOfHit != RayTraceResult.Type.BLOCK }
    }

    private fun roundToRange(value: Double) =
        (value * 2.5 * maxRange).roundToInt().coerceAtMost(maxRange)

    private fun SafeClientEvent.swapAndPlace(placeInfo: PlaceInfo) {
        getBlockSlot()?.let { slot ->
            inactiveTicks = 0

            if (placeTimer.tickAndReset(delay.toLong())) {
                defaultScope.launch {
                    delay(5)
                    onMainThreadSafeSuspend {
                        spoofHotbar(slot) {
                            placeBlock(placeInfo)
                        }
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.getBlockSlot(): HotbarSlot? {
        playerController.syncCurrentPlayItem()
        return player.hotbarSlots.firstItem<ItemBlock, HotbarSlot>()
    }
}