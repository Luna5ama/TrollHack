package cum.xiaro.trollhack.gui.hudgui.elements.player

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.gui.hudgui.LabelHud
import cum.xiaro.trollhack.util.math.Direction

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