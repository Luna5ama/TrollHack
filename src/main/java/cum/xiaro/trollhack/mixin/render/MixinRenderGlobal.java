package cum.xiaro.trollhack.mixin.render;

import cum.xiaro.trollhack.event.events.render.RenderEntityEvent;
import cum.xiaro.trollhack.module.modules.player.Freecam;
import cum.xiaro.trollhack.module.modules.render.FastRender;
import cum.xiaro.trollhack.module.modules.render.SelectionHighlight;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = RenderGlobal.class, priority = Integer.MAX_VALUE)
public abstract class MixinRenderGlobal {
    @Shadow @Final private Map<Integer, DestroyBlockProgress> damagedBlocks;
    @Shadow private WorldClient world;
    @Shadow @Final private Minecraft mc;

    @Shadow
    protected abstract void preRenderDamagedBlocks();

    @Shadow
    protected abstract void postRenderDamagedBlocks();

    @Inject(method = "drawSelectionBox", at = @At("HEAD"), cancellable = true)
    public void drawSelectionBox(EntityPlayer player, RayTraceResult movingObjectPositionIn, int execute, float partialTicks, CallbackInfo ci) {
        if (SelectionHighlight.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderEntities", at = @At("HEAD"))
    public void renderEntitiesHead(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        RenderEntityEvent.setRenderingEntities(true);
    }

    @Inject(method = "renderEntities", at = @At("RETURN"))
    public void renderEntitiesReturn(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        RenderEntityEvent.setRenderingEntities(false);
    }

    @ModifyVariable(method = "setupTerrain", at = @At(value = "STORE", ordinal = 0), ordinal = 1)
    public BlockPos setupTerrainStoreFlooredChunkPosition(BlockPos playerPos) {
        if (Freecam.INSTANCE.isEnabled()) {
            playerPos = Freecam.getRenderChunkOffset(playerPos);
        }

        return playerPos;
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", ordinal = 3, shift = At.Shift.AFTER), cancellable = true)
    public void renderEntities$INVOKE$endStartSection$3$AFTER(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        if (FastRender.INSTANCE.isEnabled()) {
            ci.cancel();

            RenderHelper.enableStandardItemLighting();
            FastRender.renderTileEntities();

            this.preRenderDamagedBlocks();

            for (DestroyBlockProgress destroyblockprogress : this.damagedBlocks.values()) {
                BlockPos blockpos = destroyblockprogress.getPosition();
                IBlockState blockState = this.world.getBlockState(blockpos);

                if (blockState.getBlock().hasTileEntity(blockState)) {
                    TileEntity tileentity = this.world.getTileEntity(blockpos);

                    if (tileentity instanceof TileEntityChest) {
                        TileEntityChest tileentitychest = (TileEntityChest) tileentity;

                        if (tileentitychest.adjacentChestXNeg != null) {
                            blockpos = blockpos.offset(EnumFacing.WEST);
                            tileentity = this.world.getTileEntity(blockpos);
                            blockState = this.world.getBlockState(blockpos);
                        } else if (tileentitychest.adjacentChestZNeg != null) {
                            blockpos = blockpos.offset(EnumFacing.NORTH);
                            tileentity = this.world.getTileEntity(blockpos);
                            blockState = this.world.getBlockState(blockpos);
                        }
                    }

                    if (tileentity != null && blockState.hasCustomBreakingProgress()) {
                        TileEntityRendererDispatcher.instance.render(tileentity, partialTicks, destroyblockprogress.getPartialBlockDamage());
                    }
                }
            }

            this.postRenderDamagedBlocks();
            this.mc.entityRenderer.disableLightmap();
            this.mc.profiler.endSection();

            RenderEntityEvent.setRenderingEntities(false);
        }
    }
}
