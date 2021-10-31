package cum.xiaro.trollhack.module.modules.misc

import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.text.MessageSendUtils
import net.minecraft.item.ItemWrittenBook
import net.minecraft.network.play.client.CPacketClickWindow

/**
 * @author IronException
 * Used with permission from ForgeHax
 * https://github.com/fr1kin/ForgeHax/blob/bb522f8/src/main/java/com/matt/forgehax/mods/AntiBookKick.java
 * Permission (and ForgeHax is MIT licensed):
 * https://discordapp.com/channels/573954110454366214/634010802403409931/693919755647844352
 */
internal object AntiBookKick : Module(
    name = "AntiBookKick",
    category = Category.MISC,
    description = "Prevents being kicked by clicking on books",
    visible = false
) {
    init {
        listener<PacketEvent.PostSend> {
            if (it.packet !is CPacketClickWindow) return@listener
            if (it.packet.clickedItem.item !is ItemWrittenBook) return@listener

            MessageSendUtils.sendNoSpamWarningMessage(chatName
                + " Don't click the book \""
                + it.packet.clickedItem.displayName
                + "\", shift click it instead!")
            mc.player.openContainer.slotClick(it.packet.slotId, it.packet.usedButton, it.packet.clickType, mc.player)
        }
    }
}