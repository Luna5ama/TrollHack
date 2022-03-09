package me.luna.trollhack.module.modules.combat

import me.luna.trollhack.event.events.InputEvent
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.manager.managers.HotbarManager.spoofHotbar
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.inventory.slot.firstItem
import me.luna.trollhack.util.inventory.slot.hotbarSlots
import me.luna.trollhack.util.text.MessageSendUtils.sendNoSpamChatMessage
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