package dev.luna5ama.trollhack.neoforge

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.utils.sound.SoundPack
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLPaths

@Mod(TrollHackMod.ID)
class TrollHackNeoForge {
    init {
        TrollHackMod.FOLDER = FMLPaths.GAMEDIR.get().resolve(TrollHackMod.ID).toFile()
        SoundPack.registerSounds()
    }
}
