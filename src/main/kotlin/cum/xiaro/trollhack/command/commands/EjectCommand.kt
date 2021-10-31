package cum.xiaro.trollhack.command.commands

import cum.xiaro.trollhack.command.ClientCommand
import cum.xiaro.trollhack.module.modules.player.InventoryManager
import cum.xiaro.trollhack.util.text.MessageSendUtils

// TODO: Remove once GUI has List
object EjectCommand : ClientCommand(
    name = "eject",
    description = "Modify AutoEject item list"
) {
    init {
        literal("add", "+") {
            item("item") { itemArg ->
                execute("Add an item to the eject list") {
                    val itemName = itemArg.value.registryName!!.toString()

                    if (InventoryManager.ejectList.contains(itemName)) {
                        MessageSendUtils.sendNoSpamErrorMessage("§c$itemName is already added to eject list")
                    } else {
                        InventoryManager.ejectList.add(itemName)
                        MessageSendUtils.sendNoSpamChatMessage("$itemName has been added to the eject list")
                    }
                }
            }
        }

        literal("del", "remove", "-") {
            item("item") { itemArg ->
                execute("Remove an item from the eject list") {
                    val itemName = itemArg.value.registryName!!.toString()

                    if (!InventoryManager.ejectList.contains(itemName)) {
                        MessageSendUtils.sendNoSpamErrorMessage("§c$itemName is not in the eject list")
                    } else {
                        InventoryManager.ejectList.remove(itemName)
                        MessageSendUtils.sendNoSpamChatMessage("$itemName has been removed from the eject list")
                    }
                }
            }
        }

        literal("list") {
            execute("List items in the eject list") {
                var list = InventoryManager.ejectList.joinToString()
                if (list.isEmpty()) list = "§cNo items!"
                MessageSendUtils.sendNoSpamChatMessage("AutoEject item list:\n$list")
            }
        }

        literal("reset", "default") {
            execute("Reset the eject list to defaults") {
                InventoryManager.ejectList.resetValue()
                MessageSendUtils.sendNoSpamChatMessage("Reset eject list to defaults")
            }
        }

        literal("clear") {
            execute("Set the eject list to nothing") {
                InventoryManager.ejectList.clear()
                MessageSendUtils.sendNoSpamChatMessage("Reset eject list was cleared")
            }
        }
    }
}