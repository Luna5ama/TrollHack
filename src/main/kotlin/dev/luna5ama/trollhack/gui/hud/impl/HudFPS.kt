package dev.luna5ama.trollhack.gui.hud.impl

import dev.luna5ama.trollhack.gui.hud.PlainTextHud
import dev.luna5ama.trollhack.utils.MinecraftWrapper

object HudFPS : PlainTextHud("FPS Hud") {
    override fun lines() = listOf("FPS: ${MinecraftWrapper.mc.fps}")
}
