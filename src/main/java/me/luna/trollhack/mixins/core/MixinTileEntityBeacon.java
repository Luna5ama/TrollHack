package me.luna.trollhack.mixins.core;

import me.luna.trollhack.module.modules.render.NoRender;
import net.minecraft.tileentity.TileEntityBeacon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TileEntityBeacon.class)
public class MixinTileEntityBeacon {
    @Inject(method = "shouldBeamRender", at = @At("HEAD"), cancellable = true)
    public void shouldBeamRender(CallbackInfoReturnable<Float> returnable) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getBeaconBeams()) {
            returnable.setReturnValue(0.0F);
            returnable.cancel();
        }
    }
}
