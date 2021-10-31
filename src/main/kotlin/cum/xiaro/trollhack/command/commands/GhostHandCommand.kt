package cum.xiaro.trollhack.command.commands

import cum.xiaro.trollhack.command.ClientCommand
import cum.xiaro.trollhack.module.modules.player.GhostHand
import cum.xiaro.trollhack.util.text.NoSpamMessage
import cum.xiaro.trollhack.util.text.formatValue

object GhostHandCommand : ClientCommand(
    name = "GhostHand",
    description = "Manage GhostHand block list"
) {
    init {
        literal("add", "+") {
            block("block") { blockArg ->
                execute("Add a block to block list") {
                    val blockName = blockArg.value.registryName.toString()

                    if (GhostHand.blockList.contains(blockName)) {
                        NoSpamMessage.sendError(GhostHandCommand, "${formatValue(blockName)} is already added to the visible block list")
                    } else {
                        GhostHand.blockList.editValue { it.add(blockName) }
                        NoSpamMessage.sendMessage(GhostHandCommand, "${formatValue(blockName)} has been added to the visible block list")
                    }
                }
            }
        }

        literal("remove", "-") {
            block("block") { blockArg ->
                execute("Remove a block from block list") {
                    val blockName = blockArg.value.registryName.toString()

                    if (!GhostHand.blockList.contains(blockName)) {
                        NoSpamMessage.sendError(GhostHandCommand, "You do not have ${formatValue(blockName)} added to xray visible block list")
                    } else {
                        GhostHand.blockList.editValue { it.remove(blockName) }
                        NoSpamMessage.sendMessage(GhostHandCommand, "Removed ${formatValue(blockName)} from xray visible block list")
                    }
                }
            }
        }

        literal("set", "=") {
            block("block") { blockArg ->
                execute("Set the block list to one block") {
                    val blockName = blockArg.value.registryName.toString()

                    GhostHand.blockList.editValue {
                        it.clear()
                        it.add(blockName)
                    }
                    NoSpamMessage.sendMessage(GhostHandCommand, "Set the xray block list to ${formatValue(blockName)}")
                }
            }
        }

        literal("reset", "default") {
            execute("Reset the GhostHand block list to defaults") {
                GhostHand.blockList.editValue { it.resetValue() }
                NoSpamMessage.sendMessage(GhostHandCommand, "Reset the visible block list to defaults")
            }
        }

        literal("list") {
            execute("Print block list") {
                NoSpamMessage.sendMessage(GhostHandCommand, GhostHand.blockList.joinToString())
            }
        }

        literal("clear") {
            execute("Clear the block list") {
                GhostHand.blockList.editValue { it.clear() }
                NoSpamMessage.sendMessage(GhostHandCommand, "Cleared the visible block list")
            }
        }
    }
}