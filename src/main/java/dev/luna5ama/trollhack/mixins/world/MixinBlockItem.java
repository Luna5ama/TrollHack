package dev.luna5ama.trollhack.mixins.world;

import dev.luna5ama.trollhack.event.impl.world.PlaceBlockEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class MixinBlockItem {
    @Inject(method = "placeBlock", at = @At("RETURN"))
    private void onPlace(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        PlaceBlockEvent event = new PlaceBlockEvent(context.getClickedPos(),state.getBlock());
        if (context.getLevel().isClientSide()) event.post();
    }
}
