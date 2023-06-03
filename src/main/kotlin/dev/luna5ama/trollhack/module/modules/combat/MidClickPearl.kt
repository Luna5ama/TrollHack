package dev.luna5ama.trollhack.module.modules.combat

import dev.luna5ama.trollhack.event.events.InputEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.spoofHotbar
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.inventory.slot.firstItem
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.util.EnumHand
import net.minecraft.util.math.RayTraceResult.Type

internal object MidClickPearl : Module(
    name = "Mid Click Pearl",
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
                    NoSpamMessage.sendMessage("No Ender Pearl was found in hotbar!")
                }
            }
        }
    }
}