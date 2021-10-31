package cum.xiaro.trollhack.module.modules.client

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module

internal object Hud : Module(
    name = "Hud",
    description = "Toggles Hud displaying and settings",
    category = Category.CLIENT,
    visible = false,
    enabledByDefault = true
) {
    val primaryColor by setting("Primary Color", ColorRGB(255, 250, 253), false)
    val secondaryColor by setting("Secondary Color", ColorRGB(255, 135, 230), false)
}