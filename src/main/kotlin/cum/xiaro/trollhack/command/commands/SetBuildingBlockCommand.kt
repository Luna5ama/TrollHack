package cum.xiaro.trollhack.command.commands

import cum.xiaro.trollhack.command.ClientCommand
import cum.xiaro.trollhack.module.modules.player.InventoryManager
import cum.xiaro.trollhack.util.items.block
import cum.xiaro.trollhack.util.items.id
import cum.xiaro.trollhack.util.text.MessageSendUtils
import net.minecraft.block.BlockAir

// TODO: Remove once GUI has Block settings
object SetBuildingBlockCommand : ClientCommand(
    name = "setbuildingblock",
    description = "Set the default building block"
) {
    init {
        executeSafe {
            val heldItem = player.inventory.getCurrentItem()
            when {
                heldItem.isEmpty -> {
                    InventoryManager.buildingBlockID = 0
                    MessageSendUtils.sendNoSpamChatMessage("Building block has been reset")
                }
                heldItem.item.block !is BlockAir -> {
                    InventoryManager.buildingBlockID = heldItem.item.id
                    MessageSendUtils.sendNoSpamChatMessage("Building block has been set to ${heldItem.displayName}")
                }
                else -> {
                    MessageSendUtils.sendNoSpamChatMessage("You are not holding a valid block")
                }
            }
        }
    }
}