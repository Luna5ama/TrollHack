package cum.xiaro.trollhack.setting.groups

import cum.xiaro.trollhack.util.interfaces.Nameable
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import cum.xiaro.trollhack.TrollHackMod
import cum.xiaro.trollhack.setting.settings.AbstractSetting
import cum.xiaro.trollhack.util.extension.rootName
import java.util.*

open class SettingGroup(
    override val name: CharSequence
) : Nameable {

    /** Settings in this group */
    protected val subSetting = LinkedHashMap<String, AbstractSetting<*>>()


    /**
     * Get a copy of the list of settings in this group
     *
     * @return A copy of [subSetting]
     */
    fun getSettings() = subSetting.values.toList()

    /**
     * Adds a setting to this group
     *
     * @param S type of the setting
     * @param setting Setting to add
     *
     * @return [setting]
     */
    open fun <S : AbstractSetting<*>> addSetting(setting: S): S {
        subSetting[setting.rootName.lowercase()] = setting
        return setting
    }


    /**
     * Writes setting values to a [JsonObject]
     *
     * @return [JsonObject] contains all the setting values
     */
    open fun write(): JsonObject = JsonObject().apply {
        add("name", JsonPrimitive(rootName))

        if (subSetting.isNotEmpty()) {
            add("settings", JsonObject().apply {
                for (setting in subSetting.values) {
                    add(setting.rootName.toJsonName(), setting.write())
                }
            })
        }
    }

    /**
     * Read setting values from a [JsonObject]
     *
     * @param jsonObject [JsonObject] to read from
     */
    open fun read(jsonObject: JsonObject) {
        if (subSetting.isNotEmpty()) {
            (jsonObject.get("settings") as? JsonObject)?.also { settings ->
                for (setting in subSetting.values) {
                    try {
                        settings.get(setting.rootName.toJsonName())?.let {
                            setting.read(it)
                        }
                    } catch (e: Exception) {
                        TrollHackMod.logger.warn("Failed loading setting ${setting.name} at $name", e)
                    }
                }
            }
        }
    }

    private fun String.toJsonName() =
        this.replace(' ', '_')
            .lowercase(Locale.ROOT)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SettingGroup

        if (name != other.name) return false
        if (subSetting != other.subSetting) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + subSetting.hashCode()
        return result
    }

}