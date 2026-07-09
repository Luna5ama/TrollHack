package dev.luna5ama.trollhack.modules.impl.misc

import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.impl.render.RenderEntityEvent
import dev.luna5ama.trollhack.event.impl.world.TickEntityEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import net.minecraft.world.entity.item.ItemEntity

object AntiItemCrash : Module("Anti Item Crash", category = Category.MISC) {
    init {
        handler<RenderEntityEvent.All.Pre> { event ->
            if (event.entity is ItemEntity) event.cancel()
        }

        handler<TickEntityEvent.Pre> { event ->
            if (event.entity is ItemEntity) event.cancel()
        }
    }
}