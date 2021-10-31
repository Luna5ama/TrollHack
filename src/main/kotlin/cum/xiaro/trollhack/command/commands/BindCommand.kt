package cum.xiaro.trollhack.command.commands

import cum.xiaro.trollhack.command.ClientCommand
import cum.xiaro.trollhack.module.ModuleManager
import cum.xiaro.trollhack.util.KeyboardUtils
import cum.xiaro.trollhack.util.text.MessageSendUtils
import cum.xiaro.trollhack.util.text.formatValue

object BindCommand : ClientCommand(
    name = "bind",
    description = "Bind and unbind modules"
) {
    init {
        literal("list") {
            execute("List used module binds") {
                val binds = ModuleManager.modules.asSequence()
                    .filter { it.bind.value.key in 1..255 }
                    .map { "${formatValue(it.bind)} ${it.name}" }
                    .sorted()
                    .toList()

                val stringBuilder = StringBuffer()
                stringBuilder.appendLine("Used binds: ${formatValue(binds.size)}")

                binds.forEach {
                    stringBuilder.appendLine(it)
                }

                MessageSendUtils.sendNoSpamChatMessage(stringBuilder.toString())
            }
        }

        literal("reset", "unbind") {
            module("module") { moduleArg ->
                execute("Reset the bind of a module to nothing") {
                    val module = moduleArg.value
                    module.bind.resetValue()
                    MessageSendUtils.sendNoSpamChatMessage("Reset bind for ${module.name}!")
                }
            }
        }

        module("module") { moduleArg ->
            string("bind") { bindArg ->
                execute("Bind a module to a key") {
                    val module = moduleArg.value
                    val bind = bindArg.value

                    if (bind.equals("None", true)) {
                        module.bind.resetValue()
                        MessageSendUtils.sendNoSpamChatMessage("Reset bind for ${module.name}!")
                        return@execute
                    }

                    val key = KeyboardUtils.getKey(bind)

                    if (key !in 1..255) {
                        KeyboardUtils.sendUnknownKeyError(bind)
                    } else {
                        module.bind.setValue(bind)
                        MessageSendUtils.sendNoSpamChatMessage("Bind for ${module.name} set to ${formatValue(module.bind)}!")
                    }
                }
            }

            execute("Get the bind of a module") {
                val module = moduleArg.value
                MessageSendUtils.sendNoSpamChatMessage("${module.name} is bound to ${formatValue(module.bind)}")
            }
        }
    }
}