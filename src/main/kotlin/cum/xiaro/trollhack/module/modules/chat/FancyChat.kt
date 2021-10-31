package cum.xiaro.trollhack.module.modules.chat

import cum.xiaro.trollhack.util.math.MathUtils
import cum.xiaro.trollhack.manager.managers.MessageManager.newMessageModifier
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.text.MessageDetection
import kotlin.math.min
import kotlin.random.Random

internal object FancyChat : Module(
    name = "FancyChat",
    category = Category.CHAT,
    description = "Makes messages you send fancy",
    visible = false,
    modulePriority = 100
) {
    private val uwu by setting("uwu", true)
    private val leet by setting("1337", false)
    private val green by setting(">", false)
    private val blue by setting("`", false)
    private val mock by setting("mOcK", false)
    private val randomCase by setting("Random Case", true, ::mock)
    private val commands by setting("Commands", false)
    private val spammer by setting("Spammer", false)

    private val modifier = newMessageModifier(
        filter = {
            (commands || MessageDetection.Command.ANY detectNot it.packet.message)
                && (spammer || it.source !is Spammer)
        },
        modifier = {
            val message = getText(it.packet.message)
            message.substring(0, min(256, message.length))
        }
    )

    init {
        onEnable {
            modifier.enable()
        }

        onDisable {
            modifier.disable()
        }
    }

    private fun getText(s: String): String {
        var string = s

        if (uwu) string = uwuConverter(string)
        if (leet) string = leetConverter(string)
        if (mock) string = mockingConverter(string)
        if (green) string = greenConverter(string)
        if (blue) string = blueConverter(string)

        return string
    }

    private fun greenConverter(input: String): String {
        return "> $input"
    }

    private fun blueConverter(input: String): String {
        return "`$input"
    }

    override fun getHudInfo(): String {
        val returned = StringBuilder()

        if (uwu) returned.append("uwu")
        if (leet) returned.append(" 1337")
        if (mock) returned.append(" mOcK")
        if (green) returned.append(" >")
        if (blue) returned.append(" `")

        return returned.toString()
    }

    private fun leetConverter(input: String): String {
        val message = StringBuilder()

        for (char in input) {
            message.append(leetSwitch(char))
        }

        return message.toString()
    }

    private fun leetSwitch(char: Char): Char {
        return when (char) {
            'a', 'A' -> '4'
            'e', 'E' -> '3'
            'g', 'G' -> '6'
            'l', 'L', 'i', 'I' -> '1'
            'o', 'O' -> '0'
            's', 'S' -> '$'
            't', 'T' -> '7'
            else -> char
        }
    }

    private fun mockingConverter(input: String): String {
        val message = StringBuilder()

        if (randomCase) {
            for (char in input) {
                val newChar = if (Random.nextBoolean()) char.uppercaseChar() else char.lowercaseChar()
                message.append(newChar)
            }
        } else {
            for ((i, char) in input.withIndex()) {
                val newChar = if (MathUtils.isNumberEven(i)) char.uppercaseChar() else char.lowercaseChar()
                message.append(newChar)
            }
        }

        return message.toString()
    }

    private fun uwuConverter(input: String): String {
        var newString = input
        newString = newString.replace("ove", "uv")
        newString = newString.replace("the", "da")
        newString = newString.replace("is", "ish")
        newString = newString.replace("r", "w")
        newString = newString.replace("ve", "v")
        newString = newString.replace("l", "w")
        return newString
    }
}
