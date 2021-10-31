package cum.xiaro.trollhack.setting

import cum.xiaro.trollhack.TrollHackMod
import cum.xiaro.trollhack.module.AbstractModule
import cum.xiaro.trollhack.module.modules.client.Configurations
import cum.xiaro.trollhack.setting.configs.NameableConfig
import java.io.File

internal object ModuleConfig : NameableConfig<AbstractModule>(
    "modules",
    "${TrollHackMod.DIRECTORY}/config/modules",
) {
    override val file: File get() = File("$filePath/${Configurations.modulePreset}.json")
    override val backup get() = File("$filePath/${Configurations.modulePreset}.bak")
}