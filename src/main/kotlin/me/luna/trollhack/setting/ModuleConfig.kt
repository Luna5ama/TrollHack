package me.luna.trollhack.setting

import me.luna.trollhack.TrollHackMod
import me.luna.trollhack.module.AbstractModule
import me.luna.trollhack.module.modules.client.Configurations
import me.luna.trollhack.setting.configs.NameableConfig
import java.io.File

internal object ModuleConfig : NameableConfig<AbstractModule>(
    "modules",
    "${TrollHackMod.DIRECTORY}/config/modules",
) {
    override val file: File get() = File("$filePath/${Configurations.modulePreset}.json")
    override val backup get() = File("$filePath/${Configurations.modulePreset}.bak")
}