package dev.luna5ama.trollhack.module.modules.chat

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.MovementUtils.isMoving
import dev.luna5ama.trollhack.util.text.MessageDetection
import dev.luna5ama.trollhack.util.text.MessageSendUtils
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.DefaultScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

internal object LoginMessage : Module(
    name = "Login Message",
    description = "Sends a given message(s) to public chat on login.",
    category = Category.CHAT,
    visible = false,
    modulePriority = 150
) {
    private val sendAfterMoving by setting(
        "Send After Moving",
        false,
        description = "Wait until you have moved after logging in"
    )

    private val file = File("${TrollHackMod.DIRECTORY}/loginmsg.txt")
    private val loginMessages = CopyOnWriteArrayList<String>()
    private var sent = false
    private var moved = false

    init {
        onEnable {
            if (file.exists()) {
                DefaultScope.launch(Dispatchers.IO) {
                    try {
                        file.forEachLine {
                            if (it.isNotBlank()) loginMessages.add(it.trim())
                        }
                        NoSpamMessage.sendMessage("$chatName Loaded ${loginMessages.size} login messages!")
                    } catch (e: Exception) {
                        NoSpamMessage.sendError("$chatName Failed loading login messages, $e")
                        disable()
                    }
                }
            } else {
                file.createNewFile()
                NoSpamMessage.sendError(
                    "$chatName Login Messages file not found!" +
                        ", please add them in the §7loginmsg.txt§f under the §7.minecraft/trollhack§f directory."
                )
                disable()
            }
        }

        onDisable {
            loginMessages.clear()
        }

        listener<ConnectionEvent.Disconnect> {
            sent = false
            moved = false
        }

        safeListener<TickEvent.Post> {
            if (!sent && (!sendAfterMoving || moved)) {
                for (message in loginMessages) {
                    if (MessageDetection.Command.TROLL_HACK detect message) {
                        MessageSendUtils.sendTrollCommand(message)
                    } else {
                        LoginMessage.sendServerMessage(message)
                    }
                }

                sent = true
            }

            if (!moved) moved = player.isMoving
        }
    }
}