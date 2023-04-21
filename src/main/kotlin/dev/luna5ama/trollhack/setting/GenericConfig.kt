package dev.luna5ama.trollhack.setting

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.setting.configs.NameableConfig

internal object GenericConfig : NameableConfig<GenericConfigClass>(
    "generic",
    "${TrollHackMod.DIRECTORY}/config/"
)