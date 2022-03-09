package me.luna.trollhack.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.luna.trollhack.TrollHackMod
import me.luna.trollhack.module.modules.client.Language
import me.luna.trollhack.util.Wrapper
import me.luna.trollhack.util.threads.defaultScope
import me.luna.trollhack.util.threads.isActiveOrFalse
import java.io.File

object TranslationManager {
    private var translationMap: TranslationMap? = null
    private var lastJob: Job? = null

    private val settingLanguage: String
        get() = if (Language.overrideLanguage) {
            Language.language
        } else {
            Wrapper.minecraft.gameSettings.language
        }

    init {
        val file = File(I18N_DIR)
        if (!file.exists()) file.mkdir()
    }

    fun TranslationKey.getTranslated(): String {
        if (settingLanguage == "en_us" || settingLanguage == "en_uk") {
            return rootString
        }

        return translationMap?.let {
            it[this]
        } ?: run {
            if (!lastJob.isActiveOrFalse) {
                lastJob = defaultScope.launch(Dispatchers.IO) {
                    reload()
                }
            }
            rootString
        }
    }

    fun reload() {
        val file = File("$I18N_DIR/$settingLanguage.lang")

        try {
            val map = TranslationMap.fromFile(file)
            translationMap = map
            TrollHackMod.logger.info("Loaded language file ${file.name}")
            TranslationKey.updateAll()
        } catch (e: IllegalArgumentException) {
            TrollHackMod.logger.warn(e.message)
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed to load language file ${file.name}", e)
        }
    }

    fun dump() {
        val file = File("$I18N_DIR/en_us.lang")
        file.createNewFile()
        file.bufferedWriter().use { writer ->
            TranslationKey.allKeys
                .sortedBy { it.keyString }
                .forEach {
                    writer.appendLine("${it.keyString}=${it.rootString}")
                }
        }
    }

    fun update() {
        val file = File("$I18N_DIR/$settingLanguage.lang")
        file.createNewFile()
        file.bufferedWriter().use { writer ->
            TranslationKey.allKeys
                .sortedBy { it.keyString }
                .forEach {
                    writer.appendLine("${it.keyString}=$it")
                }
        }
    }
}