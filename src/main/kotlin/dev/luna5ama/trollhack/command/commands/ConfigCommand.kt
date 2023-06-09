package dev.luna5ama.trollhack.command.commands

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.command.execute.IExecuteEvent
import dev.luna5ama.trollhack.event.SafeExecuteEvent
import dev.luna5ama.trollhack.module.modules.client.Configurations
import dev.luna5ama.trollhack.util.ConfigUtils
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.text.formatValue
import dev.luna5ama.trollhack.util.threads.DefaultScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ConfigCommand : ClientCommand(
    name = "config",
    alias = arrayOf("cfg"),
    description = "Change config saving path or manually save and reload your config"
) {
    private val confirmTimer = TickTimer(TimeUnit.SECONDS)
    private var lastArgs = emptyArray<String>()

    init {
        literal("all") {
            literal("reload") {
                execute("Reload all configs") {
                    DefaultScope.launch(Dispatchers.IO) {
                        val loaded = ConfigUtils.loadAll()
                        if (loaded) NoSpamMessage.sendMessage("All configurations reloaded!")
                        else NoSpamMessage.sendError("Failed to load config!")
                    }
                }
            }

            literal("save") {
                execute("Save all configs") {
                    DefaultScope.launch(Dispatchers.IO) {
                        val saved = ConfigUtils.saveAll()
                        if (saved) NoSpamMessage.sendMessage("All configurations saved!")
                        else NoSpamMessage.sendError("Failed to load config!")
                    }
                }
            }
        }

        enum<Configurations.ConfigType>("config type") { configTypeArg ->
            literal("reload") {
                execute("Reload a config") {
                    configTypeArg.value.reload()
                }
            }

            literal("save") {
                execute("Save a config") {
                    configTypeArg.value.save()
                }
            }

            literal("set") {
                string("name") { nameArg ->
                    execute("Change preset") {
                        configTypeArg.value.setPreset(nameArg.value)
                    }
                }
            }

            literal("copy", "ctrl+c", "ctrtc") {
                string("name") { nameArg ->
                    execute("Copy current preset to specific preset") {
                        val name = nameArg.value
                        if (!confirm()) return@execute

                        configTypeArg.value.copyPreset(name)
                    }
                }
            }

            literal("delete", "del", "remove") {
                string("name") { nameArg ->
                    execute("Delete specific preset") {
                        val name = nameArg.value
                        if (!confirm()) return@execute

                        configTypeArg.value.deletePreset(name)
                    }
                }
            }

            literal("list") {
                execute("List all available presets") {
                    configTypeArg.value.printAllPresets()
                }
            }

            literal("server") {
                literal("create", "new", "add") {
                    executeSafe("Create a new server preset") {
                        val ip = getIpOrNull() ?: return@executeSafe

                        configTypeArg.value.newServerPreset(ip)
                    }
                }

                literal("delete", "del", "remove") {
                    executeSafe("Delete the current server preset") {
                        val ip = getIpOrNull() ?: return@executeSafe
                        val configType = configTypeArg.value

                        if (!configType.serverPresets.contains(ip)) {
                            NoSpamMessage.sendMessage("This server doesn't have a preset in config ${configType.displayName}")
                            return@executeSafe
                        }

                        if (!confirm()) return@executeSafe

                        configType.deleteServerPreset(ip)
                    }
                }

                literal("list") {
                    execute("List all available server presets") {
                        configTypeArg.value.printAllServerPreset()
                    }
                }
            }

            execute("Print current preset name") {
                configTypeArg.value.printCurrentPreset()
            }
        }
    }

    private fun SafeExecuteEvent.getIpOrNull(): String? {
        val ip = mc.currentServerData?.serverIP

        return if (ip == null || mc.isIntegratedServerRunning) {
            NoSpamMessage.sendWarning("You are not in a server!")
            null
        } else {
            ip
        }
    }

    private fun IExecuteEvent.confirm(): Boolean {
        return if (!args.contentEquals(lastArgs) || confirmTimer.tick(8L)) {
            NoSpamMessage.sendWarning(
                "This can't be undone, run " +
                    "${formatValue("${prefix}${args.joinToString(" ")}")} to confirm!"
            )

            confirmTimer.reset()
            lastArgs = args
            false
        } else {
            lastArgs = emptyArray()
            true
        }
    }
}
