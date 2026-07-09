package dev.luna5ama.trollhack.command.comands

import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.modules.impl.player.Search
import dev.luna5ama.trollhack.utils.ChatUtils
import dev.luna5ama.trollhack.utils.formatValue
import net.minecraft.core.registries.BuiltInRegistries

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
                        val blockName = BuiltInRegistries.BLOCK.getKey(blockArg.value).toString()
                        addBlock(blockName)
                    }
                }

                execute("Add a block to search list") {
                    val blockName = BuiltInRegistries.BLOCK.getKey(blockArg.value).toString()

                    if (warningBlocks.contains(blockName)) {
                        ChatUtils.sendMessage(
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
                    val blockName = BuiltInRegistries.BLOCK.getKey(blockArg.value).toString()

                    Search.searchList.editValue {
                        if (!it.remove(blockName)) {
                            ChatUtils.sendMessage("You do not have ${formatValue(blockName)} added to search block list")
                        } else {
                            ChatUtils.sendMessage("Removed ${formatValue(blockName)} from search block list")
                        }
                    }
                }
            }
        }

        literal("set", "=") {
            block("block") { blockArg ->
                execute("Set the search list to one block") {
                    val blockName = BuiltInRegistries.BLOCK.getKey(blockArg.value).toString()

                    Search.searchList.editValue {
                        it.clear()
                        it.add(blockName)
                    }
                    ChatUtils.sendMessage("Set the search block list to ${formatValue(blockName)}")
                }
            }
        }

        literal("reset", "default") {
            execute("Reset the search list to defaults") {
                Search.searchList.setToDefault()
                ChatUtils.sendMessage("Reset the search block list to defaults")
            }
        }

        literal("list") {
            execute("Print search list") {
                ChatUtils.sendMessage(Search.searchList.value.joinToString())
            }
        }

        literal("clear") {
            execute("Clear the search list") {
                Search.searchList.editValue {
                    it.clear()
                }
                ChatUtils.sendMessage("Cleared the search block list")
            }
        }
    }

    private fun addBlock(blockName: String) {
        if (blockName == "minecraft:air") {
            ChatUtils.sendMessage("You can't add ${formatValue(blockName)} to the search block list")
            return
        }

        Search.searchList.editValue {
            if (!it.add(blockName)) {
                ChatUtils.sendMessage("${formatValue(blockName)} is already added to the search block list")
            } else {
                ChatUtils.sendMessage("${formatValue(blockName)} has been added to the search block list")
            }
        }
    }
}