package cum.xiaro.trollhack.command.commands

import cum.xiaro.trollhack.command.ClientCommand
import cum.xiaro.trollhack.manager.managers.MacroManager
import cum.xiaro.trollhack.util.KeyboardUtils
import cum.xiaro.trollhack.util.text.MessageSendUtils
import cum.xiaro.trollhack.util.text.formatValue

object MacroCommand : ClientCommand(
    name = "macro",
    alias = arrayOf("m"),
    description = "Manage your command / message macros"
) {
    init {
        literal("list") {
            string("key") { keyArg ->
                execute("List macros for a key") {
                    val input = keyArg.value
                    val key = KeyboardUtils.getKey(input)

                    if (key !in 1..255) {
                        KeyboardUtils.sendUnknownKeyError(input)
                        return@execute
                    }

                    val macros = MacroManager.macros[key]
                    val formattedName = formatValue(KeyboardUtils.getDisplayName(key) ?: "Unknown")

                    if (macros.isNullOrEmpty()) {
                        MessageSendUtils.sendNoSpamChatMessage("§cYou have no macros for the key $formattedName")
                    } else {
                        val stringBuilder = StringBuffer()
                        stringBuilder.appendLine("You have has the following macros for $formattedName:")

                        for (macro in macros) {
                            stringBuilder.appendLine("$formattedName $macro")
                        }

                        MessageSendUtils.sendNoSpamChatMessage(stringBuilder.toString())
                    }
                }
            }

            execute("List all macros") {
                if (MacroManager.isEmpty) {
                    MessageSendUtils.sendNoSpamChatMessage("§cYou have no macros")
                } else {
                    val stringBuilder = StringBuffer()
                    stringBuilder.appendLine("You have the following macros:")

                    for ((key, value) in MacroManager.macros) {
                        val formattedName = formatValue(KeyboardUtils.getDisplayName(key) ?: "Unknown")
                        stringBuilder.appendLine("$formattedName $value")
                    }

                    MessageSendUtils.sendNoSpamChatMessage(stringBuilder.toString())
                }
            }
        }

        literal("clear") {
            string("key") { keyArg ->
                execute("Clear macros for a key") {
                    val input = keyArg.value
                    val key = KeyboardUtils.getKey(input)

                    if (key !in 1..255) {
                        KeyboardUtils.sendUnknownKeyError(input)
                        return@execute
                    }

                    val formattedName = formatValue(KeyboardUtils.getDisplayName(key) ?: "Unknown")

                    MacroManager.removeMacro(key)
                    MacroManager.saveMacros()
                    MacroManager.loadMacros()
                    MessageSendUtils.sendNoSpamChatMessage("Cleared macros for $formattedName")
                }
            }
        }

        literal("add") {
            string("key") { keyArg ->
                greedy("command / message") { macroArg ->
                    execute("Add a command / message for a key") {
                        val input = keyArg.value
                        val key = KeyboardUtils.getKey(input)

                        if (key !in 1..255) {
                            KeyboardUtils.sendUnknownKeyError(input)
                            return@execute
                        }

                        val macro = macroArg.value
                        val formattedName = formatValue(KeyboardUtils.getDisplayName(key) ?: "Unknown")

                        MacroManager.addMacro(key, macro)
                        MacroManager.saveMacros()
                        MessageSendUtils.sendNoSpamChatMessage("Added macro ${formatValue(macro)} for key $formattedName")
                    }
                }
            }
        }

        literal("set") {
            string("key") { keyArg ->
                greedy("command / message") { macroArg ->
                    execute("Set a command / message for a key") {
                        val input = keyArg.value
                        val key = KeyboardUtils.getKey(input)

                        if (key !in 1..255) {
                            KeyboardUtils.sendUnknownKeyError(input)
                            return@execute
                        }

                        val macro = macroArg.value
                        val formattedName = formatValue(KeyboardUtils.getDisplayName(key) ?: "Unknown")

                        MacroManager.setMacro(key, macro)
                        MacroManager.saveMacros()
                        MessageSendUtils.sendNoSpamChatMessage("Added macro ${formatValue(macro)} for key $formattedName")
                    }
                }
            }
        }
    }
}