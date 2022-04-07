package me.luna.trollhack.command.commands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.luna.trollhack.command.ClientCommand
import me.luna.trollhack.translation.I18N_LOCAL_DIR
import me.luna.trollhack.translation.TranslationManager
import me.luna.trollhack.util.text.NoSpamMessage
import me.luna.trollhack.util.threads.defaultScope

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