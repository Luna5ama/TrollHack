package dev.luna5ama.trollhack.setting.configs

import dev.luna5ama.trollhack.setting.settings.AbstractSetting
import dev.luna5ama.trollhack.util.interfaces.Nameable

open class NameableConfig<T : Nameable>(
    name: String,
    filePath: String
) : AbstractConfig<T>(name, filePath) {

    override fun addSettingToConfig(owner: T, setting: AbstractSetting<*>) {
        getGroupOrPut(owner.internalName).addSetting(setting)
    }

    open fun getSettings(nameable: Nameable) = getGroup(nameable.internalName)?.getSettings() ?: emptyList()

}
