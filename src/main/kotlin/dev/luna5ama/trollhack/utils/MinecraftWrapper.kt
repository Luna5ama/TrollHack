package dev.luna5ama.trollhack.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer

object MinecraftWrapper {
    @JvmStatic
    val mc: Minecraft get() = Minecraft.getInstance()

    @JvmStatic
    val minecraft: Minecraft get() = mc

    @ImplicitOverriding
    @JvmStatic
    val player: LocalPlayer? get() = mc.player

    @ImplicitOverriding
    @JvmStatic
    val world: ClientLevel? get() = mc.level
}