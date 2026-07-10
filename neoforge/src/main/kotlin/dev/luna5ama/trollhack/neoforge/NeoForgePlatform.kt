package dev.luna5ama.trollhack.neoforge

import dev.luna5ama.trollhack.platform.IModLoaderPlatform
import net.neoforged.fml.ModList

class NeoForgePlatform : IModLoaderPlatform {
    override fun getModVersion(mod: String): String? {
        return ModList.get().getModContainerById(mod)
            .map { it.modInfo.version.toString() }
            .orElse(null)
    }
}
