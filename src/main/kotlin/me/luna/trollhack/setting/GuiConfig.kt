package me.luna.trollhack.setting

import me.luna.trollhack.TrollHackMod
import me.luna.trollhack.gui.rgui.Component
import me.luna.trollhack.module.modules.client.Configurations
import me.luna.trollhack.setting.configs.AbstractConfig
import me.luna.trollhack.setting.settings.AbstractSetting
import me.luna.trollhack.util.extension.rootName
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