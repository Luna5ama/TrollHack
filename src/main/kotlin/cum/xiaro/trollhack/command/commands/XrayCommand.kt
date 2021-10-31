package cum.xiaro.trollhack.command.commands

import cum.xiaro.trollhack.command.ClientCommand
import cum.xiaro.trollhack.module.modules.render.Xray
import cum.xiaro.trollhack.util.text.NoSpamMessage
import cum.xiaro.trollhack.util.text.formatValue

object XrayCommand : ClientCommand(
    name = "Xray",
    description = "Manage visible xray blocks"
) {

    init {
        literal("add", "+") {
            block("block") { blockArg ->
                execute("Add a block to block list") {
                    val blockName = blockArg.value.registryName.toString()

                    if (Xray.blockList.contains(blockName)) {
                        NoSpamMessage.sendError(XrayCommand, "${formatValue(blockName)} is already added to the visible block list")
                    } else {
                        Xray.blockList.editValue { it.add(blockName) }
                        NoSpamMessage.sendMessage(XrayCommand, "${formatValue(blockName)} has been added to the visible block list")
                    }
                }
            }
        }

        literal("remove", "-") {
            block("block") { blockArg ->
                execute("Remove a block from block list") {
                    val blockName = blockArg.value.registryName.toString()

                    if (!Xray.blockList.contains(blockName)) {
                        NoSpamMessage.sendError(XrayCommand, "You do not have ${formatValue(blockName)} added to xray visible block list")
                    } else {
                        Xray.blockList.editValue { it.remove(blockName) }
                        NoSpamMessage.sendError(XrayCommand, "Removed ${formatValue(blockName)} from xray visible block list")
                    }
                }
            }
        }

        literal("set", "=") {
            block("block") { blockArg ->
                execute("Set the block list to one block") {
                    val blockName = blockArg.value.registryName.toString()

                    Xray.blockList.editValue {
                        it.clear()
                        it.add(blockName)
                    }
                    NoSpamMessage.sendMessage(XrayCommand, "Set the xray block list to ${formatValue(blockName)}")
                }
            }
        }

        literal("reset", "default") {
            execute("Reset the block list to defaults") {
                Xray.blockList.editValue { it.resetValue() }
                NoSpamMessage.sendMessage(XrayCommand, "Reset the visible block list to defaults")
            }
        }

        literal("list") {
            execute("Print block list") {
                NoSpamMessage.sendMessage(XrayCommand, Xray.blockList.joinToString())
            }
        }

        literal("clear") {
            execute("Clear the block list") {
                Xray.blockList.editValue { it.clear() }
                NoSpamMessage.sendMessage(XrayCommand, "Cleared the visible block list")
            }
        }
    }
}