package cum.xiaro.trollhack.module.modules.chat

import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.TimeUtils
import cum.xiaro.trollhack.util.text.MessageSendUtils.sendServerMessage
import cum.xiaro.trollhack.util.text.NoSpamMessage
import cum.xiaro.trollhack.util.text.format
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.EnumDifficulty

internal object AutoQMain : Module(
    name = "AutoQMain",
    description = "Automatically does '/queue main'",
    category = Category.CHAT,
    visible = false
) {
    private val delay by setting("Delay", 5000, 0..15000, 100)
    private val twoBeeCheck by setting("2B Check", true)
    private val command by setting("Command", "/queue main")

    private val timer = TickTimer()

    init {
        @Suppress("UNNECESSARY_SAFE_CALL")
        safeListener<TickEvent.Pre> {
            if (world.difficulty == EnumDifficulty.PEACEFUL
                && player.dimension == 1
                && (!twoBeeCheck || player.serverBrand?.contains("2b2t") == true)
                && timer.tickAndReset(delay)) {
                sendQueueMain()
            }
        }
    }

    private fun sendQueueMain() {
        NoSpamMessage.sendMessage(this, "$chatName Run ${TextFormatting.GRAY format command} at ${TextFormatting.GRAY format TimeUtils.getTime(TimeUtils.TimeFormat.HHMMSS, TimeUtils.TimeUnit.H24)}")
        sendServerMessage(command)
    }
}
