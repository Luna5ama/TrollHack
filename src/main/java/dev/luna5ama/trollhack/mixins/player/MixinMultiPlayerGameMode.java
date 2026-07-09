package dev.luna5ama.trollhack.mixins.player;


import dev.luna5ama.trollhack.event.impl.player.PlayerClickBlockEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos posBlock, Direction directionFacing, CallbackInfoReturnable<Boolean> cir) {
        PlayerClickBlockEvent event = new PlayerClickBlockEvent(posBlock, directionFacing);
        event.post();
        if (event.getCancelled()) cir.setReturnValue(false);
    }
}
