package cum.xiaro.trollhack.util.inventory

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.util.inventory.operation.swapToSlot
import cum.xiaro.trollhack.util.inventory.slot.*
import cum.xiaro.trollhack.util.items.isTool
import cum.xiaro.trollhack.util.threads.onMainThreadSafe
import net.minecraft.block.state.IBlockState
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.init.Enchantments
import net.minecraft.init.Items
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.network.play.client.CPacketClickWindow

fun SafeClientEvent.equipBestTool(blockState: IBlockState) {
    findBestTool(blockState)?.let {
        swapToSlot(it)
    }
}

fun SafeClientEvent.findBestTool(blockState: IBlockState): HotbarSlot? {
    var maxSpeed = 1.0f
    var bestSlot: HotbarSlot? = null

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
                bestSlot = slot
            }
        }
    }

    return bestSlot
}

/**
 * Put the item currently holding by mouse to somewhere or throw it
 */
fun SafeClientEvent.removeHoldingItem() {
    if (player.inventory.itemStack.isEmpty) return

    val slot = player.inventoryContainer.getSlots(9..45).firstItem(Items.AIR)?.slotNumber // Get empty slots in inventory and offhand
        ?: player.craftingSlots.firstItem(Items.AIR)?.slotNumber // Get empty slots in crafting slot
        ?: -999 // Throw on the ground

    clickSlot(0, slot, 0, ClickType.PICKUP)
}

/**
 * Performs inventory clicking in specific window, slot, mouseButton, and click type
 *
 * @return Transaction id
 */
fun SafeClientEvent.clickSlot(windowID: Int, slot: Slot, mouseButton: Int, type: ClickType): Short {
    return clickSlot(windowID, slot.slotNumber, mouseButton, type)
}

/**
 * Performs inventory clicking in specific window, slot, mouseButton, and click type
 *
 * @return Transaction id
 */
fun SafeClientEvent.clickSlot(windowID: Int, slot: Int, mouseButton: Int, type: ClickType): Short {
    val container = getContainerForID(windowID) ?: return -32768

    val playerInventory = player.inventory ?: return -32768
    val transactionID = container.getNextTransactionID(playerInventory)
    val itemStack = container.slotClick(slot, mouseButton, type, player)

    connection.sendPacket(CPacketClickWindow(windowID, slot, mouseButton, type, itemStack, transactionID))
    onMainThreadSafe { playerController.updateController() }

    return transactionID
}

private fun SafeClientEvent.getContainerForID(windowID: Int): Container? =
    if (windowID == 0) player.inventoryContainer
    else player.openContainer