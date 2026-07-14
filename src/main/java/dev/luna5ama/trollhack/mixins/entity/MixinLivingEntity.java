package dev.luna5ama.trollhack.mixins.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.luna5ama.trollhack.event.impl.player.PlayerJumpEvent;
import dev.luna5ama.trollhack.manager.managers.RotationManager;
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings;
import dev.luna5ama.trollhack.modules.impl.movement.MovementFix;
import dev.luna5ama.trollhack.modules.impl.movement.MovementFixJumpAdapter;
import dev.luna5ama.trollhack.modules.impl.visual.NameTags;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    @ModifyReturnValue(method = "shouldShowName", at = @At(value = "RETURN"))
    public boolean shouldRenderName$Tweaker(boolean original) {
        return original && (!NameTags.INSTANCE.isEnabled());
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void onJumpPre(CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            if (player != Minecraft.getInstance().player)
                return;
            PlayerJumpEvent.Pre.INSTANCE.post();
        }
    }

    @Inject(method = "jumpFromGround", at = @At("RETURN"))
    private void onJumpPost(CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            if (player != Minecraft.getInstance().player)
                return;
            PlayerJumpEvent.Post.INSTANCE.post();
        }
    }

    @ModifyExpressionValue(
            method = "jumpFromGround",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F")
    )
    private float trollhack$movementFixJumpYaw(float originalYaw) {
        LivingEntity entity = (LivingEntity) (Object) this;
        Minecraft minecraft = Minecraft.getInstance();
        return MovementFixJumpAdapter.yaw(
                originalYaw,
                RotationManager.INSTANCE.getYaw(),
                entity == minecraft.player,
                MovementFix.INSTANCE.isEnabled(),
                RotationManager.INSTANCE.isActive(),
                entity.isFallFlying()
        );
    }
}
