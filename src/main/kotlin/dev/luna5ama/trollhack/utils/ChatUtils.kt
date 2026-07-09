package dev.luna5ama.trollhack.utils

import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object ChatUtils {
    private const val SECTION_SIGN = '\u00A7'

    val BLACK = SECTION_SIGN + "0"
    val DARK_BLUE = SECTION_SIGN + "1"
    val DARK_GREEN = SECTION_SIGN + "2"
    val DARK_AQUA = SECTION_SIGN + "3"
    val DARK_RED = SECTION_SIGN + "4"
    val DARK_PURPLE = SECTION_SIGN + "5"
    val GOLD = SECTION_SIGN + "6"
    val GRAY = SECTION_SIGN + "7"
    val DARK_GRAY = SECTION_SIGN + "8"
    val BLUE = SECTION_SIGN + "9"
    val GREEN = SECTION_SIGN + "a"
    val AQUA = SECTION_SIGN + "b"
    val RED = SECTION_SIGN + "c"
    val LIGHT_PURPLE = SECTION_SIGN + "d"
    val YELLOW = SECTION_SIGN + "e"
    val WHITE = SECTION_SIGN + "f"
    val OBFUSCATED = SECTION_SIGN + "k"
    val BOLD = SECTION_SIGN + "l"
    val STRIKE_THROUGH = SECTION_SIGN + "m"
    val UNDER_LINE = SECTION_SIGN + "n"
    val ITALIC = SECTION_SIGN + "o"
    val RESET = SECTION_SIGN + "r"

    private val messagePrefix: Component
        get() = Component.literal("[")
            .withStyle(ChatFormatting.WHITE)
            .append(Component.literal(TrollHackMod.NAME).withStyle(ChatFormatting.AQUA))
            .append(Component.literal("]").withStyle(ChatFormatting.WHITE))

    fun sendMessage(message: Any?) {
        sendMessage(message.toString())
    }

    fun sendMessage(message: String) {
        runSafe {
            player.displayClientMessage(
                messagePrefix.copy()
                    .append(Component.literal(" "))
                    .append(Component.literal(translateAlternateColorCodes(message)).withStyle(ChatFormatting.RESET)),
                false
            )
        }
    }

    fun sendRawMessage(message: String) {
        runSafe {
            player.displayClientMessage(Component.literal(message), false)
        }
    }

    private fun translateAlternateColorCodes(textToTranslate: String): String {
        val b = textToTranslate.toCharArray()
        for (i in 0..<b.size - 1) {
            if (b[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(b[i + 1]) > -1) {
                b[i] = SECTION_SIGN
                b[i + 1] = b[i + 1].lowercaseChar()
            }
        }
        return b.concatToString()
    }
}
