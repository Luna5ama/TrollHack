package dev.luna5ama.trollhack.module.modules.combat

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.text.MessageSendUtils
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage
import dev.luna5ama.trollhack.util.threads.runSafe

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
