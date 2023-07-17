package dev.luna5ama.trollhack.module.modules.render

import dev.luna5ama.trollhack.graphics.AnimationFlag
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.threads.runSafe

internal object GuiAnimation : Module(
    name = "Gui Animation",
    category = Category.RENDER,
    description = "Animates Minecraft gui",
    visible = false,
    enabledByDefault = true
) {
    private val hotbarAnimation = AnimationFlag(Easing.OUT_CUBIC, 200.0f)

    init {
        onEnable {
            runSafe {
                val currentPos = player.inventory.currentItem * 20.0f
                hotbarAnimation.forceUpdate(currentPos, currentPos)
            }
        }
    }

    @JvmStatic
    fun updateHotbar(): Float {
        val currentPos = mc.player?.let {
            it.inventory.currentItem * 20.0f
        } ?: 0.0f

        return hotbarAnimation.getAndUpdate(currentPos)
    }
}