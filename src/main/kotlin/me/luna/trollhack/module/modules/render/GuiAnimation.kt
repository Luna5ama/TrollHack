package me.luna.trollhack.module.modules.render

import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.graphics.AnimationFlag
import me.luna.trollhack.util.graphics.Easing
import me.luna.trollhack.util.threads.runSafe

internal object GuiAnimation : Module(
    name = "GuiAnimation",
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