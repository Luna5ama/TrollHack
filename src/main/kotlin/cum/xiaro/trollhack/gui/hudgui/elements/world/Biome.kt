package cum.xiaro.trollhack.gui.hudgui.elements.world

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.gui.hudgui.LabelHud

internal object Biome : LabelHud(
    name = "Biome",
    category = Category.WORLD,
    description = "Display the current biome you are in"
) {

    override fun SafeClientEvent.updateText() {
        val biome = world.getBiome(player.position).biomeName ?: "Unknown"

        displayText.add(biome, primaryColor)
        displayText.add("Biome", secondaryColor)
    }

}