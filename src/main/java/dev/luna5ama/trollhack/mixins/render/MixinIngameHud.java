package dev.luna5ama.trollhack.mixins.render;

import dev.luna5ama.trollhack.modules.impl.visual.NoRender;
import dev.luna5ama.trollhack.modules.impl.visual.GameAnimation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinIngameHud {

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void hidePotionEffects(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getPotionEffects()) {
            ci.cancel();
        }
    }

    @ModifyArg(
            method = "renderItemHotbar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V", ordinal = 1),
            index = 2
    )
    private int animateHotbarSelection(int vanillaX) {
        return GameAnimation.hotbarSelectionX(vanillaX);
    }

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderPortalOverlay(GuiGraphics guiGraphics, float alpha, CallbackInfo ci) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getPortal()) ci.cancel();
    }
}
