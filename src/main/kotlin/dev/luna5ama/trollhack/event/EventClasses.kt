package dev.luna5ama.trollhack.event

import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.utils.Reflections
import dev.luna5ama.trollhack.utils.shortName

object EventClasses {
    val classes: Set<Class<out IEvent>> = Reflections.getSubTypesOf(IEvent::class.java)

    init {
        TrollHackMod.LOGGER.info("Available events:")
        for (clz in classes) {
            TrollHackMod.LOGGER.info("\t ${clz.shortName}")
        }
    }
}