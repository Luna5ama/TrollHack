package dev.luna5ama.trollhack.module.modules.misc

import dev.luna5ama.trollhack.mixins.core.world.MixinWorld
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module

/**
 * @see MixinWorld.getThunderStrengthHead
 * @see MixinWorld.getRainStrengthHead
 */
internal object AntiWeather : Module(
    name = "Anti Weather",
    description = "Removes rain and thunder from your world",
    category = Category.MISC
)