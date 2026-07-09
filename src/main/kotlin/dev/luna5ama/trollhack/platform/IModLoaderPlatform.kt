package dev.luna5ama.trollhack.platform

interface IModLoaderPlatform {
    fun getModVersion(mod: String): String?
}