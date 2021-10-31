package cum.xiaro.trollhack.mixin.render;

import cum.xiaro.trollhack.module.modules.player.Freecam;
import cum.xiaro.trollhack.module.modules.render.Xray;
import cum.xiaro.trollhack.util.Wrapper;
import cum.xiaro.trollhack.util.graphics.RenderUtils3D;
import cum.xiaro.trollhack.util.math.vector.ConversionKt;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumSet;
import java.util.Set;

@Mixin(VisGraph.class)
public class MixinVisGraph {
    @Inject(method = "getVisibleFacings", at = @At("HEAD"), cancellable = true)
    public void getVisibleFacings(CallbackInfoReturnable<Set<EnumFacing>> ci) {
        if (Freecam.INSTANCE.isDisabled()) return;

        WorldClient world = Wrapper.getWorld();
        if (world == null) return;

        // Only do the hacky cave culling fix if inside of a block
        Vec3d camPos = RenderUtils3D.INSTANCE.getCamPos();
        BlockPos blockPos = ConversionKt.toBlockPos(camPos);
        if (world.getBlockState(blockPos).isFullBlock()) {
            ci.setReturnValue(EnumSet.allOf(EnumFacing.class));
        }
    }

    @Inject(method = "setOpaqueCube", at = @At("HEAD"), cancellable = true)
    public void setOpaqueCube(BlockPos pos, CallbackInfo ci) {
        if (Xray.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }
}
