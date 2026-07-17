package dev.luna5ama.trollhack.mixins.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.luna5ama.trollhack.graphics.skia.SkiaMinecraftBridge;
import dev.luna5ama.trollhack.modules.impl.player.NoEntityTrace;
import dev.luna5ama.trollhack.modules.impl.visual.AspectRatio;
import dev.luna5ama.trollhack.modules.impl.visual.FreeCamera;
import dev.luna5ama.trollhack.modules.impl.visual.NoRender;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Shadow public abstract float getDepthFar();

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onRender2D(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci, @Local GuiGraphics guiGraphics) {
        float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
        SkiaMinecraftBridge.INSTANCE.render2D(tickDelta);
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void tiltViewWhenHurtHook(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
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

    @Inject(method = "getProjectionMatrix", at = @At("TAIL"), cancellable = true)
    public void getBasicProjectionMatrixHook(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
        if (AspectRatio.INSTANCE.isEnabled()) {
            cir.setReturnValue(new Matrix4f().setPerspective(
                    (float) (fovDegrees * (Math.PI / 180d)),
                    AspectRatio.getRatio(),
                    0.05f,
                    getDepthFar()
            ));
        }
    }

    @ModifyExpressionValue(
            method = "renderItemInHand",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z")
    )
    private boolean renderFreeCameraHands(boolean vanilla) {
        return FreeCamera.shouldRenderHands(vanilla);
    }

    @Inject(method = "pick(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;raycastHitResult(FLnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/HitResult;"), cancellable = true)
    private void onUpdateTargetedEntity(float tickDelta, CallbackInfo info) {
        if (mc.crosshairPickEntity != null && mc.player != null
                && NoEntityTrace.INSTANCE.isEnabled()
                && (mc.player.getMainHandItem().is(ItemTags.PICKAXES) || !NoEntityTrace.INSTANCE.getPonly())
                && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            if (mc.player.getMainHandItem().is(ItemTags.SWORDS) && NoEntityTrace.INSTANCE.getNoSword()) return;
            Profiler.get().pop();
            info.cancel();
        }
    }
}
