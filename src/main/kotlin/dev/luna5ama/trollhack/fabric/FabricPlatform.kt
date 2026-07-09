package dev.luna5ama.trollhack.fabric

import dev.luna5ama.trollhack.platform.IModLoaderPlatform
import net.fabricmc.loader.api.FabricLoader
import kotlin.jvm.optionals.getOrNull

class FabricPlatform : IModLoaderPlatform {
    override fun getModVersion(mod: String): String? {
        return FabricLoader.getInstance().getModContainer(mod)
            .map { it.metadata.version.friendlyString }.getOrNull()
    }
}
