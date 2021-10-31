package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.event.events.InputEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.HotbarManager.spoofHotbar
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.inventory.slot.firstItem
import cum.xiaro.trollhack.util.inventory.slot.hotbarSlots
import cum.xiaro.trollhack.util.text.MessageSendUtils.sendNoSpamChatMessage
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.util.EnumHand
import net.minecraft.util.math.RayTraceResult.Type

internal object MidClickPearl : Module(
    name = "MidClickPearl",
    category = Category.COMBAT,
    description = "Throws a pearl automatically when you middle click in air"
) {
    init {
        safeListener<InputEvent.Mouse> {
            if (it.state || it.button != 2) return@safeListener

            val objectMouseOver = mc.objectMouseOver
            if (objectMouseOver == null || objectMouseOver.typeOfHit != Type.BLOCK) {
                val pearlSlot = player.hotbarSlots.firstItem(Items.ENDER_PEARL)

                if (pearlSlot != null) {
                    spoofHotbar(pearlSlot) {
                        connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
                    }
                } else {
                    sendNoSpamChatMessage("No Ender Pearl was found in hotbar!")
                }
            }
        }
    }
}