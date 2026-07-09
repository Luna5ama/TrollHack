package dev.luna5ama.trollhack.libraries

import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import kotlin.io.path.Path

abstract class AbstractLibrary(val namespace: String) {
//    val root = Path("assets", "trollhack", "libraries")
    val root = Path(TrollHackMod.ID, "libraries")

    abstract fun load(architecture: Architecture, os: OS): Boolean
}