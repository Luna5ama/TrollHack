package dev.luna5ama.trollhack.command.commands

import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.translation.I18N_LOCAL_DIR
import dev.luna5ama.trollhack.translation.TranslationManager
import dev.luna5ama.trollhack.util.text.NoSpamMessage

object TranslationCommand : ClientCommand(
    name = "translation",
    alias = arrayOf("i18n")
) {
    init {
        literal("dump") {
            executeAsync {
                TranslationManager.dump()
                NoSpamMessage.sendMessage(TranslationCommand, "Dumped root lang to $I18N_LOCAL_DIR")
            }
        }

        literal("reload") {
            executeAsync {
                TranslationManager.reload()
                NoSpamMessage.sendMessage(TranslationCommand, "Reloaded translations")
            }
        }

        literal("update") {
            string("language") {
                executeAsync {
                    TranslationManager.update()
                    NoSpamMessage.sendMessage(TranslationCommand, "Updated translation")
                }
            }
        }
    }
}