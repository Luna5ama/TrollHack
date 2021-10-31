package cum.xiaro.trollhack.mixin.entity;

import cum.xiaro.trollhack.module.modules.movement.SafeWalk;
import cum.xiaro.trollhack.module.modules.movement.Sprint;
import cum.xiaro.trollhack.module.modules.movement.Velocity;
import cum.xiaro.trollhack.module.modules.player.Freecam;
import cum.xiaro.trollhack.module.modules.player.GhostHand;
import cum.xiaro.trollhack.module.modules.player.ViewLock;
import cum.xiaro.trollhack.util.Wrapper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Entity.class, priority = Integer.MAX_VALUE)
public abstract class MixinEntity {
    @Shadow private int entityId;

    private boolean modifiedSneaking = false;

    @Inject(method = "applyEntityCollision", at = @At("HEAD"), cancellable = true)
    public void applyEntityCollisionHead(Entity entityIn, CallbackInfo ci) {
        Velocity.handleApplyEntityCollision((Entity) (Object) this, entityIn, ci);
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSneaking()Z", ordinal = 0, shift = At.Shift.BEFORE))
    public void moveInvokeIsSneakingPre(MoverType type, double x, double y, double z, CallbackInfo ci) {
        if (SafeWalk.shouldSafewalk(this.entityId, x, z)) {
            modifiedSneaking = true;
            SafeWalk.setSneaking(true);
        }
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSneaking()Z", ordinal = 0, shift = At.Shift.AFTER))
    public void moveInvokeIsSneakingPost(MoverType type, double x, double y, double z, CallbackInfo ci) {
        if (modifiedSneaking) {
            modifiedSneaking = false;
            SafeWalk.setSneaking(false);
        }
    }

    // Makes the camera guy instead of original player turn around when we move mouse
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    public void turn(float yaw, float pitch, CallbackInfo ci) {
        Entity casted = (Entity) (Object) this;

        if (Freecam.handleTurn(casted, yaw, pitch, ci)) return;
        ViewLock.handleTurn(casted, yaw, pitch, ci);
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "rayTrace", at = @At("HEAD"), cancellable = true)
    public void rayTrace$Inject$INVOKE$rayTraceBlocks(double blockReachDistance, float partialTicks, CallbackInfoReturnable<RayTraceResult> cir) {
        if ((Object) this == Wrapper.getPlayer()) {
            GhostHand.handleRayTrace(blockReachDistance, partialTicks, cir);
        }
    }

    @Inject(method = "isSprinting", at = @At("RETURN"), cancellable = true)
    public void isSprinting$Inject$RETURN(CallbackInfoReturnable<Boolean> cir) {
        //noinspection ConstantConditions
        if ((Object) this == Wrapper.getPlayer() && !cir.getReturnValue()) {
            cir.setReturnValue(Sprint.shouldSprint());
        }
    }
}
