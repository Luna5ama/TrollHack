package cum.xiaro.trollhack.mixin.player;

import cum.xiaro.trollhack.event.events.player.InteractEvent;
import cum.xiaro.trollhack.event.events.player.PlayerAttackEvent;
import cum.xiaro.trollhack.module.modules.player.BetterEat;
import cum.xiaro.trollhack.module.modules.player.FastBreak;
import cum.xiaro.trollhack.module.modules.player.FastUse;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PlayerControllerMP.class)
public abstract class MixinPlayerControllerMP {
    @Shadow @Final private Minecraft mc;
    @Shadow private float curBlockDamageMP;
    @Shadow private boolean isHittingBlock;
    @Shadow private BlockPos currentBlock;
    @Shadow private ItemStack currentItemHittingBlock;
    @Shadow private float stepSoundTickCounter;
    @Shadow private GameType currentGameType;
    @Shadow @Final private NetHandlerPlayClient connection;

    @Shadow
    public abstract boolean onPlayerDestroyBlock(BlockPos pos);

    @Shadow
    protected abstract void syncCurrentPlayItem();

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    public void attackEntity(EntityPlayer playerIn, Entity targetEntity, CallbackInfo ci) {
        if (targetEntity == null) return;
        PlayerAttackEvent event = new PlayerAttackEvent(targetEntity);
        event.post();

        if (event.getCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onStoppedUsingItem", at = @At("HEAD"), cancellable = true)
    public void onStoppedUsingItem$INJECT$HEAD(EntityPlayer playerIn, CallbackInfo ci) {
        if (BetterEat.shouldCancelStopUsingItem()) ci.cancel();
    }

    @Inject(method = "clickBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;", ordinal = 2), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    public void clickBlock$Inject$INVOKE$getBlockState(BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir, PlayerInteractEvent.LeftClickBlock forgeEvent) {
        InteractEvent.Block.LeftClick event = new InteractEvent.Block.LeftClick(pos, side);
        event.post();

        if (event.getCancelled()) {
            IBlockState blockState = this.mc.world.getBlockState(pos);
            this.mc.getTutorial().onHitBlock(this.mc.world, pos, blockState, 0.0F);
            boolean flag = blockState.getMaterial() != Material.AIR;

            if (flag && this.curBlockDamageMP == 0.0F) {
                if (forgeEvent.getUseBlock() != Event.Result.DENY) {
                    blockState.getBlock().onBlockClicked(this.mc.world, pos, this.mc.player);
                }
            }

            if (forgeEvent.getUseItem() == Event.Result.DENY) {
                cir.setReturnValue(true);
                return;
            }

            if (flag && blockState.getPlayerRelativeBlockHardness(this.mc.player, this.mc.player.world, pos) >= 1.0F) {
                this.onPlayerDestroyBlock(pos);
            } else {
                this.isHittingBlock = true;
                this.currentBlock = pos;
                this.currentItemHittingBlock = this.mc.player.getHeldItemMainhand();
                this.curBlockDamageMP = 0.0F;
                this.stepSoundTickCounter = 0.0F;
                this.mc.world.sendBlockBreakProgress(this.mc.player.getEntityId(), this.currentBlock, (int) (this.curBlockDamageMP * 10.0F) - 1);
            }

            cir.setReturnValue(true);
        }
    }

    @Inject(method = "clickBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;blockHitDelay:I", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    public void clickBlock$Inject$FIELD$blockHitDelay$PUTFIELD(BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir) {
        FastBreak.updateBreakDelay();
    }

    @Inject(method = "onPlayerDamageBlock", at = @At("HEAD"), cancellable = true)
    public void onPlayerDamageBlock$Inject$HEAD(BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir) {
        InteractEvent.Block.Damage event = new InteractEvent.Block.Damage(pos, side);
        event.post();

        if (event.getCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onPlayerDamageBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;blockHitDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 1, shift = At.Shift.AFTER))
    public void onPlayerDamageBlock$Inject$FIELD$blockHitDelay$PUTFIELD$1(BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir) {
        FastBreak.updateBreakDelay();
    }

    @Inject(method = "onPlayerDamageBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;blockHitDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 2, shift = At.Shift.AFTER))
    public void onPlayerDamageBlock$Inject$FIELD$blockHitDelay$PUTFIELD$2(BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir) {
        FastBreak.updateBreakDelay();
    }


    @Inject(method = "processRightClickBlock", at = @At("HEAD"), cancellable = true)
    public void processRightClickBlock$Inject$HEAD(EntityPlayerSP player, WorldClient worldIn, BlockPos pos, EnumFacing direction, Vec3d vec, EnumHand hand, CallbackInfoReturnable<EnumActionResult> cir) {
        InteractEvent.Block.RightClick event = new InteractEvent.Block.RightClick(pos, direction);
        event.post();

        if (event.getCancelled()) {
            cir.setReturnValue(EnumActionResult.PASS);
        }
    }

    @Inject(method = "processRightClick", at = @At("HEAD"), cancellable = true)
    public void processRightClick$Inject$HEAD(EntityPlayer player, World worldIn, EnumHand hand, CallbackInfoReturnable<EnumActionResult> cir) {
        int count;
        if (FastUse.INSTANCE.isDisabled() || (count = FastUse.INSTANCE.getMultiUse()) == 1) return;

        if (this.currentGameType == GameType.SPECTATOR) {
            cir.setReturnValue(EnumActionResult.PASS);
        } else {
            for (int use = 1; use < count; use++) {
                this.syncCurrentPlayItem();
                this.connection.sendPacket(new CPacketPlayerTryUseItem(hand));
                ItemStack itemstack = player.getHeldItem(hand);

                if (player.getCooldownTracker().hasCooldown(itemstack.getItem())) {
                    cir.setReturnValue(EnumActionResult.PASS);
                    return;
                } else {
                    EnumActionResult cancelResult = net.minecraftforge.common.ForgeHooks.onItemRightClick(player, hand);
                    if (cancelResult != null) {
                        cir.setReturnValue(cancelResult);
                        return;
                    }
                    int i = itemstack.getCount();
                    ActionResult<ItemStack> actionResult = itemstack.useItemRightClick(worldIn, player, hand);
                    ItemStack itemStack1 = actionResult.getResult();

                    if (itemStack1 != itemstack || itemStack1.getCount() != i) {
                        player.setHeldItem(hand, itemStack1);
                        if (itemStack1.isEmpty()) {
                            net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player, itemstack, hand);
                        }
                    }

                    if (actionResult.getType() != EnumActionResult.SUCCESS) {
                        cir.setReturnValue(actionResult.getType());
                        return;
                    }
                }
            }
        }
    }
}
