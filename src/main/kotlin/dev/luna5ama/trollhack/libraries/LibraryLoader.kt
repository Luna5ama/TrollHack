package dev.luna5ama.trollhack.libraries

import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod

object LibraryLoader {
    private val libraries = ArrayList<AbstractLibrary>()
    val loadFlags: BooleanArray

    init {
        libraries.run {
            add(ImGui)
        }
        loadFlags = BooleanArray(libraries.size)
    }

    fun load() {
        val arch = Architecture.detectArchitecture()
        val os = OS.detectOs()

        libraries.forEachIndexed { index, lib ->
            try {
                if (!lib.load(arch, os)) {
                    throw UnsatisfiedLinkError()
                } else {
                    TrollHackMod.LOGGER.info("Library ${lib.namespace} was loaded successfully")
                    loadFlags[index] = true
                }
            } catch (e: Throwable) {
                TrollHackMod.LOGGER.error("Failed to load library ${lib.namespace}.", e)
            }
        }
    }
}
