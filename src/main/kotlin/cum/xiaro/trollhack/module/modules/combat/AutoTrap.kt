package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.CombatManager
import cum.xiaro.trollhack.manager.managers.EntityManager
import cum.xiaro.trollhack.manager.managers.HotbarManager.spoofHotbar
import cum.xiaro.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.Bind
import cum.xiaro.trollhack.util.EntityUtils.flooredPosition
import cum.xiaro.trollhack.util.EntityUtils.spoofSneak
import cum.xiaro.trollhack.util.inventory.slot.HotbarSlot
import cum.xiaro.trollhack.util.inventory.slot.firstBlock
import cum.xiaro.trollhack.util.inventory.slot.hotbarSlots
import cum.xiaro.trollhack.util.items.blockBlacklist
import cum.xiaro.trollhack.util.math.RotationUtils.getRotationTo
import cum.xiaro.trollhack.util.math.vector.toBlockPos
import cum.xiaro.trollhack.util.text.MessageSendUtils
import cum.xiaro.trollhack.util.threads.defaultScope
import cum.xiaro.trollhack.util.threads.isActiveOrFalse
import cum.xiaro.trollhack.util.threads.runSafeSuspend
import cum.xiaro.trollhack.util.world.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

@CombatManager.CombatModule
internal object AutoTrap : Module(
    name = "AutoTrap",
    category = Category.COMBAT,
    description = "Traps your enemies in obsidian",
    modulePriority = 150
) {
    private val trapMode by setting("Trap Mode", TrapMode.FULL_TRAP)
    private val bindSelfTrap by setting("Bind Self Trap", Bind(), {
        if (it) {
            selfTrap = true
            enable()
        }
    })
    private val autoDisable by setting("Auto Disable", true)
    private val strictDirection by setting("Strict Direction", false)
    private val placeSpeed by setting("Places Per Tick", 4f, 0.25f..5f, 0.25f)
    private val rotation by setting("Rotation", true)

    private var selfTrap = false
    private var job: Job? = null

    override fun isActive(): Boolean {
        return isEnabled && job.isActiveOrFalse
    }

    init {
        onDisable {
            selfTrap = false
        }

        safeListener<TickEvent.Post> {
            if (!job.isActiveOrFalse && canRun()) job = runAutoTrap()

            if (job.isActiveOrFalse && rotation) {
                sendPlayerPacket {
                    cancelAll()
                }
            }
        }
    }

    private fun SafeClientEvent.canRun(): Boolean {
        (if (selfTrap) player else CombatManager.target)?.positionVector?.toBlockPos()?.let {
            for (offset in trapMode.offset) {
                if (!world.isPlaceable(it.add(offset))) continue
                return true
            }
        }
        return false
    }

    private fun SafeClientEvent.getObby(): HotbarSlot? {
        val slots = player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)

        if (slots == null) { // Obsidian check
            MessageSendUtils.sendNoSpamChatMessage("$chatName No obsidian in hotbar, disabling!")
            disable()
            return null
        }

        return slots
    }

    private fun SafeClientEvent.runAutoTrap() = defaultScope.launch {
        val entity = if (selfTrap) player else CombatManager.target ?: return@launch

        val emptySet = emptySet<BlockPos>()
        val placed = HashSet<BlockPos>()
        var placeCount = 0
        var lastInfo = getStructurePlaceInfo(entity.flooredPosition,
            trapMode.offset,
            emptySet, 3,
            4.25f,
            strictDirection
        )

        while (lastInfo != null) {
            if (!(isEnabled && CombatManager.isOnTopPriority(AutoTrap))) break

            val placingInfo = getStructurePlaceInfo(entity.flooredPosition, trapMode.offset, placed, 3, 4.25f, strictDirection)
                ?: lastInfo

            placeCount++
            placed.add(placingInfo.placedPos)
            val slot = getObby() ?: break

            runSafeSuspend {
                doPlace(placingInfo, slot)
            }

            if (placeCount >= 4) {
                delay(100L)
                placeCount = 0
                placed.clear()
            }

            lastInfo = getStructurePlaceInfo(entity.flooredPosition, trapMode.offset, emptySet, 3, 4.25f, strictDirection)
        }

        if (autoDisable) disable()
    }

    private fun SafeClientEvent.getStructurePlaceInfo(
        center: BlockPos,
        structureOffset: Array<BlockPos>,
        toIgnore: Set<BlockPos>,
        attempts: Int,
        range: Float,
        visibleSideCheck: Boolean
    ): PlaceInfo? {
        for (offset in structureOffset) {
            val pos = center.add(offset)
            if (toIgnore.contains(pos)) continue
            if (!world.getBlockState(pos).isReplaceable) continue
            if (!EntityManager.checkEntityCollision(AxisAlignedBB(pos))) continue

            return getNeighbor(pos, attempts, range, visibleSideCheck) ?: continue
        }

        if (attempts > 1) return getStructurePlaceInfo(center, structureOffset, toIgnore, attempts - 1, range, visibleSideCheck)

        return null
    }

    private suspend fun SafeClientEvent.doPlace(
        placeInfo: PlaceInfo,
        slot: HotbarSlot
    ) {
        val placePacket = placeInfo.toPlacePacket(EnumHand.MAIN_HAND)
        val needRotation = this@AutoTrap.rotation

        if (needRotation) {
            val rotation = getRotationTo(placeInfo.hitVec)
            val rotationPacket = CPacketPlayer.PositionRotation(player.posX, player.posY, player.posZ, rotation.x, rotation.y, player.onGround)
            connection.sendPacket(rotationPacket)
            delay((40.0f / placeSpeed).toLong())
        }

player.spoofSneak {
    spoofHotbar(slot) {
        connection.sendPacket(placePacket)
    }
}


        player.swingArm(EnumHand.MAIN_HAND)
        if (needRotation) {
            delay((10.0f / placeSpeed).toLong())
        } else {
            delay((50.0f / placeSpeed).toLong())
        }
    }

    @Suppress("UNUSED")
    private enum class TrapMode(val offset: Array<BlockPos>) {
        FULL_TRAP(arrayOf(
            BlockPos(1, 0, 0),
            BlockPos(-1, 0, 0),
            BlockPos(0, 0, 1),
            BlockPos(0, 0, -1),
            BlockPos(1, 1, 0),
            BlockPos(-1, 1, 0),
            BlockPos(0, 1, 1),
            BlockPos(0, 1, -1),
            BlockPos(0, 2, 0)
        )),
        CRYSTAL_TRAP(arrayOf(
            BlockPos(1, 1, 1),
            BlockPos(1, 1, 0),
            BlockPos(1, 1, -1),
            BlockPos(0, 1, -1),
            BlockPos(-1, 1, -1),
            BlockPos(-1, 1, 0),
            BlockPos(-1, 1, 1),
            BlockPos(0, 1, 1),
            BlockPos(0, 2, 0)
        ))
    }
}