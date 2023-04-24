package dev.luna5ama.trollhack.gui.hudgui.elements.player

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.math.MathUtils
import net.minecraft.util.EnumHand

internal object Durability : LabelHud(
    name = "Durability",
    category = Category.PLAYER,
    description = "Durability of holding items"
) {

    private val showItemName = setting("Show Item Name", true)
    private val showOffhand = setting("Show Offhand", false)
    private val showPercentage = setting("Show Percentage", true)

    override fun SafeClientEvent.updateText() {
        if (mc.player.heldItemMainhand.isItemStackDamageable) {
            if (showOffhand.value) displayText.add("MainHand:", GuiSetting.primary)
            addDurabilityText(EnumHand.MAIN_HAND)
        }

        if (showOffhand.value && mc.player.heldItemOffhand.isItemStackDamageable) {
            displayText.add("OffHand:", GuiSetting.primary)
            addDurabilityText(EnumHand.OFF_HAND)
        }
    }

    private fun addDurabilityText(hand: EnumHand) {
        val itemStack = mc.player.getHeldItem(hand)
        if (showItemName.value) displayText.add(itemStack.displayName, GuiSetting.text)

        val durability = itemStack.maxDamage - itemStack.itemDamage
        val text = if (showPercentage.value) {
            "${MathUtils.round((durability / itemStack.maxDamage.toFloat()) * 100.0f, 1)}%"
        } else {
            "$durability/${itemStack.maxDamage}"
        }

        displayText.addLine(text, GuiSetting.text)
    }

}