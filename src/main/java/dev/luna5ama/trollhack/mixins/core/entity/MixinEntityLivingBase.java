package dev.luna5ama.trollhack.mixins.core.entity;

import dev.luna5ama.trollhack.module.modules.player.HandSwing;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityLivingBase.class, priority = Integer.MAX_VALUE)
public abstract class MixinEntityLivingBase {
    @Inject(method = "getArmSwingAnimationEnd", at = @At("HEAD"), cancellable = true)
    private void getArmSwingAnimationEnd(CallbackInfoReturnable<Integer> cir) {
        if (HandSwing.INSTANCE.getModifiedSwingSpeed()) {
            cir.setReturnValue(HandSwing.INSTANCE.getSwingTicks());
        }
    }
}
