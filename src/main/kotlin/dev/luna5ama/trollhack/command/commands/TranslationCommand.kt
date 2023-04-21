package dev.luna5ama.trollhack.command.commands

import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.translation.I18N_LOCAL_DIR
import dev.luna5ama.trollhack.translation.TranslationManager
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.defaultScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TranslationCommand : ClientCommand(
    name = "translation",
    alias = arrayOf("i18n")
) {
    init {
        literal("dump") {
            execute {
                defaultScope.launch(Dispatchers.Default) {
                    TranslationManager.dump()
                    NoSpamMessage.sendMessage(TranslationCommand, "Dumped root lang to $I18N_LOCAL_DIR")
                }
            }
        }

        literal("reload") {
            execute {
                defaultScope.launch(Dispatchers.IO) {
                    TranslationManager.reload()
                    NoSpamMessage.sendMessage(TranslationCommand, "Reloaded translations")
                }
            }
        }

        literal("update") {
            string("language") {
                execute {
                    defaultScope.launch(Dispatchers.IO) {
                        TranslationManager.update()
                        NoSpamMessage.sendMessage(TranslationCommand, "Updated translation")
                    }
                }
            }
        }
    }
}