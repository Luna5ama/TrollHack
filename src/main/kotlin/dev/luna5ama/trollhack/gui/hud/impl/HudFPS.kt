package dev.luna5ama.trollhack.gui.hud.impl

import dev.luna5ama.trollhack.gui.hud.PlainTextHud
import dev.luna5ama.trollhack.utils.MinecraftWrapper
import dev.luna5ama.trollhack.graphics.font.TextComponent

object HudFPS : PlainTextHud("FPS Hud") {
    context (TextInfo)
    override fun TextComponent.buildText() {
        addLine("FPS: ${MinecraftWrapper.mc.fps}")
    }
}