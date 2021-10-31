package cum.xiaro.trollhack.setting

import cum.xiaro.trollhack.TrollHackMod
import cum.xiaro.trollhack.setting.configs.NameableConfig

internal object GenericConfig : NameableConfig<GenericConfigClass>(
    "generic",
    "${TrollHackMod.DIRECTORY}/config/"
)