package dev.luna5ama.trollhack.modules.impl.movement

import com.mojang.blaze3d.platform.InputConstants
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.UpdateEvent
import dev.luna5ama.trollhack.event.impl.render.Render3DEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import net.minecraft.client.gui.screens.ChatScreen

object GuiMove : Module("Gui Move", "Walk in inventory.", Category.MOVEMENT) {
    val sneak by setting("Sneak", true)

    init {
        nonNullHandler<Render3DEvent> {
            update()
        }

        nonNullHandler<UpdateEvent> {
            update()
        }
    }

    fun update() {
        if (mc.screen != null) {
            if (mc.screen !is ChatScreen) {
                for (k in arrayOf(
                    mc.options.keyDown,
                    mc.options.keyLeft,
                    mc.options.keyRight,
                    mc.options.keyJump,
                    mc.options.keySprint
                )) {
                    k.isDown = InputConstants.isKeyDown(
                        mc.window,
                        InputConstants.getKey(k.saveString()).value
                    )
                }

                mc.options.keyUp.isDown = InputConstants.isKeyDown(
                    mc.window,
                    InputConstants.getKey(mc.options.keyUp.saveString()).value
                )

                if (sneak) {
                    mc.options.keyShift.isDown = InputConstants.isKeyDown(
                        mc.window,
                        InputConstants.getKey(mc.options.keyShift.saveString()).value
                    )
                }
            }
        }
    }
}
