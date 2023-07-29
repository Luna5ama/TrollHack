package dev.luna5ama.trollhack.mixins.core.render;

import dev.luna5ama.trollhack.module.modules.render.AntiAlias;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING;

@Mixin(GlStateManager.class)
abstract class MixinGlStateManager<T extends Entity> {
    @Inject(method = "viewport", at = @At("HEAD"), cancellable = true)
    private static void viewport$Inject$HEAD(int x, int y, int width, int height, CallbackInfo ci) {
        float sampleLevel = AntiAlias.INSTANCE.getSampleLevel();
        if (sampleLevel == 1.0f) return;

        Framebuffer framebuffer = Minecraft.getMinecraft().getFramebuffer();
        if (GL11.glGetInteger(GL_FRAMEBUFFER_BINDING) == Minecraft.getMinecraft().getFramebuffer().framebufferObject) {
            ci.cancel();
            if (x == 0 && y == 0) {
                GL11.glViewport(x, y, framebuffer.framebufferWidth, framebuffer.framebufferHeight);
            } else {
                GL11.glViewport(
                    (int) (x * sampleLevel),
                    (int) (y * sampleLevel),
                    (int) (width * sampleLevel),
                    (int) (height * sampleLevel)
                );
            }
        }
    }

    @Inject(method = "glLineWidth", at = @At("HEAD"), cancellable = true)
    private static void glLineWidth$Inject$HEAD(float width, CallbackInfo ci) {
        float sampleLevel = AntiAlias.INSTANCE.getSampleLevel();
        if (sampleLevel != 1.0f) {
            ci.cancel();
            GL11.glLineWidth(width * sampleLevel);
        }
    }
}
