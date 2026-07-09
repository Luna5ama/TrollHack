package dev.luna5ama.trollhack.mixins.player;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.luna5ama.trollhack.event.impl.player.AttackEntityEvent;
import dev.luna5ama.trollhack.event.impl.player.PlayerTravelEvent;
import dev.luna5ama.trollhack.manager.managers.EntityMovementManager;
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings;
import dev.luna5ama.trollhack.modules.impl.visual.NameTags;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Player.class, priority = 800)
public abstract class MixinPlayer extends LivingEntity {
    protected MixinPlayer(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void attackAHook2(Entity target, CallbackInfo ci) {
        final AttackEntityEvent event = new AttackEntityEvent(target);
        event.post(event);
        if (event.getCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravelPre(Vec3 movementInput, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player != Minecraft.getInstance().player)
            return;

        PlayerTravelEvent.Pre event = new PlayerTravelEvent.Pre(player);
        event.post();
        if (event.getCancelled()) {
            move(MoverType.SELF, getDeltaMovement());
            ci.cancel();
        }
    }

    @Inject(method = "travel", at = @At("RETURN"), cancellable = true)
    private void onTravelPost(Vec3 movementInput, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player != Minecraft.getInstance().player)
            return;

        PlayerTravelEvent.Post event = new PlayerTravelEvent.Post(player);
        event.post();
        if (event.getCancelled()) {
            ci.cancel();
        }
    }

    @ModifyReturnValue(method = "isStayingOnGroundSurface", at = @At("RETURN"))
    private boolean hookSafeWalk(boolean original) {
        if(EntityMovementManager.INSTANCE.isSafeWalk()) {
            return EntityMovementManager.INSTANCE.isSafeWalk();
        }
        return original;
    }

    @ModifyReturnValue(method = "shouldShowName", at = @At(value = "RETURN"))
    public boolean shouldRenderName$Tweaker(boolean original) {
        return original && (!NameTags.INSTANCE.isEnabled());
    }
}
