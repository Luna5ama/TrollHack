package dev.luna5ama.trollhack.mixins.render;

import dev.luna5ama.trollhack.modules.impl.visual.NoRender;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class MixinBackgroundRenderer {
    @Inject(method = "setupFog", at = @At("TAIL"), cancellable = true)
    private void onApplyFog(Camera camera, int renderDistance, DeltaTracker deltaTracker, float darkenWorldAmount, ClientLevel level, CallbackInfoReturnable<Vector4f> cir) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getFog()) {
            cir.setReturnValue(new Vector4f(0.0f, 0.0f, 0.0f, 0.0f));
        }
    }
}
