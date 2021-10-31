package cum.xiaro.trollhack.mixin.world;

import cum.xiaro.trollhack.module.modules.render.Xray;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelRenderer.class)
public class MixinBlockModelRenderer {
    @Inject(method = "renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z", at = @At("HEAD"), cancellable = true)
    public void renderModel(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn, BlockPos posIn, BufferBuilder buffer, boolean checkSides, long rand, CallbackInfoReturnable<Boolean> ci) {
        if (Xray.shouldReplace(stateIn)) {
            ci.cancel();
        }
    }
}
