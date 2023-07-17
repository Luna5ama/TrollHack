package dev.luna5ama.trollhack.command.commands

import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer

object ReloadGlyphCommand : ClientCommand(
    name = "reloadglyph",
    description = "Reloads the font renderer glyph",
) {
    init {
        execute {
            MainFontRenderer.reloadFonts()
        }
    }
}