package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import dev.luna5ama.trollhack.manager.AbstractManager
import dev.luna5ama.trollhack.utils.delegates.CachedValueN
import dev.luna5ama.trollhack.utils.extension.listFilesRecursively
import dev.luna5ama.trollhack.config.Categories
import dev.luna5ama.trollhack.config.FileSystemSource
import dev.luna5ama.trollhack.modules.impl.client.Presets
import dev.luna5ama.trollhack.utils.Profiler

object ConfigManager : AbstractManager() {
    var namespace = "default"
    var currentFileSystemSource = FileSystemSource.DefaultDiskSource
    val configRoot get() = currentFileSystemSource.root.resolve(TrollHackMod.ID)
    val currentConfigRoot get() = configRoot.resolve("config").resolve(namespace)
    private val lastCleanedFile get() = configRoot.resolve(".lastcleaned")
    private val currentNamespaceFile get() = configRoot.resolve(".current")
    val lastCleanTime by CachedValueN(10000) {
        lastCleanedFile.readText().toLong()
    }

    private val configCategories = Categories()

    fun clean() {
        if (!currentConfigRoot.exists()) save()
        else {
            if (!lastCleanedFile.exists()) lastCleanedFile.createNewFile()
            configCategories.clean()
            lastCleanedFile.writeText(System.currentTimeMillis().toString())
        }
    }

    override fun load(profilerScope: Profiler.ProfilerScope) {
        if (configRoot.exists() && configRoot.isFile) configRoot.delete()
        configRoot.mkdirs()

        if (currentNamespaceFile.exists() && currentNamespaceFile.isFile) {
            namespace = currentNamespaceFile.readText()
            if (!currentConfigRoot.exists()) namespace = "default"
        } else {
            if (currentNamespaceFile.exists()) currentNamespaceFile.deleteRecursively()
            currentNamespaceFile.createNewFile()
            currentNamespaceFile.writeText("default")
        }
        Presets.target = namespace
        TrollHackMod.LOGGER.info("Loading from '$namespace'")

        if (!currentConfigRoot.exists()) save()
        if (!lastCleanedFile.exists()) {
            lastCleanedFile.createNewFile()
            inferCleanTime()
        }
        configCategories.read()
        save()
    }

    fun tryLoad(): Boolean {
        if (!currentConfigRoot.exists()) return false
        if (!lastCleanedFile.exists()) {
            lastCleanedFile.createNewFile()
            inferCleanTime()
        }
        TrollHackMod.LOGGER.info("Refreshing from '$namespace'")
        configCategories.read()
        currentNamespaceFile.writeText(namespace)
        save()
        return true
    }

    private fun inferCleanTime() {
        lastCleanedFile.writeText(configRoot.listFilesRecursively().maxOf { it.lastModified() }.toString())
    }

    fun save() {
        if (currentNamespaceFile.exists()) {
            if (currentNamespaceFile.isDirectory) {
                currentNamespaceFile.deleteRecursively()
                currentNamespaceFile.createNewFile()
            }
        } else currentNamespaceFile.createNewFile()
        currentNamespaceFile.writeText(namespace)

        TrollHackMod.LOGGER.info("Saving to '$namespace'")

        if (!currentConfigRoot.exists()) currentConfigRoot.mkdirs()
        configCategories.save()
        clean()
    }

    fun getOrCreateCategory(category: String) = configCategories.getConfigurationManager(category)
}