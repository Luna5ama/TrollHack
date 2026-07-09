package dev.luna5ama.trollhack.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.multiplayer.ClientLevel

interface Helper {
    val mc: Minecraft get() = Minecraft.getInstance()

    val minecraft: Minecraft get() = mc

    val player: LocalPlayer? get() = mc.player

    val world: ClientLevel? get() = mc.level
}