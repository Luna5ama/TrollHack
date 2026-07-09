package dev.luna5ama.trollhack.command

import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.command.comands.*
import dev.luna5ama.trollhack.event.api.ClientExecuteContext
import dev.luna5ama.trollhack.event.api.IListening
import dev.luna5ama.trollhack.utils.ChatUtils
import dev.luna5ama.trollhack.utils.Profiler
import dev.luna5ama.trollhack.utils.formatValue
import dev.luna5ama.trollhack.utils.threads.Coroutine
import kotlin.system.measureTimeMillis

object CommandManager : AbstractCommandManager<ClientExecuteContext>() {
    val prefix: String get() = "."

    override fun load(profilerScope: Profiler.ProfilerScope) {
        val time = measureTimeMillis {
            register(SearchCommand)
        }

        TrollHackMod.LOGGER.info("${getCommands().size} commands loaded, took ${time}ms")
    }

    override fun register(builder: CommandBuilder<ClientExecuteContext>): Command<ClientExecuteContext> {
        synchronized(lockObject) {
            (builder as? IListening)?.subscribe()
            return super.register(builder)
        }
    }

    override fun unregister(builder: CommandBuilder<ClientExecuteContext>): Command<ClientExecuteContext>? {
        synchronized(lockObject) {
            (builder as? IListening)?.unsubscribe()
            return super.unregister(builder)
        }
    }

    fun runCommand(string: String) {
        Coroutine.launch {
            val args = tryParseArgument(string) ?: return@launch
            TrollHackMod.LOGGER.debug("Running command with args: [${args.joinToString()}]")

            try {
                try {
                    invoke(ClientExecuteContext(args))
                } catch (e: CommandNotFoundException) {
                    e.printStackTrace()
                    handleCommandNotFoundException(args.first())
                } catch (e: SubCommandNotFoundException) {
                    e.printStackTrace()
                    handleSubCommandNotFoundException(string, args, e)
                }
            } catch (e: Exception) {
                ChatUtils.sendMessage("Error occurred while running command! (${e.message}), check the log for info!")
                TrollHackMod.LOGGER.warn("Error occurred while running command!", e)
            }
        }
    }

    fun tryParseArgument(string: String) = try {
        parseArguments(string)
    } catch (e: IllegalArgumentException) {
        ChatUtils.sendMessage(e.message.toString())
        null
    }

    override suspend fun invoke(event: ClientExecuteContext) {
        val name = event.args.getOrNull(0) ?: throw IllegalArgumentException("Arguments can not be empty!")
        val command = getCommand(name)
        val finalArg = command.finalArgs.firstOrNull { it.checkArgs(event.args) }
            ?: throw SubCommandNotFoundException(event.args, command)

        RenderSystem.queueFencedTask {
            runBlocking {
                finalArg.invoke(event)
            }
        }
    }

    private fun handleCommandNotFoundException(command: String) {
        ChatUtils.sendMessage(
            "Unknown command: ${formatValue("$prefix$command")}. " +
                "Run ${formatValue("${prefix}help")} for a list of commands."
        )
    }

    private suspend fun handleSubCommandNotFoundException(
        string: String,
        args: Array<String>,
        e: SubCommandNotFoundException
    ) {
        val bestCommand = e.command.finalArgs.maxByOrNull { it.countArgs(args) }

        var message = "Invalid syntax: ${formatValue("$prefix$string")}\n"

        if (bestCommand != null) message += "Did you mean ${formatValue("$prefix${bestCommand.printArgHelp()}")}?\n"

        message += "\nRun ${formatValue("${prefix}help ${e.command.name}")} for a list of available arguments."

        ChatUtils.sendMessage(message)
    }

}
