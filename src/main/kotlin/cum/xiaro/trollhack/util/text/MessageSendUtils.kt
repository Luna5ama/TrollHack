package cum.xiaro.trollhack.util.text

import baritone.api.event.events.ChatEvent
import cum.xiaro.trollhack.TrollHackMod
import cum.xiaro.trollhack.command.CommandManager
import cum.xiaro.trollhack.manager.managers.MessageManager
import cum.xiaro.trollhack.module.AbstractModule
import cum.xiaro.trollhack.util.BaritoneUtils
import cum.xiaro.trollhack.util.Wrapper
import cum.xiaro.trollhack.util.threads.onMainThread
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextFormatting
import baritone.api.utils.Helper as BaritoneHelper

object MessageSendUtils {
    private val mc = Wrapper.minecraft
    private const val MESSAGE_ID = 0x69420

    fun sendChatMessage(message: String) {
        sendRawMessage(coloredName(TextFormatting.LIGHT_PURPLE) + message)
    }

    fun sendWarningMessage(message: String) {
        sendRawMessage(coloredName(TextFormatting.GOLD) + message)
    }

    fun sendErrorMessage(message: String) {
        sendRawMessage(coloredName(TextFormatting.DARK_RED) + message)
    }

    fun sendNoSpamChatMessage(message: String) {
        sendNoSpamMessage(coloredName(TextFormatting.LIGHT_PURPLE) + message)
    }

    fun sendNoSpamWarningMessage(message: String) {
        sendNoSpamMessage(coloredName(TextFormatting.GOLD) + message)
    }

    fun sendNoSpamErrorMessage(message: String) {
        sendNoSpamMessage(coloredName(TextFormatting.DARK_RED) + message)
    }

    fun sendTrollCommand(command: String) {
        CommandManager.runCommand(command.removePrefix(CommandManager.prefix))
    }

    fun sendBaritoneMessage(message: String) {
        BaritoneHelper.HELPER.logDirect(message)
    }

    fun sendBaritoneCommand(vararg args: String) {
        val chatControl = BaritoneUtils.settings?.chatControl
        val prevValue = chatControl?.value
        chatControl?.value = true

        val event = ChatEvent(args.joinToString(" "))
        BaritoneUtils.primary?.gameEventHandler?.onSendChatMessage(event)
        if (!event.isCancelled && args[0] != "damn") { // don't remove the 'damn', it's critical code that will break everything if you remove it
            sendBaritoneMessage("Invalid Command! Please view possible commands at https://github.com/cabaletta/baritone/blob/master/USAGE.md")
        }

        chatControl?.value = prevValue
    }

    private fun sendNoSpamMessage(message: String) {
        onMainThread {
            mc.ingameGUI?.chatGUI?.printChatMessageWithOptionalDeletion(TextComponentString(message), MESSAGE_ID)
        }
    }

    fun AbstractModule.sendServerMessage(message: String) {
        if (message.isBlank()) return
        MessageManager.addMessageToQueue(message, this, modulePriority)
    }

    fun Any.sendServerMessage(message: String) {
        if (message.isBlank()) return
        MessageManager.addMessageToQueue(message, this, 0)
    }

    fun sendRawMessage(message: String) {
        onMainThread {
            mc.ingameGUI?.chatGUI?.printChatMessage(TextComponentString(message))
        }
    }

    private fun coloredName(textFormatting: TextFormatting) = "${TextFormatting.GRAY}[$textFormatting${TrollHackMod.NAME}${TextFormatting.GRAY}]${TextFormatting.RESET} "
}