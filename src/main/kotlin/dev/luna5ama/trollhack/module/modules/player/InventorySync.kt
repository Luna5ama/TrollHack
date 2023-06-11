package dev.luna5ama.trollhack.module.modules.player

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.mixins.accessor.network.AccessorCPacketClickWindow
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.extension.synchronized
import it.unimi.dsi.fastutil.shorts.ShortLinkedOpenHashSet
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.ContainerPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketClickWindow
import net.minecraft.network.play.client.CPacketConfirmTransaction
import net.minecraft.network.play.server.SPacketConfirmTransaction
import kotlin.math.abs
import kotlin.random.Random

internal object InventorySync : Module(
    name = "Inventory Sync",
    category = Category.PLAYER,
    description = "Fix inventory desyncs",
    modulePriority = 50
) {
    private val startDelay by setting("Start Delay", 200, 50..1000, 50, fineStep = 1)
    private val endTimeout by setting("End Timeout", 300, 50..1000, 50, fineStep = 1)
    private val interval by setting("Interval", 1500, 50..3000, 50)
    private val forceConfirm by setting("Force Confirm", true)
    private val cancelExtraConfirm by setting("Cancel Extra Confirm", false)

    private val firstPacketTimer = TickTimer()
    private val packetTimer = TickTimer()
    private val sendTimer = TickTimer()
    private var sent = false

    private val craftingItems = Array(5) { Items.AIR }

    private val illegalStack = ItemStack(Item.getItemFromBlock(Blocks.BARRIER))
    private val randomActionID = ShortLinkedOpenHashSet().synchronized()

    init {
        onEnable {
            firstPacketTimer.time = 0L
            packetTimer.time = 0L
            sendTimer.time = 0L
            sent = true
        }

        onDisable {
            randomActionID.clear()
            craftingItems.fill(Items.AIR)
        }

        safeListener<PacketEvent.PostSend> {
            if (it.packet !is CPacketClickWindow) return@safeListener
            if (it.packet.clickedItem === illegalStack) return@safeListener

            if (sent && packetTimer.tick(endTimeout)) firstPacketTimer.reset()
            packetTimer.reset()
            sent = false
        }

        safeListener<PacketEvent.Receive> {
            if (!forceConfirm || !cancelExtraConfirm) return@safeListener
            if (it.packet !is SPacketConfirmTransaction) return@safeListener
            if (it.packet.windowId != 0) return@safeListener
            if (it.packet.wasAccepted()) return@safeListener

            if (randomActionID.remove(it.packet.actionNumber)) {
                it.cancel()
            }
        }

        safeParallelListener<TickEvent.Post> {
            for (i in 1 until craftingItems.size) {
                val prev = craftingItems[i]
                val curr = player.inventoryContainer.getSlot(i).stack.item
                if (prev != curr) {
                    craftingItems[0] = Items.AIR
                }
                craftingItems[i] = curr
            }
            if (mc.currentScreen is GuiContainer && player.openContainer is ContainerPlayer) {
                val craftOutput = player.openContainer.getSlot(0).stack.item
                if (craftOutput != Items.AIR) {
                    craftingItems[0] = craftOutput
                }
            }

            if (craftingItems[0] != Items.AIR) return@safeParallelListener

            if (!firstPacketTimer.tick(startDelay)) {
                if (forceConfirm && cancelExtraConfirm) randomActionID.clear()
                return@safeParallelListener
            }

            if (sent && packetTimer.tick(endTimeout)) return@safeParallelListener
            if (!sendTimer.tickAndReset(interval)) return@safeParallelListener

            var random = Random.nextInt().toShort()
            val potentialNumber = player.inventoryContainer.getNextTransactionID(player.inventory)

            if (abs(random - potentialNumber) < 1000) {
                random = if (potentialNumber > random) {
                    (potentialNumber + 1337).toShort()
                } else {
                    (potentialNumber - 1337).toShort()
                }
            }

            val packet = CPacketClickWindow(
                0,
                0,
                0,
                ClickType.PICKUP,
                ItemStack.EMPTY,
                random
            )
            @Suppress("KotlinConstantConditions")
            (packet as AccessorCPacketClickWindow).trollSetClickedItem(illegalStack)

            connection.sendPacket(
                packet
            )
            sent = true

            if (forceConfirm) {
                connection.sendPacket(CPacketConfirmTransaction(0, random, true))
                if (cancelExtraConfirm) randomActionID.add(random)
            }
        }
    }
}