package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.accessor.windowID
import cum.xiaro.trollhack.util.inventory.slot.craftingSlots
import cum.xiaro.trollhack.util.inventory.slot.hasAnyItem
import net.minecraft.network.play.client.CPacketCloseWindow

internal object XCarry : Module(
    name = "XCarry",
    category = Category.PLAYER,
    description = "Store items in crafting slots"
) {
    init {
        safeListener<PacketEvent.Send> {
            if (it.packet is CPacketCloseWindow && it.packet.windowID == 0 && (!player.inventory.itemStack.isEmpty || player.craftingSlots.hasAnyItem())) {
                it.cancel()
            }
        }
    }
}