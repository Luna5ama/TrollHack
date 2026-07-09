package dev.luna5ama.trollhack.modules.impl.player

import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module

object NoEntityTrace : Module(
    "No Entity Trace", "NoEntityTrace", Category.PLAYER
){
     val ponly by setting("Pickaxe Only", true)
     val noSword by setting("No Sword", true)
}

