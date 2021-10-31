package cum.xiaro.trollhack.command.commands

import cum.xiaro.trollhack.command.ClientCommand
import cum.xiaro.trollhack.util.text.NoSpamMessage
import cum.xiaro.trollhack.util.threads.defaultScope
import cum.xiaro.trollhack.util.translation.I18N_DIR
import cum.xiaro.trollhack.util.translation.TranslationManager
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
                    NoSpamMessage.sendMessage(TranslationCommand, "Dumped root lang to $I18N_DIR")
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