package dev.luna5ama.trollhack.mixins.world;

import dev.luna5ama.trollhack.event.impl.world.WorldEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class MixinLevel {
    @Shadow @Final public boolean isClientSide;

    @Shadow public abstract BlockState getBlockState(BlockPos pos);

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    public void onBlockChange(BlockPos pos, BlockState newBlock, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        if (isClientSide) {
            var oldBlock = getBlockState(pos);
            var event = new WorldEvent.ClientBlockUpdate(pos, oldBlock, newBlock);
            event.post();
        }
    }
}
