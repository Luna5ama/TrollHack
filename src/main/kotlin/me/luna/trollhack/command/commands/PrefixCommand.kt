package me.luna.trollhack.command.commands

import me.luna.trollhack.command.ClientCommand
import me.luna.trollhack.module.modules.client.CommandSetting
import me.luna.trollhack.util.text.NoSpamMessage
import me.luna.trollhack.util.text.formatValue

object PrefixCommand : ClientCommand(
    name = "prefix",
    description = "Change command prefix"
) {
    init {
        literal("reset") {
            execute("Reset the prefix to ;") {
                CommandSetting.prefix = ";"
                NoSpamMessage.sendMessage(PrefixCommand, "Reset prefix to [${formatValue(';')}]!")
            }
        }

        string("new prefix") { prefixArg ->
            execute("Set a new prefix") {
                if (prefixArg.value.isEmpty() || prefixArg.value == "\\") {
                    CommandSetting.prefix = ";"
                    NoSpamMessage.sendMessage(PrefixCommand, "Reset prefix to [${formatValue(';')}]!")
                    return@execute
                }

                CommandSetting.prefix = prefixArg.value
                NoSpamMessage.sendMessage(PrefixCommand, "Set prefix to ${formatValue(prefixArg.value)}!")
            }
        }
    }
}