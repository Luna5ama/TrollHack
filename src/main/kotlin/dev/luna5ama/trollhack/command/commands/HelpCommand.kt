package dev.luna5ama.trollhack.command.commands

import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.command.CommandManager
import dev.luna5ama.trollhack.util.text.MessageSendUtils
import dev.luna5ama.trollhack.util.text.formatValue
import net.minecraft.util.text.TextFormatting

object HelpCommand : ClientCommand(
    name = "help",
    description = "Help for commands"
) {
    init {
        string("command") { commandArg ->
            execute("List help for a command") {
                val cmd = CommandManager.getCommandOrNull(commandArg.value) ?: run {
                    MessageSendUtils.sendErrorMessage("Could not find command ${formatValue(commandArg.value)}!")
                    return@execute
                }

                MessageSendUtils.sendChatMessage(
                    "Help for command ${formatValue("$prefix${cmd.name}")}\n"
                        + cmd.printArgHelp()
                )
            }
        }
        execute("List available commands") {
            val commands = CommandManager
                .getCommands()
                .sortedBy { it.nameAsString }

            MessageSendUtils.sendChatMessage("Available commands: ${formatValue(commands.size)}")
            commands.forEach {
                MessageSendUtils.sendRawMessage("    $prefix${it.name}\n        ${TextFormatting.GRAY}${it.description}")
            }
        }
    }
}