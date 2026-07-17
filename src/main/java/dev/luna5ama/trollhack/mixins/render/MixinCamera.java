package dev.luna5ama.trollhack.mixins.render;

import dev.luna5ama.trollhack.modules.impl.visual.NoCameraClip;
import dev.luna5ama.trollhack.modules.impl.visual.AspectRatio;
import dev.luna5ama.trollhack.modules.impl.visual.FreeCamera;

import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Inject(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;calculateFov(F)F"
            )
    )
    private void applyFreeCameraBeforeFrustum(DeltaTracker deltaTracker, CallbackInfo ci) {
        Camera camera = (Camera) (Object) this;
        FreeCamera.INSTANCE.applyCamera(camera, camera.getCameraEntityPartialTicks(deltaTracker));
    }

    @ModifyArgs(
            method = "alignWithEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V")
    )
    private void smoothThirdPersonCamera(Args args) {
        NoCameraClip cameraClip = NoCameraClip.INSTANCE;
        if (!cameraClip.isEnabled() || !cameraClip.getAction()) {
            cameraClip.resetCameraPos();
            return;
        }
        if (Minecraft.getInstance().options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            cameraClip.resetCameraPos();
            return;
        }

        cameraClip.updateActionCamera(new Vec3(args.get(0), args.get(1), args.get(2)));
        Vec3 cameraPos = cameraClip.cameraPos();
        if (cameraPos != null) {
            args.set(0, cameraPos.x);
            args.set(1, cameraPos.y);
            args.set(2, cameraPos.z);
        }
    }

    @ModifyArgs(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setupPerspective(FFFFF)V")
    )
    private void adjustAspectRatio(Args args) {
        if (AspectRatio.INSTANCE.isEnabled()) {
            args.set(3, (float) args.get(4) * AspectRatio.getRatio());
        }
    }

    @ModifyArg(
            method = "createProjectionMatrixForCulling",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/joml/Matrix4f;perspective(FFFFZ)Lorg/joml/Matrix4f;"
            ),
            index = 1
    )
    private float adjustCullingAspectRatio(float vanillaRatio) {
        return AspectRatio.INSTANCE.isEnabled() ? AspectRatio.getRatio() : vanillaRatio;
    }

    @Inject(at = @At("HEAD"), method = "getMaxZoom(F)F", cancellable = true)
    private void onClipToSpace(float desiredCameraDistance,
                               CallbackInfoReturnable<Float> cir)
    {
        if (FreeCamera.INSTANCE.isEnabled()) {
            cir.setReturnValue(0.0f);
        } else if (NoCameraClip.INSTANCE.isEnabled()) {
            cir.setReturnValue(NoCameraClip.INSTANCE.getDistance());
        }
    }
}
