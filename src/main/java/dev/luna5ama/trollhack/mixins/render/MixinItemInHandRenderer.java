package dev.luna5ama.trollhack.mixins.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luna5ama.trollhack.graphics.blaze3d.ShaderHolder;
import dev.luna5ama.trollhack.modules.impl.visual.Shaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {
    @Final
    @Shadow
    private Minecraft minecraft;

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void beginShadersHandCapture(
            float frameInterp,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            LocalPlayer player,
            int lightCoords,
            CallbackInfo ci
    ) {
        Shaders shaders = Shaders.INSTANCE;
        if (shaders.isEnabled() && Shaders.shouldRenderHands()) {
            ShaderHolder.beginHandOutlineCapture(
                    minecraft.getMainRenderTarget().width,
                    minecraft.getMainRenderTarget().height
            );
        }
    }

    @Inject(method = "renderHandsWithItems", at = @At("RETURN"))
    private void endShadersHandCapture(
            float frameInterp,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            LocalPlayer player,
            int lightCoords,
            CallbackInfo ci
    ) {
        if (ShaderHolder.isRenderingHands()) {
            minecraft.renderBuffers().outlineBufferSource().endOutlineBatch();
        }
        ShaderHolder.endHandOutlineCapture();
    }

    @ModifyArg(
            method = "renderItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
            ),
            index = 4
    )
    private int applyShadersHandOutline(int outlineColor) {
        Shaders shaders = Shaders.INSTANCE;
        return shaders.isEnabled() && Shaders.shouldRenderHands() ? Shaders.outlineArgb() : outlineColor;
    }
}
