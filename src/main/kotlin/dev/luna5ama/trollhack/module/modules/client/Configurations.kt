package dev.luna5ama.trollhack.module.modules.client

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.gui.AbstractTrollGui
import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.setting.ConfigManager
import dev.luna5ama.trollhack.setting.GenericConfig
import dev.luna5ama.trollhack.setting.GuiConfig
import dev.luna5ama.trollhack.setting.ModuleConfig
import dev.luna5ama.trollhack.setting.configs.AbstractConfig
import dev.luna5ama.trollhack.setting.configs.IConfig
import dev.luna5ama.trollhack.setting.settings.impl.primitive.StringSetting
import dev.luna5ama.trollhack.util.ConfigUtils
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.text.formatValue
import dev.luna5ama.trollhack.util.threads.DefaultScope
import dev.luna5ama.trollhack.util.threads.TimerScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.nio.file.Paths

internal object Configurations : AbstractModule(
    name = "Configurations",
    description = "Setting up configurations of the client",
    category = Category.CLIENT,
    alwaysEnabled = true,
    visible = false,
    config = GenericConfig
) {
    private const val defaultPreset = "default"

    private val autoSaving0 = setting("Auto Saving", true)
    private val autoSaving by autoSaving0
    private val savingFeedBack by setting("Saving FeedBack", false, autoSaving0.atTrue())
    private val savingInterval by setting(
        "Interval",
        10,
        1..30,
        1,
        autoSaving0.atTrue(),
        description = "Frequency of auto saving in minutes"
    )
    val serverPreset by setting("Server Preset", false)
    private val guiPresetSetting = setting("Gui Preset", defaultPreset)
    private val modulePresetSetting = setting("Module Preset", defaultPreset)

    val guiPreset by guiPresetSetting
    val modulePreset by modulePresetSetting

    private val timer = TickTimer(TimeUnit.MINUTES)
    private var connected = false

    init {
        TimerScope.launchLooping("Config Auto Saving", 60000L) {
            if (autoSaving && mc.currentScreen !is AbstractTrollGui && timer.tickAndReset(savingInterval.toLong())) {
                if (savingFeedBack) NoSpamMessage.sendMessage(this@Configurations, "Auto saving settings...")
                else TrollHackMod.logger.info("Auto saving settings...")
                ConfigUtils.saveAll()
            }
        }

        listener<ConnectionEvent.Connect> {
            connected = true
        }

        safeListener<TickEvent.Pre> {
            if (serverPreset && connected && !mc.isIntegratedServerRunning) {
                val ip = mc.currentServerData?.serverIP ?: return@safeListener
                connected = false
                ConfigType.GUI.setServerPreset(ip)
                ConfigType.MODULES.setServerPreset(ip)
            } else {
                connected = false
            }
        }
    }

    private fun verifyPresetName(input: String): Boolean {
        val nameWithoutExtension = input.removeSuffix(".json")
        val nameWithExtension = "$nameWithoutExtension.json"

        return if (!ConfigUtils.isPathValid(nameWithExtension)) {
            NoSpamMessage.sendMessage("${formatValue(nameWithoutExtension)} is not a valid preset name")
            false
        } else {
            true
        }
    }

    private fun updatePreset(setting: StringSetting, input: String, config: IConfig) {
        if (!verifyPresetName(input)) return

        val nameWithoutExtension = input.removeSuffix(".json")
        val prev = setting.value

        try {
            ConfigManager.save(config)
            setting.value = nameWithoutExtension
            ConfigManager.save(GenericConfig)
            ConfigManager.load(config)

            NoSpamMessage.sendMessage("Preset set to ${formatValue(nameWithoutExtension)}!")
        } catch (e: IOException) {
            NoSpamMessage.sendMessage("Couldn't set preset: ${e.message}")
            TrollHackMod.logger.warn("Couldn't set path!", e)

            setting.value = prev
            ConfigManager.save(GenericConfig)
        }
    }

    init {
        with({ prev: String, input: String ->
            if (verifyPresetName(input)) {
                input
            } else {
                if (verifyPresetName(prev)) {
                    prev
                } else {
                    defaultPreset
                }
            }
        }) {
            guiPresetSetting.consumers.add(this)
            modulePresetSetting.consumers.add(this)
        }
    }

    @Suppress("UNUSED")
    enum class ConfigType(
        override val displayName: CharSequence,
        override val config: AbstractConfig<out Any>,
        override val setting: StringSetting
    ) : DisplayEnum, IConfigType {
        GUI("GUI", GuiConfig, guiPresetSetting),
        MODULES("Modules", ModuleConfig, modulePresetSetting);

        override val serverPresets get() = getJsons(config.filePath) { it.name.startsWith("server-") }

        override val allPresets get() = getJsons(config.filePath) { true }

        private companion object {
            fun getJsons(path: String, filter: (File) -> Boolean): Set<String> {
                val dir = File(path)
                if (!dir.exists() || !dir.isDirectory) return emptySet()

                val files = dir.listFiles() ?: return emptySet()
                val jsonFiles = files.filter {
                    it.isFile && it.extension == "json" && it.length() > 8L && filter(it)
                }

                return LinkedHashSet<String>().apply {
                    jsonFiles.forEach {
                        add(it.nameWithoutExtension)
                    }
                }
            }
        }
    }

    interface IConfigType : DisplayEnum {
        val config: AbstractConfig<out Any>
        val setting: StringSetting
        val serverPresets: Set<String>
        val allPresets: Set<String>

        fun reload() {
            DefaultScope.launch(Dispatchers.IO) {
                var loaded = ConfigManager.load(GenericConfig)
                loaded = ConfigManager.load(config) || loaded

                if (loaded) NoSpamMessage.sendMessage("${formatValue(config.name)} config reloaded!")
                else NoSpamMessage.sendError("Failed to load ${formatValue(config.name)} config!")
            }
        }

        fun save() {
            DefaultScope.launch(Dispatchers.IO) {
                var saved = ConfigManager.save(GenericConfig)
                saved = ConfigManager.save(config) || saved

                if (saved) NoSpamMessage.sendMessage("${formatValue(config.name)} config saved!")
                else NoSpamMessage.sendError("Failed to load ${formatValue(config.name)} config!")
            }
        }

        fun setPreset(name: String) {
            DefaultScope.launch(Dispatchers.IO) {
                updatePreset(setting, name, config)
            }
        }

        fun copyPreset(name: String) {
            DefaultScope.launch(Dispatchers.IO) {
                if (name == setting.value) {
                    NoSpamMessage.sendError("Destination preset name ${formatValue(name)} is same as current preset")
                }

                ConfigManager.save(config)

                try {
                    val fileFrom = File("${config.filePath}/${setting.value}.json")
                    val fileTo = File("${config.filePath}/${name}.json")

                    fileFrom.copyTo(fileTo, true)
                } catch (e: Exception) {
                    NoSpamMessage.sendError("Failed to copy preset, ${e.message}")
                    TrollHackMod.logger.error("Failed to copy preset", e)
                }
            }
        }

        fun deletePreset(name: String) {
            DefaultScope.launch(Dispatchers.IO) {
                if (!allPresets.contains(name)) {
                    NoSpamMessage.sendMessage(
                        "${formatValue(name)} is not a valid preset for ${
                            formatValue(
                                displayName
                            )
                        } config"
                    )
                    return@launch
                }

                try {
                    val file = File("${config.filePath}/${name}.json")
                    val fileBak = File("${config.filePath}/${name}.bak")

                    file.delete()
                    fileBak.delete()

                    NoSpamMessage.sendMessage("Deleted preset $name for ${formatValue(displayName)} config")
                } catch (e: Exception) {
                    NoSpamMessage.sendError("Failed to delete preset, ${e.message}")
                    TrollHackMod.logger.error("Failed to delete preset", e)
                }
            }
        }

        fun printCurrentPreset() {
            val path = Paths.get("${config.filePath}/${setting.value}.json").toAbsolutePath()
            NoSpamMessage.sendMessage("Path to config: ${formatValue(path)}")
        }

        fun printAllPresets() {
            if (allPresets.isEmpty()) {
                NoSpamMessage.sendMessage("No preset for ${formatValue(displayName)} config!")
            } else {
                val stringBuilder = StringBuilder()
                stringBuilder.appendLine("List of presets: ${formatValue(allPresets.size)}")

                allPresets.forEach {
                    val path = Paths.get("${config.filePath}/${it}.json").toAbsolutePath()
                    stringBuilder.appendLine(formatValue(path))
                }

                NoSpamMessage.sendMessage(stringBuilder.toString())
            }
        }

        fun newServerPreset(ip: String) {
            if (!serverPresetDisabledMessage()) return

            setPreset(convertIpToPresetName(ip))
        }

        fun setServerPreset(ip: String) {
            if (!serverPresetDisabledMessage()) return

            val presetName = convertIpToPresetName(ip)

            if (serverPresets.contains(presetName)) {
                NoSpamMessage.sendMessage(
                    "Changing preset to ${formatValue(presetName)} for ${
                        formatValue(
                            displayName
                        )
                    } config"
                )
                setPreset(presetName)
            } else {
                NoSpamMessage.sendMessage(
                    "No server preset found for ${formatValue(displayName)} config, using ${
                        formatValue(
                            defaultPreset
                        )
                    } preset..."
                )
                setPreset(defaultPreset)
            }
        }

        fun deleteServerPreset(ip: String) {
            deletePreset(convertIpToPresetName(ip))
        }

        fun printAllServerPreset() {
            if (!serverPresetDisabledMessage()) return

            if (serverPresets.isEmpty()) {
                NoSpamMessage.sendMessage("No server preset for ${formatValue(displayName)} config!")
            } else {
                val stringBuilder = StringBuilder()
                stringBuilder.appendLine(
                    "List of server presets for ${formatValue(displayName)} config: ${
                        formatValue(
                            serverPresets.size
                        )
                    }"
                )

                serverPresets.forEach {
                    val path = Paths.get("${config.filePath}/${it}.json").toAbsolutePath()
                    stringBuilder.appendLine(formatValue(path))
                }

                NoSpamMessage.sendMessage(stringBuilder.toString())
            }
        }

        private fun convertIpToPresetName(ip: String) = "server-" +
            ip.replace('.', '_').replace(':', '_')

        private fun serverPresetDisabledMessage() = if (!serverPreset) {
            NoSpamMessage.sendMessage("Server preset is not enabled, enable it in Configurations in ClickGUI")
            false
        } else {
            true
        }
    }
}