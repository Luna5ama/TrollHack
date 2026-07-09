package dev.luna5ama.trollhack.language

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import dev.luna5ama.trollhack.TrollHackMod.FOLDER
import net.minecraft.locale.Language
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Path
import java.util.LinkedList

class Config private constructor() {
    var version: Int = 0

    @JvmField
    var multilingualItemSearch: Boolean = true
    @JvmField
    var fallbacks: LinkedList<String> = LinkedList<String>()
    @JvmField
    var previousFallbacks: LinkedList<String> = LinkedList<String>()
    @JvmField
    var language: String = ""
    @JvmField
    var previousLanguage: String = ""

    companion object {
        private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
        private val PATH: Path = FOLDER.toPath().resolve("client_language.json")

        private lateinit var INSTANCE: Config

        fun load() {
            if (Files.notExists(PATH)) {
                INSTANCE = Config()
                save()
            } else try {
                INSTANCE = GSON.fromJson<Config?>(Files.readString(PATH), Config::class.java)
            } catch (e: Exception) {
                INSTANCE = Config()
                TrollHackMod.LOGGER.error("Couldn't load config file: ", e)
            }

            migrateToVersion1()

            if (INSTANCE.language == TrollHackMod.NO_LANGUAGE && !INSTANCE.fallbacks.isEmpty()) {
                INSTANCE.language = INSTANCE.fallbacks.pollFirst()
            }
            if (INSTANCE.previousLanguage == TrollHackMod.NO_LANGUAGE && !INSTANCE.previousFallbacks.isEmpty()) {
                INSTANCE.previousLanguage = INSTANCE.previousFallbacks.pollFirst()
            }
        }

        @JvmStatic
        fun save() {
            if (!::INSTANCE.isInitialized) return
            try {
                Files.write(PATH, mutableSetOf<String?>(GSON.toJson(INSTANCE)))
            } catch (e: Exception) {
                TrollHackMod.LOGGER.error("Couldn't save config file: ", e)
            }
        }

        @JvmStatic
        fun getInstance(): Config {
            if (!::INSTANCE.isInitialized) load()
            return INSTANCE
        }

        private fun migrateToVersion1() {
            if (INSTANCE.version >= 1) return
            INSTANCE.version = 1

            if (!INSTANCE.language.isEmpty()
                && INSTANCE.language != Language.DEFAULT && !INSTANCE.fallbacks.contains(Language.DEFAULT)
            ) INSTANCE.fallbacks.add(Language.DEFAULT)

            if (!INSTANCE.previousLanguage.isEmpty()
                && INSTANCE.previousLanguage != Language.DEFAULT && !INSTANCE.previousFallbacks.contains(
                    Language.DEFAULT
                )
            ) INSTANCE.previousFallbacks.add(Language.DEFAULT)

            save()
        }
    }
}
