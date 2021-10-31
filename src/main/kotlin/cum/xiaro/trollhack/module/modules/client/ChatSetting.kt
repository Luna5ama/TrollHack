package cum.xiaro.trollhack.module.modules.client

import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module

internal object ChatSetting : Module(
    name = "ChatSetting",
    category = Category.CLIENT,
    description = "Configures chat message manager",
    visible = false,
    alwaysEnabled = true
) {
    val delay by setting("Message Delay", 500, 0..20000, 50, description = "Delay between each message in ms")
    val maxMessageQueueSize by setting("Max Message Queue Size", 50, 10..200, 5)
}