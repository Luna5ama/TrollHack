package cum.xiaro.trollhack.command.commands

import cum.xiaro.trollhack.command.ClientCommand
import cum.xiaro.trollhack.module.modules.render.Search
import cum.xiaro.trollhack.util.text.MessageSendUtils
import cum.xiaro.trollhack.util.text.formatValue

// TODO: Remove once GUI has List
object SearchCommand : ClientCommand(
    name = "search",
    description = "Manage search blocks"
) {
    private val warningBlocks = hashSetOf("minecraft:grass", "minecraft:end_stone", "minecraft:lava", "minecraft:bedrock", "minecraft:netherrack", "minecraft:dirt", "minecraft:water", "minecraft:stone")

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
                        MessageSendUtils.sendNoSpamWarningMessage("Your world contains lots of ${formatValue(blockName)}, " +
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
                            MessageSendUtils.sendNoSpamErrorMessage("You do not have ${formatValue(blockName)} added to search block list")
                        } else {
                            MessageSendUtils.sendNoSpamChatMessage("Removed ${formatValue(blockName)} from search block list")
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
                    MessageSendUtils.sendNoSpamChatMessage("Set the search block list to ${formatValue(blockName)}")
                }
            }
        }

        literal("reset", "default") {
            execute("Reset the search list to defaults") {
                Search.searchList.resetValue()
                MessageSendUtils.sendNoSpamChatMessage("Reset the search block list to defaults")
            }
        }

        literal("list") {
            execute("Print search list") {
                MessageSendUtils.sendNoSpamChatMessage(Search.searchList.joinToString())
            }
        }

        literal("clear") {
            execute("Clear the search list") {
                Search.searchList.editValue {
                    it.clear()
                }
                MessageSendUtils.sendNoSpamChatMessage("Cleared the search block list")
            }
        }
    }

    private fun addBlock(blockName: String) {
        if (blockName == "minecraft:air") {
            MessageSendUtils.sendNoSpamChatMessage("You can't add ${formatValue(blockName)} to the search block list")
            return
        }

        Search.searchList.editValue {
            if (!it.add(blockName)) {
                MessageSendUtils.sendNoSpamErrorMessage("${formatValue(blockName)} is already added to the search block list")
            } else {
                MessageSendUtils.sendNoSpamChatMessage("${formatValue(blockName)} has been added to the search block list")
            }
        }
    }
}