package cum.xiaro.trollhack.module.modules.client

import cum.xiaro.trollhack.util.interfaces.DisplayEnum
import cum.xiaro.trollhack.TrollHackMod
import cum.xiaro.trollhack.event.events.ConnectionEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.gui.AbstractTrollGui
import cum.xiaro.trollhack.module.AbstractModule
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.setting.ConfigManager
import cum.xiaro.trollhack.setting.GenericConfig
import cum.xiaro.trollhack.setting.GuiConfig
import cum.xiaro.trollhack.setting.ModuleConfig
import cum.xiaro.trollhack.setting.configs.AbstractConfig
import cum.xiaro.trollhack.setting.configs.IConfig
import cum.xiaro.trollhack.setting.settings.impl.primitive.StringSetting
import cum.xiaro.trollhack.util.ConfigUtils
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.TimeUnit
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.text.MessageSendUtils
import cum.xiaro.trollhack.util.text.NoSpamMessage
import cum.xiaro.trollhack.util.text.formatValue
import cum.xiaro.trollhack.util.threads.BackgroundScope
import cum.xiaro.trollhack.util.threads.defaultScope
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
    private val savingInterval by setting("Interval", 10, 1..30, 1, autoSaving0.atTrue(), description = "Frequency of auto saving in minutes")
    val serverPreset by setting("Server Preset", false)
    private val guiPresetSetting = setting("Gui Preset", defaultPreset)
    private val modulePresetSetting = setting("Module Preset", defaultPreset)

    val guiPreset by guiPresetSetting
    val modulePreset by modulePresetSetting

    private val timer = TickTimer(TimeUnit.MINUTES)
    private var connected = false

    init {
        BackgroundScope.launchLooping("Config Auto Saving", 60000L) {
            if (autoSaving && mc.currentScreen !is AbstractTrollGui<*, *> && timer.tickAndReset(savingInterval.toLong())) {
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
            MessageSendUtils.sendNoSpamChatMessage("${formatValue(nameWithoutExtension)} is not a valid preset name")
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

            MessageSendUtils.sendNoSpamChatMessage("Preset set to ${formatValue(nameWithoutExtension)}!")
        } catch (e: IOException) {
            MessageSendUtils.sendNoSpamChatMessage("Couldn't set preset: ${e.message}")
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
            defaultScope.launch(Dispatchers.IO) {
                var loaded = ConfigManager.load(GenericConfig)
                loaded = ConfigManager.load(config) || loaded

                if (loaded) MessageSendUtils.sendNoSpamChatMessage("${formatValue(config.name)} config reloaded!")
                else MessageSendUtils.sendNoSpamErrorMessage("Failed to load ${formatValue(config.name)} config!")
            }
        }

        fun save() {
            defaultScope.launch(Dispatchers.IO) {
                var saved = ConfigManager.save(GenericConfig)
                saved = ConfigManager.save(config) || saved

                if (saved) MessageSendUtils.sendNoSpamChatMessage("${formatValue(config.name)} config saved!")
                else MessageSendUtils.sendNoSpamErrorMessage("Failed to load ${formatValue(config.name)} config!")
            }
        }

        fun setPreset(name: String) {
            defaultScope.launch(Dispatchers.IO) {
                updatePreset(setting, name, config)
            }
        }

        fun copyPreset(name: String) {
            defaultScope.launch(Dispatchers.IO) {
                if (name == setting.value) {
                    MessageSendUtils.sendNoSpamErrorMessage("Destination preset name ${formatValue(name)} is same as current preset")
                }

                ConfigManager.save(config)

                try {
                    val fileFrom = File("${config.filePath}/${setting.value}.json")
                    val fileTo = File("${config.filePath}/${name}.json")

                    fileFrom.copyTo(fileTo, true)
                } catch (e: Exception) {
                    MessageSendUtils.sendNoSpamErrorMessage("Failed to copy preset, ${e.message}")
                    TrollHackMod.logger.error("Failed to copy preset", e)
                }
            }
        }

        fun deletePreset(name: String) {
            defaultScope.launch(Dispatchers.IO) {
                if (!allPresets.contains(name)) {
                    MessageSendUtils.sendNoSpamChatMessage("${formatValue(name)} is not a valid preset for ${formatValue(displayName)} config")
                    return@launch
                }

                try {
                    val file = File("${config.filePath}/${name}.json")
                    val fileBak = File("${config.filePath}/${name}.bak")

                    file.delete()
                    fileBak.delete()

                    MessageSendUtils.sendNoSpamChatMessage("Deleted preset $name for ${formatValue(displayName)} config")
                } catch (e: Exception) {
                    MessageSendUtils.sendNoSpamErrorMessage("Failed to delete preset, ${e.message}")
                    TrollHackMod.logger.error("Failed to delete preset", e)
                }
            }
        }

        fun printCurrentPreset() {
            val path = Paths.get("${config.filePath}/${setting.value}.json").toAbsolutePath()
            MessageSendUtils.sendNoSpamChatMessage("Path to config: ${formatValue(path)}")
        }

        fun printAllPresets() {
            if (allPresets.isEmpty()) {
                MessageSendUtils.sendNoSpamChatMessage("No preset for ${formatValue(displayName)} config!")
            } else {
                val stringBuilder = StringBuilder()
                stringBuilder.appendLine("List of presets: ${formatValue(allPresets.size)}")

                allPresets.forEach {
                    val path = Paths.get("${config.filePath}/${it}.json").toAbsolutePath()
                    stringBuilder.appendLine(formatValue(path))
                }

                MessageSendUtils.sendNoSpamChatMessage(stringBuilder.toString())
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
                MessageSendUtils.sendNoSpamChatMessage("Changing preset to ${formatValue(presetName)} for ${formatValue(displayName)} config")
                setPreset(presetName)
            } else {
                MessageSendUtils.sendNoSpamChatMessage("No server preset found for ${formatValue(displayName)} config, using ${formatValue(defaultPreset)} preset...")
                setPreset(defaultPreset)
            }
        }

        fun deleteServerPreset(ip: String) {
            deletePreset(convertIpToPresetName(ip))
        }

        fun printAllServerPreset() {
            if (!serverPresetDisabledMessage()) return

            if (serverPresets.isEmpty()) {
                MessageSendUtils.sendNoSpamChatMessage("No server preset for ${formatValue(displayName)} config!")
            } else {
                val stringBuilder = StringBuilder()
                stringBuilder.appendLine("List of server presets for ${formatValue(displayName)} config: ${formatValue(serverPresets.size)}")

                serverPresets.forEach {
                    val path = Paths.get("${config.filePath}/${it}.json").toAbsolutePath()
                    stringBuilder.appendLine(formatValue(path))
                }

                MessageSendUtils.sendNoSpamChatMessage(stringBuilder.toString())
            }
        }

        private fun convertIpToPresetName(ip: String) = "server-" +
            ip.replace('.', '_').replace(':', '_')

        private fun serverPresetDisabledMessage() = if (!serverPreset) {
            MessageSendUtils.sendNoSpamChatMessage("Server preset is not enabled, enable it in Configurations in ClickGUI")
            false
        } else {
            true
        }
    }
}