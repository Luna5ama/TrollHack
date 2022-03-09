package me.luna.trollhack.gui.hudgui.elements.player

import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.gui.hudgui.LabelHud
import me.luna.trollhack.util.math.Direction

internal object Direction : LabelHud(
    name = "Direction",
    category = Category.PLAYER,
    description = "Direction of player facing to"
) {

    override fun SafeClientEvent.updateText() {
        val entity = mc.renderViewEntity ?: player
        val direction = Direction.fromEntity(entity)
        displayText.add(direction.displayString, secondaryColor)
        displayText.add("(${direction.displayNameXY})", primaryColor)
    }

}