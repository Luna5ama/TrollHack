package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.accessor.packetSlot
import dev.luna5ama.trollhack.util.accessor.packetWindowID
import dev.luna5ama.trollhack.util.threads.runSafe
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Container
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketClickWindow
import net.minecraft.network.play.client.CPacketConfirmTransaction
import net.minecraft.network.play.server.SPacketConfirmTransaction
import net.minecraft.network.play.server.SPacketSetSlot

internal object InventorySync : Module(
    name = "Inventory Sync",
    category = Category.PLAYER,
    description = "Fix inventory desyncs",
    modulePriority = 50
) {
    private val forceConfirmTransaction by setting("Force Confirm Transaction", true)
    private val minDelayMs by setting("Min Delay ms", 200, 0..5000, 10)
    private val maxDelayMs by setting("Max Delay ms", 500, 0..5000, 10)
    private val serverResponseTimeout by setting("Server Response Timeout ms", 1000, 0..5000, 10)

    private val slotTime = LongArray(46 * 2) { Long.MAX_VALUE }
    private val idSlotMap = Short2ObjectOpenHashMap<Tracker>()
    private val trackerQueue = ObjectArrayFIFOQueue<Tracker>()

    init {
        onDisable {
            slotTime.fill(Long.MAX_VALUE)
            idSlotMap.clear()
        }

        safeListener<PacketEvent.Receive> {
            val container = player.inventoryContainer ?: return@safeListener
            val playerInventory = player.inventory ?: return@safeListener

            when (it.packet) {
                is SPacketConfirmTransaction -> {
                    if (forceConfirmTransaction) {
                        it.cancel()
                        connection.sendPacket(
                            CPacketConfirmTransaction(
                                it.packet.windowId,
                                it.packet.actionNumber,
                                true
                            )
                        )
                    }

                    if (it.packet.windowId != 0) return@safeListener
                    if (it.packet.wasAccepted()) return@safeListener

                    synchronized(idSlotMap) {
                        val tracker = idSlotMap.remove(it.packet.actionNumber) ?: return@safeListener
                        tracker.time = System.currentTimeMillis()
                        trackerQueue.enqueue(tracker)
                    }

                    if (!forceConfirmTransaction) {
                        it.cancel()
                        connection.sendPacket(CPacketConfirmTransaction(0, it.packet.actionNumber, true))
                    }
                }
                is SPacketSetSlot -> {
                    synchronized(idSlotMap) {
                        var tracker: Tracker? = null
                        while (!trackerQueue.isEmpty) {
                            tracker = trackerQueue.first()
                            if (System.currentTimeMillis() - tracker.time > 1L) {
                                trackerQueue.dequeue()
                                continue
                            }
                            break
                        }

                        if (tracker == null) return@safeListener

                        if (!it.packet.stack.isEmpty) {
                            if (it.packet.windowId != -1) return@safeListener

                            it.packet.packetWindowID = 0
                            it.packet.packetSlot = tracker.slot

                            val transactionID = container.getNextTransactionID(playerInventory)

                            connection.sendPacket(
                                CPacketClickWindow(
                                    0,
                                    tracker.slot,
                                    0,
                                    ClickType.PICKUP,
                                    ItemStack.EMPTY,
                                    transactionID
                                )
                            )
                            return@safeListener
                        }

                        if (it.packet.windowId != 0) return@safeListener
                        if (it.packet.slot != tracker.slot) return@safeListener

                        it.cancel()
                    }
                }
            }
        }

        safeParallelListener<TickEvent.Post> {
            synchronized(idSlotMap) {
                val timeout = System.currentTimeMillis() - serverResponseTimeout
                idSlotMap.values.removeIf { it.time < timeout }
            }
        }

        safeListener<TickEvent.Post> {
            if (!player.inventory.itemStack.isEmpty) return@safeListener
            val current = System.currentTimeMillis()
            val container = player.inventoryContainer ?: return@safeListener
            val playerInventory = player.inventory ?: return@safeListener

            for (i in 0 until 46) {
                if (slotTime[i] == Long.MAX_VALUE) continue

                if (slotTime[i] + maxDelayMs > current &&
                    slotTime[i + 46] + minDelayMs > current
                ) continue

                slotTime[i] = Long.MAX_VALUE
                slotTime[i + 46] = Long.MAX_VALUE

                val transactionID = container.getNextTransactionID(playerInventory)
                idSlotMap.put(transactionID, Tracker(i))

                connection.sendPacket(
                    CPacketClickWindow(
                        0,
                        i,
                        0,
                        ClickType.PICKUP,
                        ItemStack.EMPTY,
                        transactionID
                    )
                )
                break
            }
        }
    }

    @JvmStatic
    fun handleSlotClick(
        container: Container,
        slot: Int,
        button: Int,
        clickType: ClickType
    ) {
        if (isDisabled) return
        if (slot !in 0 until 46) return
        if (clickType != ClickType.PICKUP
            && clickType != ClickType.QUICK_MOVE
            && clickType != ClickType.SWAP
        ) return

        runSafe {
            if (container != player.inventoryContainer) return@runSafe

            if (clickType == ClickType.SWAP && button in 0..8) {
                handleSlotClick(container, button + 36, 0, ClickType.PICKUP)
            }

            if (slotTime[slot] == Long.MAX_VALUE) {
                slotTime[slot] = System.currentTimeMillis()
            }
            slotTime[slot + 46] = System.currentTimeMillis()
        }
    }

    private class Tracker(val slot: Int) {
        var time = System.currentTimeMillis()
    }
}