package dev.luna5ama.trollhack.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import dev.luna5ama.trollhack.config.filesystem.AbstractFile
import dev.luna5ama.trollhack.manager.managers.ConfigManager
import dev.luna5ama.trollhack.utils.Nameable
import dev.luna5ama.trollhack.utils.Supervisor
import dev.luna5ama.trollhack.utils.extension.listFilesRecursively
import dev.luna5ama.trollhack.utils.extension.recreateAndBackup
import dev.luna5ama.trollhack.graphics.font.TextComponent

@Suppress("LoggingSimilarMessage")
class NamespacedConfigurationManager(override val name: CharSequence) : Nameable {
    private val folder get() = ConfigManager.currentConfigRoot.resolve(name.toString())
    private val namedFolder get() = folder.resolve("named")
    private val prettier = GsonBuilder().setPrettyPrinting().create()
    private val namedConfigurations = mutableListOf<Configurable.NamedConfigurable>()
    private val anonymousConfigurations = mutableListOf<Configurable.AnonymousConfigurable>()

    fun addConfigurable(named: Configurable.NamedConfigurable) {
        namedConfigurations.add(named)
    }

    fun addConfigurable(anonymous: Configurable.AnonymousConfigurable) {
        anonymousConfigurations.add(anonymous)
    }

    /**
     * Clean backup files. If folder is not existent then call [save]
     */
    fun clean() {
        if (!folder.exists() || !namedFolder.exists()) save()
        else {
            cleanFiles(folder.listFilesRecursively())
            cleanFiles(namedFolder.listFilesRecursively())
        }
    }

    private fun cleanFiles(files0: List<AbstractFile>) {
        val files = files0.filter { it.isFile && it.name.endsWith(".bak") }.toMutableList()
        files.forEach { it.delete() }
    }

    /**
     * Read configurations. If folder is not existent then call [save]
     */
    fun read() {
        if (!folder.exists() || !namedFolder.exists()) save()
        else {
            val supervisor = Supervisor()
            val warningMessage = TextComponent()
            with(warningMessage) {
                addLine("Configurations of the terms following cannot be loaded properly:")
                TrollHackMod.LOGGER.debug(namedFolder.path.toString())
                val namedConfigJsons = namedFolder.listFilesRecursively().filter { it.name.endsWith(".json") }
                    .associateBy {
                        it.absolutePath.removeSuffix(".json")
                            .removePrefix(namedFolder.absolutePath)
                            .removePrefix("/")
                    }
                namedConfigurations.forEach { named ->
                    if (named.excluded) return@forEach // FIXME
                    supervisor {
                        try {
                            val jsonText = namedConfigJsons[named.nameAsString]?.readText()
                                ?: throw RuntimeException(namedFolder.absolutePath)
                            val json = JsonParser.parseString(jsonText).asJsonObject
                            readJsonObject(json, named)
                        } catch (e: Exception) {
                            val message = "  Failed to load configurable object ${named.nameAsString}"
                            TrollHackMod.LOGGER.warn(message, e)
                            addLine(message)
                            errorOccurred = true
                        }
                    }
                }

                val anonymousJson = JsonParser
                    .parseString(folder.resolve("anonymous.json").readText()).asJsonObject
                anonymousConfigurations.forEach { anonymous ->
                    if (anonymous.excluded) return@forEach
                    supervisor {
                        try {
                            val json = anonymousJson[anonymous.nameAsString].asJsonObject
                            readJsonObject(json, anonymous)
                        } catch (e: Exception) {
                            val message = "  Failed to load configurable object ${anonymous.nameAsString}"
                            TrollHackMod.LOGGER.warn(message)
                            addLine(message)
                            errorOccurred = true
                        }
                    }
                }
            }
            if (supervisor.errorOccurred) {
                TrollHackMod.messageDialog = warningMessage
                TrollHackMod.showMessageDialog = true
                save()
            }
        }
    }

    /**
     * Save configurations
     */
    fun save() {
        if (!folder.exists()) folder.mkdirs()
        if (!namedFolder.isDirectory) namedFolder.delete()
        namedFolder.mkdirs()

        namedConfigurations.forEach { named ->
            if (named.excluded) return@forEach
            val json = JsonObject()
            writeJsonObject(json, named)
            val jsonFile = namedFolder.resolve(named.nameAsString + ".json")
            jsonFile.recreateAndBackup()
            jsonFile.writeText(prettier.toJson(json), Charsets.UTF_8)
        }

        val anonymousJson = JsonObject()
        anonymousConfigurations.forEach { anonymous ->
            if (anonymous.excluded) return@forEach
            val json = JsonObject()
            writeJsonObject(json, anonymous)
            anonymousJson.add(anonymous.nameAsString, json)
        }
        val jsonFile = folder.resolve("anonymous.json")
        jsonFile.recreateAndBackup()
        jsonFile.writeText(prettier.toJson(anonymousJson), Charsets.UTF_8)
    }

    private fun writeJsonObject(json: JsonObject, configurable: Configurable) {
        configurable.settings.forEach { setting ->
            setting.writeJson(json)
        }
    }

    context(Supervisor, TextComponent)
    private fun readJsonObject(json: JsonObject, configurable: Configurable) {
        configurable.settings.forEach { setting ->
            try {
                setting.chooseJsonElement(json)?.let {
                    setting.readJson(it)
                }
            } catch (e: Exception) {
                val warningMessage = "  Failed to load setting '${setting.nameAsString}'" +
                        " in configurable object $configurable"
                TrollHackMod.LOGGER.warn(warningMessage)
                addLine("\t$warningMessage")
                errorOccurred = true
            }
        }
    }
}