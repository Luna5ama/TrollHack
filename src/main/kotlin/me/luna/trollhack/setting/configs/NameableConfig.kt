package me.luna.trollhack.setting.configs

import me.luna.trollhack.setting.settings.AbstractSetting
import me.luna.trollhack.util.extension.rootName
import me.luna.trollhack.util.interfaces.Nameable

open class NameableConfig<T : Nameable>(
    name: String,
    filePath: String
) : AbstractConfig<T>(name, filePath) {

    override fun addSettingToConfig(owner: T, setting: AbstractSetting<*>) {
        getGroupOrPut(owner.rootName).addSetting(setting)
    }

    open fun getSettings(nameable: Nameable) = getGroup(nameable.rootName)?.getSettings() ?: emptyList()

}
