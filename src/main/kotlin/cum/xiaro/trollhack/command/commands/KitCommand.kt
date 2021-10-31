package cum.xiaro.trollhack.command.commands

import cum.xiaro.trollhack.command.ClientCommand
import cum.xiaro.trollhack.module.modules.player.Kit
import cum.xiaro.trollhack.util.inventory.slot.inventorySlots
import cum.xiaro.trollhack.util.text.NoSpamMessage
import cum.xiaro.trollhack.util.text.formatValue

object KitCommand : ClientCommand(
    name = "kit",
    description = "Kit management"
) {
    init {
        literal("create", "add", "new", "+") {
            string("name") { nameArg ->
                executeSafe {
                    val slots = player.inventorySlots
                    val array = List(36) {
                        slots[it].stack.item.registryName?.toString() ?: "minecraft:air"
                    }

                    val name = nameArg.value
                    if (Kit.kitMap.value.put(name, array) == null) {
                        NoSpamMessage.sendMessage(KitCommand, "Created kit ${formatValue(name)}!")
                    } else {
                        NoSpamMessage.sendWarning(KitCommand, "Override kit ${formatValue(name)}!")
                    }
                }
            }
        }

        literal("delete", "del", "remove", "-") {
            string("name") { nameArg ->
                execute {
                    val name = nameArg.value
                    if (Kit.kitMap.value.remove(name) != null) {
                        NoSpamMessage.sendMessage(KitCommand, "Deleted kit ${formatValue(name)}!")
                    } else {
                        NoSpamMessage.sendWarning(KitCommand, "Kit ${formatValue(name)} not found!")
                    }
                }
            }
        }

        literal("set", "=") {
            string("name") { nameArg ->
                execute {
                    val name = nameArg.value
                    if (Kit.kitMap.value.containsKey(name)) {
                        Kit.kitName = name
                        NoSpamMessage.sendMessage(KitCommand, "Set kit to ${formatValue(name)}!")
                    } else {
                        NoSpamMessage.sendWarning(KitCommand, "Kit ${formatValue(name)} not found!")
                    }
                }
            }
        }

        literal("list") {
            execute {
                NoSpamMessage.sendMessage(KitCommand, "List of kits:\n${Kit.kitMap.value.keys.joinToString()}")
            }
        }
    }
}