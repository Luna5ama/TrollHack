package dev.luna5ama.trollhack.mixins.world;

import dev.luna5ama.trollhack.modules.impl.movement.NoSlowDown;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WebBlock.class)
public class MixinCobwebBlock {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void onEntityCollision(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean bl, CallbackInfo ci) {
        if (NoSlowDown.INSTANCE.isEnabled() && NoSlowDown.INSTANCE.getWeb()
                && Minecraft.getInstance().player != null && entity == Minecraft.getInstance().player) {
            ci.cancel();
            entity.makeStuckInBlock(state,
                    new Vec3(
                            NoSlowDown.INSTANCE.getHorizontalSpeed(),
                            NoSlowDown.INSTANCE.getVerticalSpeed(),
                            NoSlowDown.INSTANCE.getHorizontalSpeed()
                    )
            );
        }
    }
}
