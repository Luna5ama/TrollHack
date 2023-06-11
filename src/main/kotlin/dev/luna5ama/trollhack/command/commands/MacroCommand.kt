package dev.luna5ama.trollhack.command.commands

import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.manager.managers.MacroManager
import dev.luna5ama.trollhack.util.KeyboardUtils
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.text.formatValue

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

                    if (macros.isEmpty()) {
                        NoSpamMessage.sendMessage("§cYou have no macros for the key $formattedName")
                    } else {
                        val stringBuilder = StringBuffer()
                        stringBuilder.appendLine("You have has the following macros for $formattedName:")

                        for (macro in macros) {
                            stringBuilder.appendLine("$formattedName $macro")
                        }

                        NoSpamMessage.sendMessage(stringBuilder.toString())
                    }
                }
            }

            execute("List all macros") {
                if (MacroManager.isEmpty) {
                    NoSpamMessage.sendMessage("§cYou have no macros")
                } else {
                    val stringBuilder = StringBuffer()
                    stringBuilder.appendLine("You have the following macros:")

                    for ((key, value) in MacroManager.macros.withIndex()) {
                        val formattedName = formatValue(KeyboardUtils.getDisplayName(key) ?: "Unknown")
                        stringBuilder.appendLine("$formattedName $value")
                    }

                    NoSpamMessage.sendMessage(stringBuilder.toString())
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
                    NoSpamMessage.sendMessage("Cleared macros for $formattedName")
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
                        NoSpamMessage.sendMessage("Added macro ${formatValue(macro)} for key $formattedName")
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
                        NoSpamMessage.sendMessage("Added macro ${formatValue(macro)} for key $formattedName")
                    }
                }
            }
        }
    }
}