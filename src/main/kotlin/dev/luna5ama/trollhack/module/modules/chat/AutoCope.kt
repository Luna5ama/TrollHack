package dev.luna5ama.trollhack.module.modules.chat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.safeConcurrentListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.text.MessageDetection
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.text.unformatted
import net.minecraft.init.Items
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.network.play.server.SPacketUpdateHealth
import net.minecraft.util.EnumHand
import java.io.File

internal object AutoCope : Module(
    name = "Auto Cope",
    description = "Automatically sends excuses when you die",
    category = Category.CHAT,
    modulePriority = 500
) {
    private const val NAME = "\$NAME"

    private val mode by setting("Mode", Mode.INTERNAL)
    private val copeReply by setting("Cope Reply", "cope $NAME")

    private enum class Mode {
        INTERNAL, EXTERNAL
    }

    private const val CLIENT_NAME = "%CLIENT%"
    private val defaultExcuses = arrayOf(
        "Sorry, im using $CLIENT_NAME client",
        "My ping is so bad",
        "I was changing my config :(",
        "Why did my AutoTotem break",
        "I was desynced",
        "Stupid hackers killed me",
        "Wow, so many try hards",
        "Lagggg",
        "I wasn't trying",
        "I'm not using $CLIENT_NAME client",
        "Thers to much lag",
        "My dog ate my pc",
        "Sorry, $CLIENT_NAME Client is really bad",
        "I was lagging",
        "He was cheating!",
        "Your hacking!",
        "Lol imagine actully trying",
        "I didn't move my mouse",
        "I was playing on easy mode(;",
        "My wifi went down",
        "I'm playing vanila",
        "My optifine didn't work",
        "The CPU cheated!"
    )

    private val file = File("${TrollHackMod.DIRECTORY}/excuses.txt")
    private var loadedExcuses = defaultExcuses

    private val clients = arrayOf(
        "Future",
        "Lambda",
        "EarthHack",
        "Salhack",
        "Pyro",
        "Impact"
    )

    private val timer = TickTimer(TimeUnit.SECONDS)

    init {
        safeConcurrentListener<PacketEvent.Receive> { event ->
            when (event.packet) {
                is SPacketUpdateHealth -> {
                    if (loadedExcuses.isEmpty()) return@safeConcurrentListener
                    if (event.packet.health <= 0.0f && !isHoldingTotem && timer.tickAndReset(3L)) {
                        AutoCope.sendServerMessage(getExcuse())
                    }
                }
                is SPacketChat -> {
                    val message = event.packet.chatComponent.unformatted
                    val playerName = MessageDetection.Message.OTHER.playerName(message) ?: return@safeConcurrentListener
                    val messageWithoutName = message.replace(playerName, "")
                    if (copeReplyList.any { it.setting.value && it.run { detect(messageWithoutName) } }) {
                        AutoCope.sendServerMessage(copeReply.replace(NAME, playerName))
                    }
                }
            }
        }

        onEnable {
            loadedExcuses = if (mode == Mode.EXTERNAL) {
                if (file.exists()) {
                    val cacheList = ArrayList<String>()
                    try {
                        file.forEachLine { if (it.isNotBlank()) cacheList.add(it.trim()) }
                        NoSpamMessage.sendMessage("$chatName Loaded spammer messages!")
                    } catch (e: Exception) {
                        TrollHackMod.logger.error("Failed loading excuses", e)
                    }
                    cacheList.toTypedArray()
                } else {
                    file.createNewFile()
                    NoSpamMessage.sendError(
                        "$chatName Excuses file is empty!" +
                            ", please add them in the §7excuses.txt§f under the §7.minecraft/trollhack§f directory."
                    )
                    defaultExcuses
                }
            } else {
                defaultExcuses
            }
        }
    }

    private val SafeClientEvent.isHoldingTotem: Boolean
        get() = EnumHand.values().any { player.getHeldItem(it).item == Items.TOTEM_OF_UNDYING }

    private fun getExcuse() = loadedExcuses.random().replace(CLIENT_NAME, clients.random())

    private class CopeReply(settingName: String, val detect: SafeClientEvent.(String) -> Boolean) {
        val setting = setting(settingName, false)
    }

    private val copeReplyList = listOf(
        CopeReply("Face Place Cope") {
            (it.contains("faceplace", true) || it.contains("face place", true)) && targeting(it)
        },
        CopeReply("Robot Cope") {
            (it.contains("ai", true) || it.contains("robot", true)) && targeting(it)
        },
        CopeReply("Bed Cope") {
            it.contains("bed", true) && targeting(it)
        }
    )

    private val targetKeyWords = listOf(
        "fuck",
        "bro is",
        "iq",
        "skill",
        "brain",
        "fag",
        "lel",
        "bad",
        "suck",
        "ass",
        "retard",
        "imagine",
        "lol",
        "shit",
        "gay",
        "aid",
        "dont like",
        "don't like",
        "hate",
        "noob"
    )

    private fun SafeClientEvent.targeting(message: String): Boolean {
        return message.contains(player.name, true)
            || targetKeyWords.any { message.contains(it, true) }
    }
}