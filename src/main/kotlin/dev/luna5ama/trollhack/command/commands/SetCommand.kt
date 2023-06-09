package dev.luna5ama.trollhack.command.commands

import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.gui.GuiManager
import dev.luna5ama.trollhack.gui.hudgui.AbstractHudElement
import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.module.ModuleManager
import dev.luna5ama.trollhack.setting.settings.AbstractSetting
import dev.luna5ama.trollhack.setting.settings.impl.primitive.BooleanSetting
import dev.luna5ama.trollhack.setting.settings.impl.primitive.EnumSetting
import dev.luna5ama.trollhack.util.delegate.AsyncCachedValue
import dev.luna5ama.trollhack.util.extension.remove
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.text.format
import dev.luna5ama.trollhack.util.text.formatValue
import net.minecraft.util.text.TextFormatting
import java.util.*

object SetCommand : ClientCommand(
    name = "set",
    alias = arrayOf("setting", "settings"),
    description = "Change the setting of a certain module."
) {
    private val moduleSettingMap: Map<AbstractModule, Map<String, AbstractSetting<*>>> by AsyncCachedValue(
        5L,
        TimeUnit.SECONDS
    ) {
        ModuleManager.modules
            .associateWith { module ->
                module.fullSettingList.associateBy {
                    it.nameAsString.formatSetting()
                }
            }
    }

    private val hudElementSettingMap: Map<AbstractHudElement, Map<String, AbstractSetting<*>>> by AsyncCachedValue(
        5L,
        TimeUnit.SECONDS
    ) {
        GuiManager.hudElements
            .associateWith { hudElements ->
                hudElements.settingList.associateBy {
                    it.nameAsString.formatSetting()
                }
            }
    }

    init {
        hudElement("hud element") { hudElementArg ->
            string("setting") { settingArg ->
                literal("toggle") {
                    execute {
                        val hudElement = hudElementArg.value
                        val settingName = settingArg.value
                        val setting = getSetting(hudElement, settingName)

                        toggleSetting(hudElement.nameAsString, settingName, setting)
                    }
                }

                greedy("value") { valueArg ->
                    execute("Set the value of a hud element's setting") {
                        val hudElement = hudElementArg.value
                        val settingName = settingArg.value
                        val setting = getSetting(hudElement, settingName)

                        setSetting(hudElement.nameAsString, settingName, setting, valueArg.value)
                    }
                }

                execute("Show the value of a setting") {
                    val hudElement = hudElementArg.value
                    val settingName = settingArg.value
                    val setting = getSetting(hudElement, settingName)

                    printSetting(hudElement.nameAsString, settingName, setting)
                }
            }

            execute("List settings for a hud element") {
                listSetting(hudElementArg.value.nameAsString, hudElementArg.value.settingList)
            }
        }

        module("module") { moduleArg ->
            string("setting") { settingArg ->
                literal("toggle") {
                    execute {
                        val module = moduleArg.value
                        val settingName = settingArg.value
                        val setting = getSetting(module, settingName)

                        toggleSetting(module.nameAsString, settingName, setting)
                    }
                }

                greedy("value") { valueArg ->
                    execute("Set the value of a module's setting") {
                        val module = moduleArg.value
                        val settingName = settingArg.value
                        val setting = getSetting(module, settingName)

                        setSetting(module.nameAsString, settingName, setting, valueArg.value)
                    }
                }

                execute("Show the value of a setting") {
                    val module = moduleArg.value
                    val settingName = settingArg.value
                    val setting = getSetting(module, settingName)

                    printSetting(module.nameAsString, settingName, setting)
                }
            }

            execute("List settings for a module") {
                listSetting(moduleArg.value.nameAsString, moduleArg.value.fullSettingList)
            }
        }
    }

    private fun String.formatSetting(lowerCase: Boolean = true) =
        this.remove(' ', '_')
            .run {
                if (lowerCase) this.lowercase(Locale.ROOT) else this
            }

    private fun getSetting(module: AbstractModule, settingName: String) =
        moduleSettingMap[module]?.get(settingName.formatSetting())

    private fun getSetting(module: AbstractHudElement, settingName: String) =
        hudElementSettingMap[module]?.get(settingName.formatSetting())

    private fun toggleSetting(name: String, settingName: String, setting: AbstractSetting<*>?) {
        if (setting == null) {
            sendUnknownSettingMessage(name, settingName)
            return
        }

        when (setting) {
            is BooleanSetting -> {
                setting.value = !setting.value
            }

            is EnumSetting -> {
                setting.nextValue()
            }

            else -> {
                NoSpamMessage.sendMessage("Unable to toggle value for ${formatValue(setting.name)}")
            }
        }

        NoSpamMessage.sendMessage("Set ${formatValue(setting.name)} to ${formatValue(setting.value)}.")
    }

    private fun setSetting(name: String, settingName: String, setting: AbstractSetting<*>?, value: String) {
        if (setting == null) {
            sendUnknownSettingMessage(name, settingName)
            return
        }

        try {
            setting.setValue(value)
            NoSpamMessage.sendMessage("Set ${formatValue(setting.name)} to ${formatValue(value)}.")
        } catch (e: Exception) {
            NoSpamMessage.sendMessage("Unable to set value! ${TextFormatting.RED format e.message.toString()}")
            TrollHackMod.logger.info("Unable to set value!", e)
        }
    }

    private fun printSetting(name: String, settingName: String, setting: AbstractSetting<*>?) {
        if (setting == null) {
            sendUnknownSettingMessage(name, settingName)
            return
        }

        NoSpamMessage.sendMessage(
            "${formatValue(settingName)} is a " +
                "${formatValue(setting.valueClass.simpleName)}. " +
                "Its current value is ${formatValue(setting)}"
        )
    }

    private fun listSetting(name: String, settingList: List<AbstractSetting<*>>) {
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine("List of settings for ${formatValue(name)} ${formatValue(settingList.size)}")

        settingList.joinTo(stringBuilder, "\n") {
            "    ${it.nameAsString.formatSetting(false)} ${TextFormatting.GRAY format it.value}"
        }

        NoSpamMessage.sendMessage(stringBuilder.toString())
    }

    private fun sendUnknownSettingMessage(settingName: String, name: String) {
        NoSpamMessage.sendMessage("Unknown setting ${formatValue(settingName)} in ${formatValue(name)}!")
    }
}