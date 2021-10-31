package cum.xiaro.trollhack.module.modules.chat

import cum.xiaro.trollhack.util.interfaces.DisplayEnum
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.TimeUtils
import cum.xiaro.trollhack.util.accessor.textComponent
import cum.xiaro.trollhack.util.graphics.color.EnumTextColor
import cum.xiaro.trollhack.util.text.format
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.util.text.TextComponentString

internal object ChatTimestamp : Module(
    name = "ChatTimestamp",
    category = Category.CHAT,
    description = "Shows the time a message was sent beside the message",
    visible = false
) {
    private val color by setting("Color", EnumTextColor.GRAY)
    private val separator by setting("Separator", Separator.ARROWS)
    private val timeFormat by setting("Time Format", TimeUtils.TimeFormat.HHMM)
    private val timeUnit by setting("Time Unit", TimeUtils.TimeUnit.H12)

    init {
        listener<PacketEvent.Receive> {
            if (it.packet is SPacketChat) {
                it.packet.textComponent = TextComponentString(formattedTime).appendSibling(it.packet.textComponent)
            }
        }
    }

    val formattedTime: String
        get() = "${separator.left}${color format TimeUtils.getTime(timeFormat, timeUnit)}${separator.right} "

    val time: String
        get() = "${separator.left}${TimeUtils.getTime(timeFormat, timeUnit)}${separator.right} "

    @Suppress("unused")
    private enum class Separator(override val displayName: CharSequence, val left: String, val right: String) :
        DisplayEnum {
        ARROWS("< >", "<", ">"),
        SQUARE_BRACKETS("[ ]", "[", "]"),
        CURLY_BRACKETS("{ }", "{", "}"),
        ROUND_BRACKETS("( )", "(", ")"),
        NONE("None", "", "")
    }
}
