package dev.luna5ama.trollhack.module.modules.misc

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.client.MainMenu
import org.lwjgl.opengl.Display
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

internal object UnfocusedFps : Module(
    name = "Unfocused Fps",
    description = "Reduces FPS when the game is running in the background",
    category = Category.MISC
) {
    private val fpsLimit by setting("FPS Limit", 30, 1..120, 1)

    @JvmStatic
    fun handleGetLimitFramerate(cir: CallbackInfoReturnable<Int>) {
        if (isDisabled) return
        if (Display.isActive()) return
        cir.returnValue = fpsLimit
    }
}