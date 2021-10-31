package cum.xiaro.trollhack.mixin.world;

import cum.xiaro.trollhack.event.events.AddCollisionBoxEvent;
import cum.xiaro.trollhack.module.modules.render.Xray;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BlockStateContainer.StateImplementation.class)
public class MixinStateImplementation {
    @Shadow @Final private Block block;

    @Inject(method = "addCollisionBoxToList", at = @At("HEAD"))
    public void addCollisionBoxToList(World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entityIn, boolean isActualState, CallbackInfo ci) {
        new AddCollisionBoxEvent(entityIn, entityBox, pos, block, collidingBoxes).post();
    }

    @Inject(method = "shouldSideBeRendered", at = @At("HEAD"), cancellable = true)
    public void shouldSideBeRendered(IBlockAccess blockAccess, BlockPos pos, EnumFacing facing, CallbackInfoReturnable<Boolean> ci) {
        if (Xray.shouldReplace(blockAccess.getBlockState(pos.offset(facing)))) {
            ci.setReturnValue(true);
        }
    }
}
