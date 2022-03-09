package me.luna.trollhack.module.modules.misc

import me.luna.trollhack.mixins.core.world.MixinWorld
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module

/**
 * @see MixinWorld.getThunderStrengthHead
 * @see MixinWorld.getRainStrengthHead
 */
internal object AntiWeather : Module(
    name = "AntiWeather",
    description = "Removes rain and thunder from your world",
    category = Category.MISC
)
