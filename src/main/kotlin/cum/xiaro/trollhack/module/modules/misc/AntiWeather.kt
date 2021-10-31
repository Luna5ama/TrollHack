package cum.xiaro.trollhack.module.modules.misc

import cum.xiaro.trollhack.mixin.world.MixinWorld
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module

/**
 * @see MixinWorld.getThunderStrengthHead
 * @see MixinWorld.getRainStrengthHead
 */
internal object AntiWeather : Module(
    name = "AntiWeather",
    description = "Removes rain and thunder from your world",
    category = Category.MISC
)
