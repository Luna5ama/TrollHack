package dev.luna5ama.trollhack.module.modules.movement

import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import net.minecraft.init.MobEffects

internal object AntiLevitation : Module(
    name = "Anti Levitation",
    description = "Removes levitation potion effect",
    category = Category.MOVEMENT
) {
    init {
        safeListener<TickEvent.Pre> {
            player.removeActivePotionEffect(MobEffects.LEVITATION)
        }
    }
}