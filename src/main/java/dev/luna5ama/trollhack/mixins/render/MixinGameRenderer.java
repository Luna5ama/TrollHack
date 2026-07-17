package dev.luna5ama.trollhack.mixins.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.luna5ama.trollhack.graphics.blaze3d.ShaderHolder;
import dev.luna5ama.trollhack.graphics.skia.SkiaMinecraftBridge;
import dev.luna5ama.trollhack.modules.impl.visual.FreeCamera;
import dev.luna5ama.trollhack.modules.impl.visual.NoRender;
import dev.luna5ama.trollhack.modules.impl.visual.Shaders;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onRender2D(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
        SkiaMinecraftBridge.INSTANCE.render2D(tickDelta);
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void tiltViewWhenHurtHook(CameraRenderState cameraRenderState, PoseStack poseStack, CallbackInfo ci) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getHurtCam()) {
            ci.cancel();
        }
    }

    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack stack, CallbackInfo ci) {
        if (stack.getItem() == Items.TOTEM_OF_UNDYING && NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getTotem()) {
            ci.cancel();
        }
    }

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F"))
    private float applyCameraTransformationsMathHelperLerpProxy(float delta, float first, float second) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getNausea()) return 0;
        return Mth.lerp(delta, first, second);
    }
    @Unique
    private static final Minecraft mc = Minecraft.getInstance();

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void processShadersOutline(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        if (!Shaders.INSTANCE.isEnabled()) return;

        try {
            ShaderHolder.processEntityOutlineTarget(mc.levelRenderer.entityOutlineTarget(), Shaders.INSTANCE.getMode());
            ShaderHolder.processChestOutlineTarget(mc.getMainRenderTarget());
            ShaderHolder.processHandOutlineTarget(mc.getMainRenderTarget());
        } finally {
            ShaderHolder.endFrame();
        }
    }

    @ModifyExpressionValue(
            method = "renderItemInHand",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z")
    )
    private boolean renderFreeCameraHands(boolean vanilla) {
        return FreeCamera.shouldRenderHands(vanilla);
    }
}
