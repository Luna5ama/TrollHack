package dev.luna5ama.trollhack.modules.impl.misc

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.UpdateEvent
import dev.luna5ama.trollhack.mixins.accessor.IMinecraftClientAccessor
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module

object FastPlace : Module(
    "Fast Place",
    "Places blocks exceptionally fast",
    Category.MISC
) {

    init {
        nonNullHandler<UpdateEvent> {
            (mc as IMinecraftClientAccessor ).setRightClickDelay(0)
        }
    }
}
