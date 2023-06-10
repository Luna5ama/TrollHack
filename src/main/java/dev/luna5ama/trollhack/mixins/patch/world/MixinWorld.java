package dev.luna5ama.trollhack.mixins.patch.world;

import dev.luna5ama.trollhack.util.world.RaytraceKt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(World.class)
public abstract class MixinWorld {
    protected MixinWorld() {
    }

    @Shadow
    public abstract IBlockState getBlockState(BlockPos pos);

    /**
     * @author Luna
     * @reason Raytrace optimization
     */
    @Overwrite
    @Nullable
    public RayTraceResult rayTraceBlocks(
        Vec3d vec31,
        Vec3d vec32,
        boolean stopOnLiquid,
        boolean ignoreBlockWithoutBoundingBox,
        boolean returnLastUncollidableBlock
    ) {
        return RaytraceKt.rayTrace(
            (World) (Object) this,
            vec31,
            vec32,
            stopOnLiquid,
            ignoreBlockWithoutBoundingBox,
            returnLastUncollidableBlock
        );
    }
}
