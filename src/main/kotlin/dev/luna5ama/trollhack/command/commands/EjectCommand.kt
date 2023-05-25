package dev.luna5ama.trollhack.command.commands

import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.module.modules.player.AutoEject
import dev.luna5ama.trollhack.util.text.NoSpamMessage

// TODO: Remove once GUI has List
object EjectCommand : ClientCommand(
    name = "eject",
    description = "Modify AutoEject item list"
) {
    init {
        literal("set") {
            item("item") { itemArg ->
                int("stackSize") { stackSizeArg ->
                    execute("Set an item to be ejected at given stack size") {
                        val itemName = itemArg.value.registryName!!.toString()
                        val stackSize = stackSizeArg.value

                        if (stackSize <= 0) {
                            NoSpamMessage.sendError("Stack size must be greater than 0!")
                            return@execute
                        }

                        AutoEject.ejectMap.value.put(itemName, stackSize)
                        NoSpamMessage.sendMessage("$itemName has been set to eject at $stackSize stack.")
                    }
                }
                execute("Set an item to be ejected at 0 stack") {
                    val itemName = itemArg.value.registryName!!.toString()

                    AutoEject.ejectMap.value.put(itemName, 0)
                    NoSpamMessage.sendMessage("$itemName has been set to eject at 0 stack.")
                }
            }
        }

        literal("del", "remove", "-") {
            item("item") { itemArg ->
                execute("Remove an item from the eject list") {
                    val itemName = itemArg.value.registryName!!.toString()

                    if (!AutoEject.ejectMap.value.containsKey(itemName)) {
                        NoSpamMessage.sendError("§c$itemName is not in the eject list")
                    } else {
                        AutoEject.ejectMap.value.removeInt(itemName)
                        NoSpamMessage.sendMessage("$itemName has been removed from the eject list")
                    }
                }
            }
        }

        literal("list") {
            execute("List items in the eject list") {
                var list = AutoEject.ejectMap.value.entries.joinToString()
                if (list.isEmpty()) list = "§cNo items!"
                NoSpamMessage.sendMessage("AutoEject item list:\n$list")
            }
        }

        literal("reset", "default") {
            execute("Reset the eject list to defaults") {
                AutoEject.ejectMap.resetValue()
                NoSpamMessage.sendMessage("Reset eject list to defaults")
            }
        }

        literal("clear") {
            execute("Set the eject list to nothing") {
                AutoEject.ejectMap.value.clear()
                NoSpamMessage.sendMessage("Reset eject list was cleared")
            }
        }
    }
}