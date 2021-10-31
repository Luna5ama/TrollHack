package cum.xiaro.trollhack.mixin.render;

import cum.xiaro.trollhack.event.events.render.Render2DEvent;
import cum.xiaro.trollhack.gui.hudgui.elements.client.Notification;
import cum.xiaro.trollhack.module.modules.movement.ElytraFlight;
import cum.xiaro.trollhack.module.modules.player.BlockInteraction;
import cum.xiaro.trollhack.module.modules.render.AntiFog;
import cum.xiaro.trollhack.module.modules.render.AntiOverlay;
import cum.xiaro.trollhack.module.modules.render.Fov;
import cum.xiaro.trollhack.module.modules.render.ThirdPersonCamera;
import cum.xiaro.trollhack.util.Wrapper;
import cum.xiaro.trollhack.util.graphics.GlStateUtils;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityRenderer.class, priority = Integer.MAX_VALUE)
public class MixinEntityRenderer {
    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V", shift = At.Shift.AFTER))
    public void updateCameraAndRender(float partialTicks, long nanoTime, CallbackInfo ci) {
        Wrapper.getMinecraft().profiler.startSection("trollRender2D");

        GlStateUtils.INSTANCE.alpha(false);
        GlStateUtils.INSTANCE.pushMatrixAll();

        Render2DEvent.Mc.INSTANCE.post();
        GlStateUtils.INSTANCE.rescaleActual();
        Render2DEvent.Absolute.INSTANCE.post();
        GlStateUtils.INSTANCE.rescaleTroll();
        Render2DEvent.Troll.INSTANCE.post();

        GlStateUtils.INSTANCE.popMatrixAll();
        GlStateUtils.INSTANCE.alpha(true);

        GlStateUtils.INSTANCE.useProgramForce(0);
        Wrapper.getMinecraft().profiler.endSection();
    }

    @Inject(method = "updateCameraAndRender", at = @At(value = "RETURN"))
    public void updateCameraAndRender$Inject$RETURN(float partialTicks, long nanoTime, CallbackInfo ci) {
        Wrapper.getMinecraft().profiler.endStartSection("trollNotification");
        Notification.INSTANCE.render();
        GlStateUtils.INSTANCE.useProgramForce(0);
    }

    @ModifyVariable(method = "orientCamera", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    public RayTraceResult orientCamera$ModifyVariable$0$STORE$0(RayTraceResult value) {
        if (ThirdPersonCamera.INSTANCE.isEnabled()) {
            return null;
        } else {
            return value;
        }
    }

    @ModifyVariable(method = "orientCamera", at = @At(value = "STORE", ordinal = 0), ordinal = 3)
    public double orientCamera$ModifyVariable$3$STORE$0(double value) {
        if (ThirdPersonCamera.INSTANCE.isEnabled()) {
            return ThirdPersonCamera.INSTANCE.getDistance();
        } else {
            return value;
        }
    }

    @Inject(method = "displayItemActivation", at = @At(value = "HEAD"), cancellable = true)
    public void displayItemActivation(ItemStack stack, CallbackInfo ci) {
        if (AntiOverlay.INSTANCE.isEnabled() && AntiOverlay.INSTANCE.getTotems().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "setupFog", at = @At(value = "RETURN"), cancellable = true)
    public void setupFog(int startCoords, float partialTicks, CallbackInfo callbackInfo) {
        if (AntiFog.INSTANCE.isEnabled()) {
            GlStateManager.disableFog();
        }
    }

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    public void hurtCameraEffect(float ticks, CallbackInfo ci) {
        if (AntiOverlay.INSTANCE.isEnabled() && AntiOverlay.INSTANCE.getHurtCamera().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "getMouseOver", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getPositionEyes(F)Lnet/minecraft/util/math/Vec3d;", shift = At.Shift.BEFORE), cancellable = true)
    public void getEntitiesInAABBexcluding(float partialTicks, CallbackInfo ci) {
        if (BlockInteraction.isNoEntityTraceEnabled()) {
            ci.cancel();
            Wrapper.getMinecraft().profiler.endSection();
        }
    }

    @Inject(method = "orientCamera", at = @At("RETURN"))
    private void orientCameraStoreEyeHeight(float partialTicks, CallbackInfo ci) {
        if (!ElytraFlight.INSTANCE.shouldSwing()) return;
        Entity entity = Wrapper.getMinecraft().getRenderViewEntity();
        if (entity != null) {
            GlStateManager.translate(0.0f, entity.getEyeHeight() - 0.4f, 0.0f);
        }
    }

    @ModifyVariable(method = "getFOVModifier", at = @At(value = "STORE", ordinal = 1), ordinal = 1)
    public float getFOVModifier$STORE$float$1$1(float value) {
        return Fov.getFOVModifierDynamicFov(value);
    }

    @Inject(method = "getFOVModifier", at = @At(value = "RETURN", ordinal = 1), cancellable = true)
    public void getFOVModifier$Inject$RETURN(float partialTicks, boolean useFOVSetting, CallbackInfoReturnable<Float> cir) {
        if (useFOVSetting) {
            Fov.getFOVModifierNoDynamicFov(cir);
        }
    }

    @ModifyVariable(method = "updateRenderer", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    public float updateRenderer$STORE$float$0$0(float value) {
        return Fov.getMouseSensitivity(value);
    }

    @ModifyVariable(method = "updateCameraAndRender", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    public float updateCameraAndRender$STORE$float$0$0(float value) {
        return Fov.getMouseSensitivity(value);
    }
}
