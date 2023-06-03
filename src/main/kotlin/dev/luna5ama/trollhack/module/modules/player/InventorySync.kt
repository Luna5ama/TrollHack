package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.TickTimer
import net.minecraft.init.Blocks
import net.minecraft.inventory.ClickType
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketClickWindow

internal object InventorySync : Module(
    name = "Inventory Sync",
    category = Category.PLAYER,
    description = "Fix inventory desyncs",
    modulePriority = 50
) {
    private val startDelay by setting("Start Delay", 200, 50..1000, 50, fineStep = 1)
    private val endTimeout by setting("End Timeout", 300, 50..1000, 50, fineStep = 1)
    private val interval by setting("Interval", 1500, 50..3000, 50)

    private val firstPacketTimer = TickTimer()
    private val packetTimer = TickTimer()
    private val sendTimer = TickTimer()
    private var sent = false

    private val illegalPacket = CPacketClickWindow(
        0,
        0,
        0,
        ClickType.PICKUP,
        ItemStack(Item.getItemFromBlock(Blocks.BEDROCK)),
        0
    )

    init {
        onEnable {
            firstPacketTimer.time = 0L
            packetTimer.time = 0L
            sendTimer.time = 0L
            sent = true
        }

        safeListener<PacketEvent.PostSend> {
            if (it.packet is CPacketClickWindow && it.packet !== illegalPacket) {
                if (sent && packetTimer.tick(endTimeout)) firstPacketTimer.reset()
                packetTimer.reset()
                sent = false
            }
        }

        safeParallelListener<TickEvent.Post> {
            if (!firstPacketTimer.tick(startDelay)) return@safeParallelListener
            if (sent && packetTimer.tick(endTimeout)) return@safeParallelListener
            if (!sendTimer.tickAndReset(interval)) return@safeParallelListener

            connection.sendPacket(illegalPacket)
            sent = true
            println("Syncing")
        }
    }
}