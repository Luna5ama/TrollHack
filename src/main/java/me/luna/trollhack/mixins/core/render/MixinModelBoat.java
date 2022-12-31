package me.luna.trollhack.mixins.core.render;

import me.luna.trollhack.module.modules.movement.EntitySpeed;
import me.luna.trollhack.module.modules.movement.BoatFly;
import me.luna.trollhack.util.Wrapper;
import net.minecraft.client.model.ModelBoat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBoat.class)
public class MixinModelBoat {

    @Inject(method = "render", at = @At("HEAD"))
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo info) {
        if (Wrapper.getPlayer().getRidingEntity() == entityIn && EntitySpeed.INSTANCE.isEnabled()) {
            GlStateManager.color(1.0f, 1.0f, 1.0f, EntitySpeed.getOpacity());
            GlStateManager.enableBlend();
        }

        if (BoatFly.isBoatFlying(entityIn)) {
            GlStateManager.color(1.0f, 1.0f, 1.0f, BoatFly.INSTANCE.getOpacity());
            GlStateManager.enableBlend();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBoat;setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V"), cancellable = true)
    private void onRender(final Entity entityIn, final float limbSwing, final float limbSwingAmount, final float ageInTicks, final float netHeadYaw, final float headPitch, final float scale, final CallbackInfo ci) {
        if (BoatFly.shouldModifyScale(entityIn)) {
            final double size = BoatFly.INSTANCE.getSize();
            // In 3rd person this thing freaks out
            if (size != 1.0) {
                GlStateManager.scale(size, size, size);
            }
        }
    }

    @Inject(method = "renderMultipass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;colorMask(ZZZZ)V", ordinal = 0))
    private void onRenderMultipass(Entity entityIn, float partialTicks, float p_187054_3_, float p_187054_4_, float p_187054_5_, float p_187054_6_, float scale, CallbackInfo ci) {
        if (BoatFly.shouldModifyScale(entityIn)) {
            final double size = BoatFly.INSTANCE.getSize();
            if (size != 1.0) {
                GlStateManager.scale(size, size, size);
            }
        }
    }
}
