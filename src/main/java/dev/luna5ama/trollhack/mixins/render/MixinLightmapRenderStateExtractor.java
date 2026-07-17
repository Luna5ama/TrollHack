package dev.luna5ama.trollhack.mixins.render;

import dev.luna5ama.trollhack.modules.impl.visual.NoRender;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapRenderStateExtractor.class)
public class MixinLightmapRenderStateExtractor {
    @Inject(method = "calculateDarknessScale", at = @At("HEAD"), cancellable = true)
    private void removeDarkness(
            LivingEntity entity,
            float darknessScale,
            float tickDelta,
            CallbackInfoReturnable<Float> cir
    ) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getDarkness()) {
            cir.setReturnValue(0.0f);
        }
    }
}
