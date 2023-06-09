package dev.luna5ama.trollhack.module.modules.chat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.extension.synchronized
import dev.luna5ama.trollhack.util.text.MessageDetection
import dev.luna5ama.trollhack.util.text.MessageSendUtils
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.DefaultScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL
import kotlin.random.Random

internal object Spammer : Module(
    name = "Spammer",
    description = "Spams text from a file on a set delay into the chat",
    category = Category.CHAT,
    modulePriority = 100
) {
    private val modeSetting = setting("Order", Mode.RANDOM_ORDER)
    private val delay = setting("Delay", 10, 1..180, 1, description = "Delay between messages, in seconds")
    private val loadRemote = setting("Load From URL", false)
    private val remoteURL = setting("Remote URL", "Unchanged", loadRemote.atTrue())

    private val file = File("${TrollHackMod.DIRECTORY}/spammer.txt")
    private val spammer = ArrayList<String>().synchronized()
    private val timer = TickTimer(TimeUnit.SECONDS)
    private var currentLine = 0

    private enum class Mode {
        IN_ORDER, RANDOM_ORDER
    }

    private val urlValue
        get() = if (remoteURL.value != "Unchanged") {
            remoteURL.value
        } else {
            NoSpamMessage.sendError("Change the RemoteURL setting in the ClickGUI!")
            disable()
            null
        }

    init {
        onEnable {
            spammer.clear()

            if (loadRemote.value) {
                val url = urlValue ?: return@onEnable

                DefaultScope.launch(Dispatchers.IO) {
                    try {
                        val text = URL(url).readText()
                        spammer.addAll(text.split("\n"))

                        NoSpamMessage.sendMessage("$chatName Loaded remote spammer messages!")
                    } catch (e: Exception) {
                        NoSpamMessage.sendError("$chatName Failed loading remote spammer, $e")
                        disable()
                    }
                }

            } else {
                DefaultScope.launch(Dispatchers.IO) {
                    if (file.exists()) {
                        try {
                            file.forEachLine { if (it.isNotBlank()) spammer.add(it.trim()) }
                            NoSpamMessage.sendMessage("$chatName Loaded spammer messages!")
                        } catch (e: Exception) {
                            NoSpamMessage.sendError("$chatName Failed loading spammer, $e")
                            disable()
                        }
                    } else {
                        file.createNewFile()
                        NoSpamMessage.sendError(
                            "$chatName Spammer file is empty!" +
                                ", please add them in the §7spammer.txt§f under the §7.minecraft/trollhack§f directory."
                        )
                        disable()
                    }
                }
            }
        }

        safeListener<TickEvent.Post> {
            if (spammer.isEmpty() || !timer.tickAndReset(delay.value)) return@safeListener

            val message = if (modeSetting.value == Mode.IN_ORDER) getOrdered() else getRandom()
            if (MessageDetection.Command.TROLL_HACK detect message) {
                MessageSendUtils.sendTrollCommand(message)
            } else {
                Spammer.sendServerMessage(message)
            }
        }
    }

    private fun getOrdered(): String {
        currentLine %= spammer.size
        return spammer[currentLine++]
    }

    private fun getRandom(): String {
        val prevLine = currentLine
        // Avoids sending the same message
        while (spammer.size != 1 && currentLine == prevLine) {
            currentLine = Random.nextInt(spammer.size)
        }
        return spammer[currentLine]
    }
}