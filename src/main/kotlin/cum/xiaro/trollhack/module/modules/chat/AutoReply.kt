package cum.xiaro.trollhack.module.modules.chat

import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeParallelListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.TimeUnit
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.text.MessageDetection
import cum.xiaro.trollhack.util.text.MessageSendUtils
import cum.xiaro.trollhack.util.text.MessageSendUtils.sendServerMessage
import net.minecraft.network.play.server.SPacketChat

internal object AutoReply : Module(
    name = "AutoReply",
    description = "Automatically reply to direct messages",
    category = Category.CHAT
) {
    private val customMessage = setting("Custom Message", false)
    private val customText = setting("Custom Text", "unchanged", customMessage.atTrue())

    private val timer = TickTimer(TimeUnit.SECONDS)

    init {
        listener<PacketEvent.Receive> {
            if (it.packet !is SPacketChat || MessageDetection.Direct.RECEIVE detect it.packet.chatComponent.unformattedText) return@listener
            if (customMessage.value) {
                sendServerMessage("/r " + customText.value)
            } else {
                sendServerMessage("/r I just automatically replied, thanks to Troll Hack's AutoReply module!")
            }
        }

        safeParallelListener<TickEvent.Post> {
            if (timer.tickAndReset(5L) && customMessage.value && customText.value.equals("unchanged", true)) {
                MessageSendUtils.sendNoSpamWarningMessage("$chatName Warning: In order to use the custom $name, please change the CustomText setting in ClickGUI")
            }
        }
    }
}