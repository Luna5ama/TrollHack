package dev.luna5ama.trollhack.gui.hudgui.elements.world

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.hudgui.LabelHud

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