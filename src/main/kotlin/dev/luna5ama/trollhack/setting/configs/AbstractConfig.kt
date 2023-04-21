package dev.luna5ama.trollhack.setting.configs

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.setting.groups.SettingGroup
import dev.luna5ama.trollhack.setting.groups.SettingMultiGroup
import dev.luna5ama.trollhack.setting.settings.AbstractSetting
import dev.luna5ama.trollhack.setting.settings.SettingRegister
import dev.luna5ama.trollhack.util.ConfigUtils
import java.io.File

abstract class AbstractConfig<T : Any>(
    name: String,
    val filePath: String
) : SettingMultiGroup(name), IConfig, SettingRegister<T> {

    override val file get() = File("$filePath$name.json")
    override val backup get() = File("$filePath$name.bak")

    final override fun <S : AbstractSetting<*>> T.setting(setting: S): S {
        addSettingToConfig(this, setting)
        return setting
    }

    abstract fun addSettingToConfig(owner: T, setting: AbstractSetting<*>)

    override fun save() {
        val directory = File(filePath)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        saveToFile(this, file, backup)
    }

    override fun load() {
        val directory = File(filePath)
        if (!directory.exists()) {
            directory.mkdirs()
            return
        }

        try {
            loadFromFile(this, file)
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed to load latest, loading backup.")
            loadFromFile(this, backup)
        }
    }

    /**
     * Save a group to a file
     *
     * @param group Group to save
     * @param file Main file of [group]'s json
     * @param backup Backup file of [group]'s json
     */
    protected fun saveToFile(group: SettingGroup, file: File, backup: File) {
        ConfigUtils.fixEmptyJson(file)
        ConfigUtils.fixEmptyJson(backup)
        if (file.exists()) file.copyTo(backup, true)

        file.bufferedWriter().use {
            gson.toJson(group.write(), it)
        }
    }

    /**
     * Load settings values of a group
     *
     * @param group Group to load
     * @param file file of [group]'s json
     */
    protected fun loadFromFile(group: SettingGroup, file: File) {
        ConfigUtils.fixEmptyJson(file)

        parser.parse(file.readText()).asJsonObject?.let {
            group.read(it)
        }
    }

    /**
     * Contains a gson object and a parser object
     */
    protected companion object {
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        val parser = JsonParser()
    }

}