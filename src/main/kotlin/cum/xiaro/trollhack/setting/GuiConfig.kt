package cum.xiaro.trollhack.setting

import cum.xiaro.trollhack.TrollHackMod
import cum.xiaro.trollhack.gui.rgui.Component
import cum.xiaro.trollhack.module.modules.client.Configurations
import cum.xiaro.trollhack.setting.configs.AbstractConfig
import cum.xiaro.trollhack.setting.settings.AbstractSetting
import cum.xiaro.trollhack.util.extension.rootName
import java.io.File

internal object GuiConfig : AbstractConfig<Component>(
    "gui",
    "${TrollHackMod.DIRECTORY}/config/gui"
) {
    override val file get() = File("$filePath/${Configurations.guiPreset}.json")
    override val backup get() = File("$filePath/${Configurations.guiPreset}.bak")

    override fun addSettingToConfig(owner: Component, setting: AbstractSetting<*>) {
        val groupName = owner.settingGroup.groupName
        if (groupName.isNotEmpty()) {
            getGroupOrPut(groupName).getGroupOrPut(owner.rootName).addSetting(setting)
        }
    }
}