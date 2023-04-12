package dev.luna5ama.trollhack.mixins.patch.player;

import dev.luna5ama.trollhack.util.Wrapper;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerControllerMP.class)
public abstract class MixinPlayerControllerMP {
    @Inject(method = "syncCurrentPlayItem", at = @At("HEAD"), cancellable = true)
    private void Inject$syncCurrentPlayItem$HEAD(CallbackInfo ci) {
        if (Wrapper.getPlayer() == null) ci.cancel();
    }
}
