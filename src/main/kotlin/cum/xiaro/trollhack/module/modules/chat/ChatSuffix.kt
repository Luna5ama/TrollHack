package cum.xiaro.trollhack.module.modules.chat

import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeParallelListener
import cum.xiaro.trollhack.manager.managers.MessageManager.newMessageModifier
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.TimeUnit
import cum.xiaro.trollhack.util.text.MessageDetection
import cum.xiaro.trollhack.util.text.MessageSendUtils
import kotlin.math.min

internal object ChatSuffix : Module(
    name = "ChatSuffix",
    category = Category.CHAT,
    description = "Add a custom ending to your message!",
    visible = false,
    modulePriority = 200
) {
    private val textMode by setting("Message", TextMode.NAME)
    private val decoMode by setting("Separator", DecoMode.NONE)
    private val commands by setting("Commands", false)
    private val spammer by setting("Spammer", false)
    private val customText by setting("Custom Text", "Default")

    private enum class DecoMode {
        SEPARATOR, CLASSIC, NONE
    }

    private enum class TextMode {
        NAME, CUSTOM
    }

    private val timer = TickTimer(TimeUnit.SECONDS)
    private val modifier = newMessageModifier(
        filter = {
            (commands || MessageDetection.Command.ANY detectNot it.packet.message)
                && (spammer || it.source !is Spammer)
        },
        modifier = {
            val message = it.packet.message + getFull()
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

    private fun getText() = when (textMode) {
        TextMode.NAME -> "ＴＲＯＬＬ　ＨＡＣＫ"
        TextMode.CUSTOM -> customText
    }

    private fun getFull() = when (decoMode) {
        DecoMode.NONE -> " " + getText()
        DecoMode.CLASSIC -> " \u00ab " + getText() + " \u00bb"
        DecoMode.SEPARATOR -> " | " + getText()
    }

    init {
        safeParallelListener<TickEvent.Post> {
            if (timer.tickAndReset(5L) && textMode == TextMode.CUSTOM && customText.equals("Default", ignoreCase = true)) {
                MessageSendUtils.sendNoSpamWarningMessage("$chatName Warning: In order to use the custom $name, please change the CustomText setting in ClickGUI")
            }
        }
    }


}
