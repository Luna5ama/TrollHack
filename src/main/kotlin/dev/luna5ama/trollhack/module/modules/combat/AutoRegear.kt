package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.BlockPosUtil
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSetDeadEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.gui.hudgui.elements.client.Notification
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.module.modules.player.InventorySorter
import dev.luna5ama.trollhack.module.modules.player.Kit
import dev.luna5ama.trollhack.util.Bind
import dev.luna5ama.trollhack.util.EntityUtils.isFriend
import dev.luna5ama.trollhack.util.EntityUtils.isSelf
import dev.luna5ama.trollhack.util.EntityUtils.spoofSneak
import dev.luna5ama.trollhack.util.EntityUtils.spoofUnSneak
import dev.luna5ama.trollhack.util.TickTimer
import dev.luna5ama.trollhack.util.extension.synchronized
import dev.luna5ama.trollhack.util.inventory.InventoryTask
import dev.luna5ama.trollhack.util.inventory.executedOrTrue
import dev.luna5ama.trollhack.util.inventory.inventoryTask
import dev.luna5ama.trollhack.util.inventory.isStackable
import dev.luna5ama.trollhack.util.inventory.operation.pickUp
import dev.luna5ama.trollhack.util.inventory.operation.quickMove
import dev.luna5ama.trollhack.util.inventory.operation.swapWith
import dev.luna5ama.trollhack.util.inventory.slot.*
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.math.VectorUtils
import dev.luna5ama.trollhack.util.math.VectorUtils.setAndAdd
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.math.vector.distanceTo
import dev.luna5ama.trollhack.util.threads.ConcurrentScope
import dev.luna5ama.trollhack.util.threads.runSynchronized
import dev.luna5ama.trollhack.util.world.PlaceInfo.Companion.newPlaceInfo
import dev.luna5ama.trollhack.util.world.isAir
import dev.luna5ama.trollhack.util.world.isReplaceable
import dev.luna5ama.trollhack.util.world.placeBlock
import dev.luna5ama.trollhack.util.world.rayTraceVisible
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import kotlinx.coroutines.launch
import net.minecraft.block.BlockShulkerBox
import net.minecraft.inventory.Container
import net.minecraft.inventory.ContainerShulkerBox
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemShulkerBox
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

internal object AutoRegear : Module(
    name = "Auto Regear",
    description = "Automatically regear using container",
    category = Category.COMBAT
) {
    private val placeShulkerKey by setting("Place Shulker Key", Bind(), { placeShulker = true })
    private val placeRange by setting("Place Range", 4.0f, 1.0f..6.0f, 0.1f)
    private val shulkearBoxOnly by setting("Shulker Box Only", true)
    private val takeArmor by setting("Take Armor", true)
    private val clickDelayMs by setting("Click Delay ms", 10, 0..1000, 1)
    private val postDelayMs by setting("Post Delay ms", 50, 0..1000, 1)
    private val moveTimeoutMs by setting("Move Timeout ms", 100, 0..1000, 1)

    private val armorTimer = TickTimer()
    private val timeoutTimer = TickTimer()
    private var lastContainer: Container? = null
    private var lastTask: InventoryTask? = null
    private val moveTimeMap = Int2LongOpenHashMap().apply {
        defaultReturnValue(Long.MIN_VALUE)
    }
    private val explosionPosMap = Long2LongOpenHashMap().synchronized().apply {
        defaultReturnValue(Long.MIN_VALUE)
    }
    private val directions = EnumFacing.values()

    private var placeShulker = false

    override fun getHudInfo(): String {
        return Kit.kitName
    }

    init {
        onDisable {
            lastContainer = null
            lastTask?.cancel()
            lastTask = null
            moveTimeMap.clear()
        }

        onEnable {
            placeShulker = false
        }

        safeListener<CrystalSetDeadEvent> {
            explosionPosMap.put(VectorUtils.toLong(it.x, it.y, it.z), System.currentTimeMillis() + 3000L)
        }

        safeListener<RunGameLoopEvent.Tick> {
            if (!lastTask.executedOrTrue) return@safeListener

            val openContainer = player.openContainer
            if (openContainer === player.inventoryContainer
                && (!shulkearBoxOnly || openContainer !is ContainerShulkerBox)
            ) {
                lastTask?.cancel()
                return@safeListener
            }

            if (openContainer !== lastContainer) {
                moveTimeMap.clear()
                timeoutTimer.time = Long.MAX_VALUE
                lastContainer = openContainer
            } else if (timeoutTimer.tick(500)) {
                return@safeListener
            }

            val itemArray = Kit.getKitItemArray() ?: run {
                Notification.send(InventorySorter, "No kit named ${Kit.kitName} was not found!")
                return@safeListener
            }

            if (takeArmor(openContainer)) return@safeListener
            if (doRegear(openContainer, itemArray)) return@safeListener
        }

        safeParallelListener<TickEvent.Post> {
            val currentTime = System.currentTimeMillis()
            explosionPosMap.runSynchronized {
                values.removeIf {
                    it < currentTime
                }
            }

            if (!placeShulker) return@safeParallelListener
            placeShulker = false

            val shulkerSlot = player.allSlots.firstBlock<BlockShulkerBox>() ?: return@safeParallelListener

            ConcurrentScope.launch {
                val explosionPos = explosionPosMap.runSynchronized {
                    val iterator = keys.iterator()
                    LongArray(size) {
                        iterator.next()
                    }
                }

                val rangeSq = placeRange * placeRange
                val mutable = BlockPos.MutableBlockPos()

                VectorUtils.getBlockPosInSphere(player, placeRange)
                    .filterNot { world.getBlockState(it).isReplaceable }
                    .flatMap { pos ->
                        directions.asSequence().filter {
                            val directionVec = it.directionVec
                            player.distanceSqTo(pos.x + 0.5 + directionVec.x, pos.y + 0.5 + directionVec.y, pos.z + 0.5 + directionVec.z) < rangeSq
                        }.filter {
                            world.getBlockState(mutable.setAndAdd(pos, it)).isReplaceable
                                && world.checkNoEntityCollision(AxisAlignedBB(mutable), null)
                                && world.isAir(mutable.move(it))
                        }.map {
                            pos to it
                        }
                    }.maxWithOrNull(
                        compareBy<Pair<BlockPos, EnumFacing>> { (pos, direction) ->
                            val placedPos = mutable.setAndAdd(pos, direction)
                            explosionPos.none {
                                world.rayTraceVisible(
                                    placedPos.x + 0.5,
                                    placedPos.y + 0.5,
                                    placedPos.z + 0.5,
                                    BlockPosUtil.xFromLong(it) + 0.5,
                                    BlockPosUtil.yFromLong(it) + 0.5,
                                    BlockPosUtil.zFromLong(it) + 0.5,
                                    mutable
                                )
                            }
                        }.thenBy { (pos, _) ->
                            EntityManager.players.asSequence()
                                .filterNot { it.isSelf }
                                .filterNot { it.isFriend }
                                .maxOfOrNull { it.distanceSqTo(pos) } ?: 0.0
                        }.thenBy { (pos, _) ->
                            player.distanceSqTo(pos)
                        }
                    )?.let {
                        val placeInfo = newPlaceInfo(it.first, it.second)
                        println(placeInfo.pos)
                        println(placeInfo.direction)
                        println(placeInfo.placedPos)
                        if (Bypass.blockPlaceRotation) {
                            val rotationTo = getRotationTo(placeInfo.hitVec)
                            connection.sendPacket(CPacketPlayer.Rotation(rotationTo.x, rotationTo.y, player.onGround))
                        }
                        player.spoofSneak {
                            ghostSwitch(HotbarSwitchManager.Override.SWAP, shulkerSlot) {
                                placeBlock(placeInfo)
                            }
                        }
                        player.spoofUnSneak {
                            connection.sendPacket(CPacketPlayerTryUseItemOnBlock(
                                placeInfo.placedPos,
                                placeInfo.direction,
                                EnumHand.MAIN_HAND,
                                placeInfo.hitVecOffset.x,
                                placeInfo.hitVecOffset.y,
                                placeInfo.hitVecOffset.z
                            ))
                        }
                    }
            }
        }
    }

    private fun SafeClientEvent.takeArmor(
        openContainer: Container
    ): Boolean {
        if (!takeArmor) return false

        AutoArmor.enable()

        val windowID = openContainer.windowId
        val currentTime = System.currentTimeMillis()
        val containerSlots = openContainer.getContainerSlots().filter { currentTime > moveTimeMap[it.slotNumber] }
        val playerInventory = player.allSlots
        val tempHotbarSlot = player.hotbarSlots.firstEmpty()
            ?: player.hotbarSlots.find {
                val item = it.stack.item
                item !is ItemShulkerBox && item !is ItemArmor
            } ?: return false

        for (slotFrom in containerSlots) {
            val stack = slotFrom.stack
            val item = stack.item
            if (item !is ItemArmor) continue

            if (playerInventory.any {
                    val playetItem = it.stack.item
                    playetItem is ItemArmor && playetItem.armorType == item.armorType
                }) continue

            if (!armorTimer.tickAndReset(100L)) {
                timeoutTimer.time = Long.MAX_VALUE
                return true
            }

            lastTask = inventoryTask {
                swapWith(windowID, slotFrom, tempHotbarSlot)

                delay(clickDelayMs)
                postDelay(postDelayMs)
                runInGui()
            }

            moveTimeMap[slotFrom.slotNumber] = currentTime + moveTimeoutMs
            timeoutTimer.time = Long.MAX_VALUE

            return true
        }

        return false
    }

    private fun doRegear(
        openContainer: Container,
        itemArray: Array<Item>
    ): Boolean {
        val windowID = openContainer.windowId
        val currentTime = System.currentTimeMillis()
        val containerSlots = mutableListOf<Slot>()
        openContainer.getContainerSlots().filterTo(containerSlots) { currentTime > moveTimeMap.get(it.slotNumber) }

        val playerSlot = openContainer.getPlayerSlots()
        var hasEmptyBefore = false

        for (index in playerSlot.indices.reversed()) {
            val slotTo = playerSlot[index]
            val slotToStack = slotTo.stack
            if (slotToStack.isEmpty) {
                hasEmptyBefore = true
            }

            if (currentTime <= moveTimeMap.get(slotTo.slotNumber)) continue

            val targetItem = itemArray[index]
            if (targetItem is ItemShulkerBox) continue

            val isHotbar = index in playerSlot.size - 9 until playerSlot.size

            if (isHotbar && slotToStack.item is ItemArmor) continue

            val slotFrom = containerSlots.getMaxCompatibleStack(slotTo, targetItem) ?: continue

            lastTask = if (!hasEmptyBefore && slotToStack.isStackable(slotFrom.stack)) {
                inventoryTask {
                    quickMove(windowID, slotFrom)

                    delay(clickDelayMs)
                    postDelay(postDelayMs)
                    runInGui()
                }
            } else {
                inventoryTask {
                    pickUp(windowID, slotFrom)
                    pickUp(windowID, slotTo)
                    pickUp(windowID) { if (player.inventory.getCurrentItem().isEmpty) null else slotFrom }

                    delay(clickDelayMs)
                    postDelay(postDelayMs)
                    runInGui()
                }
            }

            moveTimeMap.put(slotTo.slotNumber, currentTime + moveTimeoutMs)
            moveTimeMap.put(slotFrom.slotNumber, currentTime + moveTimeoutMs)
            timeoutTimer.time = Long.MAX_VALUE
            containerSlots.remove(slotFrom)

            return true
        }

        if (timeoutTimer.time == Long.MAX_VALUE) {
            timeoutTimer.reset()
        }

        return false
    }
}