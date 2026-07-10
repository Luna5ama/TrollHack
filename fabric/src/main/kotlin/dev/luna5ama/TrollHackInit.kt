package dev.luna5ama

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.utils.sound.SoundPack
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader

class TrollHackInit : ModInitializer{
    override fun onInitialize() {
        TrollHackMod.FOLDER = (FabricLoader.getInstance().gameDir.resolve(TrollHackMod.ID).toFile())
        SoundPack.registerSounds()
    }
}