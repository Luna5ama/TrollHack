package dev.luna5ama.trollhack.mixins;

import dev.luna5ama.trollhack.modules.impl.visual.FreeCamera;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandler {
    @Redirect(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
    )
    private void redirectFreeCameraLook(LocalPlayer player, double deltaX, double deltaY) {
        if (FreeCamera.INSTANCE.isEnabled()) {
            FreeCamera.changeLookDirection(deltaX, deltaY);
        } else {
            player.turn(deltaX, deltaY);
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void adjustFreeCameraSpeed(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (FreeCamera.adjustSpeed(vertical)) {
            ci.cancel();
        }
    }
}
