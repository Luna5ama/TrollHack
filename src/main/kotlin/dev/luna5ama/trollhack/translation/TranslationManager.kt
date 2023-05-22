package dev.luna5ama.trollhack.translation

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.module.modules.client.Language
import dev.luna5ama.trollhack.util.readText
import dev.luna5ama.trollhack.util.threads.DefaultScope
import dev.luna5ama.trollhack.util.threads.isActiveOrFalse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL
import java.util.*
import java.util.zip.ZipFile

object TranslationManager {
    private var translationMap: TranslationMap? = null
    private var lastJob: Job? = null

    init {
        val local = File(I18N_LOCAL_DIR)
        if (!local.exists()) local.mkdir()
    }

    fun checkUpdate() {
        DefaultScope.launch(Dispatchers.IO) {
            try {
                var localHash: String? = null

                val onlineCacheFile = File(I18N_ONLINE_CACHE_DIR)
                if (onlineCacheFile.exists()) {
                    val zipFile = ZipFile(onlineCacheFile)
                    localHash = zipFile.entries().asSequence()
                        .find { it.name.substringAfterLast('/') == ".hash" }
                        ?.let { entry ->
                            zipFile.getInputStream(entry).use { it.readText() }
                        }
                }

                val onlineHash = URL(I18N_ONLINE_HASH_URL).readText()

                if (localHash != onlineHash) {
                    URL(I18N_ONLINE_DOWNLOAD_URL).openStream().use { online ->
                        onlineCacheFile.outputStream().use {
                            online.copyTo(it)
                        }
                    }
                }
            } catch (e: Exception) {
                TrollHackMod.logger.warn("Failed to check for translation updates", e)
            }
        }
    }

    fun TranslationKey.getTranslated(): String {
        if (Language.settingLanguage.startsWith("en")) {
            return rootString
        }

        return translationMap?.let {
            it[this]
        } ?: run {
            if (!lastJob.isActiveOrFalse) {
                lastJob = DefaultScope.launch(Dispatchers.IO) {
                    reload()
                }
            }
            rootString
        }
    }

    fun reload() {
        val language = Language.settingLanguage.lowercase(Locale.ROOT)

        try {
            val (content, source) = tryRead(language)
            val map = TranslationMap.fromString(language, content)
            translationMap = map
            TrollHackMod.logger.info("Loaded language $language from $source")
            TranslationKey.updateAll()
        } catch (e: IllegalArgumentException) {
            TrollHackMod.logger.warn(e.message)
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed to load language $language", e)
        }
    }

    private fun tryRead(language: String): Pair<String, String> {
        tryReadLocal(language)?.let {
            return it to "local"
        }

        tryReadOnline(language)?.let {
            return it to "online"
        }

        tryReadJar(language)?.let {
            return it to "jar"
        }

        throw IllegalArgumentException("No .lang file found for language $language")
    }

    private fun tryReadLocal(language: String): String? {
        val file = File("$I18N_LOCAL_DIR/$language.lang")
        if (file.exists()) {
            return file.readText()
        }

        return null
    }

    private fun tryReadOnline(language: String): String? {
        val file = File(I18N_ONLINE_CACHE_DIR)
        if (file.exists()) {
            val zipFile = ZipFile(file)
            zipFile.entries().asSequence()
                .find { it.name.substringAfterLast('/') == "$language.lang" }
                ?.let { entry ->
                    return zipFile.getInputStream(entry).use { it.readText() }
                }
        }

        return null
    }

    private fun tryReadJar(language: String): String? {
        return javaClass.getResource("$I18N_JAR_DIR/$language.lang")?.readText()
    }

    fun dump() {
        val file = File("$I18N_LOCAL_DIR/en_us.lang")
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
        val file = File("$I18N_LOCAL_DIR/${Language.settingLanguage}.lang")
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