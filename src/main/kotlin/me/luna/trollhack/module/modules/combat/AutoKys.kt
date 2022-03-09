package me.luna.trollhack.module.modules.combat

import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.text.MessageSendUtils
import me.luna.trollhack.util.text.MessageSendUtils.sendServerMessage
import me.luna.trollhack.util.threads.runSafe

internal object AutoKys : Module(
    name = "AutoKys",
    description = "Do /kill",
    category = Category.COMBAT
) {
    init {
        onEnable {
            runSafe {
                MessageSendUtils.sendServerMessage("/kill")
            }
            disable()
        }
    }
}
