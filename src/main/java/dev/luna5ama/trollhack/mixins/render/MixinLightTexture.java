package dev.luna5ama.trollhack.mixins.render;

import dev.luna5ama.trollhack.modules.impl.visual.FullBright;
import dev.luna5ama.trollhack.modules.impl.visual.NoRender;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LightTexture.class)
public class MixinLightTexture {
    @ModifyArgs(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/AbstractUniform;set(Lorg/joml/Vector3f;)V"), require = 0)
    private void update(Args args) {
        if (FullBright.INSTANCE.isEnabled()) {
            var vec = new Vector3f(FullBright.INSTANCE.getRgb().getRed(), FullBright.INSTANCE.getRgb().getGreen(), FullBright.INSTANCE.getRgb().getBlue());
            args.set(0, vec);
        }
    }

    @Inject(method = "calculateDarknessScale", at = @At("HEAD"), cancellable = true)
    private void getDarknessFactor(LivingEntity entity, float darknessScale, float tickDelta, CallbackInfoReturnable<Float> info) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getDarkness()) info.setReturnValue(0.0f);
    }
}
