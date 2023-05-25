package dev.luna5ama.trollhack.command.commands

import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.module.modules.render.Search
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.text.formatValue

// TODO: Remove once GUI has List
object SearchCommand : ClientCommand(
    name = "search",
    description = "Manage search blocks"
) {
    private val warningBlocks = hashSetOf(
        "minecraft:grass",
        "minecraft:end_stone",
        "minecraft:lava",
        "minecraft:bedrock",
        "minecraft:netherrack",
        "minecraft:dirt",
        "minecraft:water",
        "minecraft:stone"
    )

    init {
        literal("add", "+") {
            block("block") { blockArg ->
                literal("force") {
                    execute("Force add a block to search list") {
                        val blockName = blockArg.value.registryName.toString()
                        addBlock(blockName)
                    }
                }

                execute("Add a block to search list") {
                    val blockName = blockArg.value.registryName.toString()

                    if (warningBlocks.contains(blockName)) {
                        NoSpamMessage.sendWarning(
                            "Your world contains lots of ${formatValue(blockName)}, " +
                                "it might cause extreme lag to add it. " +
                                "If you are sure you want to add it run ${formatValue("$prefixName add force $blockName")}"
                        )
                    } else {
                        addBlock(blockName)
                    }
                }
            }
        }

        literal("remove", "del", "delete", "-") {
            block("block") { blockArg ->
                execute("Remove a block from search list") {
                    val blockName = blockArg.value.registryName.toString()

                    Search.searchList.editValue {
                        if (!it.remove(blockName)) {
                            NoSpamMessage.sendError("You do not have ${formatValue(blockName)} added to search block list")
                        } else {
                            NoSpamMessage.sendMessage("Removed ${formatValue(blockName)} from search block list")
                        }
                    }
                }
            }
        }

        literal("set", "=") {
            block("block") { blockArg ->
                execute("Set the search list to one block") {
                    val blockName = blockArg.value.registryName.toString()

                    Search.searchList.editValue {
                        it.clear()
                        it.add(blockName)
                    }
                    NoSpamMessage.sendMessage("Set the search block list to ${formatValue(blockName)}")
                }
            }
        }

        literal("reset", "default") {
            execute("Reset the search list to defaults") {
                Search.searchList.resetValue()
                NoSpamMessage.sendMessage("Reset the search block list to defaults")
            }
        }

        literal("list") {
            execute("Print search list") {
                NoSpamMessage.sendMessage(Search.searchList.joinToString())
            }
        }

        literal("clear") {
            execute("Clear the search list") {
                Search.searchList.editValue {
                    it.clear()
                }
                NoSpamMessage.sendMessage("Cleared the search block list")
            }
        }
    }

    private fun addBlock(blockName: String) {
        if (blockName == "minecraft:air") {
            NoSpamMessage.sendMessage("You can't add ${formatValue(blockName)} to the search block list")
            return
        }

        Search.searchList.editValue {
            if (!it.add(blockName)) {
                NoSpamMessage.sendError("${formatValue(blockName)} is already added to the search block list")
            } else {
                NoSpamMessage.sendMessage("${formatValue(blockName)} has been added to the search block list")
            }
        }
    }
}