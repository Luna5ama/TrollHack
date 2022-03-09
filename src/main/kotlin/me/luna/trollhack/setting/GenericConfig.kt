package me.luna.trollhack.setting

import me.luna.trollhack.TrollHackMod
import me.luna.trollhack.setting.configs.NameableConfig

internal object GenericConfig : NameableConfig<GenericConfigClass>(
    "generic",
    "${TrollHackMod.DIRECTORY}/config/"
)