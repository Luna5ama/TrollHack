package dev.luna5ama.trollhack.mixins.remove_poll;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {
    @SuppressWarnings("EmptyMethod")
    @Redirect(method = "flipFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;pollEvents()V", ordinal = 0), remap = false)
    private static void removeFirstPoll() {
        // noop
        // should fix some bugs with minecraft polling events twice for some reason (why does it do that in the first place?)
    }
}
