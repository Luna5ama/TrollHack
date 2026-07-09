package dev.luna5ama.trollhack.mixins.world;

import dev.luna5ama.trollhack.modules.impl.movement.NoSlowDown;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class MixinBlock implements ItemLike {

    @Inject(at = @At("HEAD"), method = "getSpeedFactor", cancellable = true)
    private void onGetVelocityMultiplier(CallbackInfoReturnable<Float> cir) {
        if (!NoSlowDown.INSTANCE.isEnabled())
            return;
        if (cir.getReturnValueF() < 1.0f)
            cir.setReturnValue(1F);
    }

}