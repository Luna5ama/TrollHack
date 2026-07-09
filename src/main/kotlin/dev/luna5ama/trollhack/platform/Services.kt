package dev.luna5ama.trollhack.platform

import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import java.util.*
import java.util.function.Supplier

object Services {
    val PLATFORM = load(IModLoaderPlatform::class.java)

    private fun <T> load(clazz: Class<out T>): T {
        val loadedService = ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow { NullPointerException("Failed to load service for " + clazz.getName()) }
        TrollHackMod.LOGGER.debug("Loaded {} for service {}", loadedService, clazz)
        return loadedService
    }
}