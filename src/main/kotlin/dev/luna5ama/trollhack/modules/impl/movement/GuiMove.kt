package dev.luna5ama.trollhack.modules.impl.movement

import com.mojang.blaze3d.platform.InputConstants
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.player.InputUpdateEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.world.entity.player.Input

object GuiMove : Module("GUI Move", "Walk in inventory.", Category.MOVEMENT) {
    val sneak by setting("Sneak", false)

    init {
        nonNullHandler<InputUpdateEvent>(Int.MAX_VALUE) { event ->
            if (mc.screen == null || mc.screen is ChatScreen) return@nonNullHandler

            val up = isKeyDown(mc.options.keyUp)
            val down = isKeyDown(mc.options.keyDown)
            val left = isKeyDown(mc.options.keyLeft)
            val right = isKeyDown(mc.options.keyRight)
            val original = event.movementInput.keyPresses

            event.movementInput.keyPresses = Input(
                up,
                down,
                left,
                right,
                isKeyDown(mc.options.keyJump),
                if (sneak) isKeyDown(mc.options.keyShift) else original.shift(),
                isKeyDown(mc.options.keySprint)
            )
        }
    }

    private fun isKeyDown(mapping: KeyMapping): Boolean {
        return InputConstants.isKeyDown(
            mc.window,
            InputConstants.getKey(mapping.saveString()).value
        )
    }
}
