package cum.xiaro.trollhack.setting.configs

import cum.xiaro.trollhack.util.interfaces.Nameable
import cum.xiaro.trollhack.setting.settings.AbstractSetting
import cum.xiaro.trollhack.util.extension.rootName

open class NameableConfig<T : Nameable>(
    name: String,
    filePath: String
) : AbstractConfig<T>(name, filePath) {

    override fun addSettingToConfig(owner: T, setting: AbstractSetting<*>) {
        getGroupOrPut(owner.rootName).addSetting(setting)
    }

    open fun getSettings(nameable: Nameable) = getGroup(nameable.rootName)?.getSettings() ?: emptyList()

}
