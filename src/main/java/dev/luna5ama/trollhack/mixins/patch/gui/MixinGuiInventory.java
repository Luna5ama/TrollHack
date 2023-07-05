package dev.luna5ama.trollhack.mixins.patch.gui;

import dev.luna5ama.trollhack.graphics.RenderUtils3D;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiInventory.class)
public class MixinGuiInventory {
    @Unique
    private static float trollHack$prevRotationYaw = 0.0f;
    @Unique
    private static float trollHack$prevRotationPitch = 0.0f;
    @Unique
    private static float trollHack$prevRenderYawOffset = 0.0f;

    @Inject(method = "drawEntityOnScreen", at = @At("HEAD"))
    private static void Inject$drawEntityOnScreen$HEAD(
        int posX,
        int posY,
        int scale,
        float mouseX,
        float mouseY,
        EntityLivingBase entity,
        CallbackInfo ci
    ) {
        trollHack$prevRotationYaw = entity.prevRotationYaw;
        trollHack$prevRotationPitch = entity.prevRotationPitch;
        trollHack$prevRenderYawOffset = entity.prevRenderYawOffset;
    }

    @Inject(method = "drawEntityOnScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderEntity(Lnet/minecraft/entity/Entity;DDDFFZ)V", shift = At.Shift.BEFORE))
    private static void drawEntityOnScreenInvokeRenderEntityPre(
        int posX,
        int posY,
        int scale,
        float mouseX,
        float mouseY,
        EntityLivingBase entity,
        CallbackInfo ci
    ) {
        entity.prevRotationYaw = entity.rotationYaw;
        entity.prevRotationPitch = entity.rotationPitch;
        entity.prevRenderYawOffset = entity.renderYawOffset;
    }

    @ModifyArg(method = "drawEntityOnScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderEntity(Lnet/minecraft/entity/Entity;DDDFFZ)V"), index = 5)
    private static float drawEntityOnScreenInvokeRenderEntityPartialTicks(float partialTicks) {
        return RenderUtils3D.getPartialTicks();
    }

    @Inject(method = "drawEntityOnScreen", at = @At("RETURN"))
    private static void Inject$drawEntityOnScreen$RETURN(
        int posX,
        int posY,
        int scale,
        float mouseX,
        float mouseY,
        EntityLivingBase entity,
        CallbackInfo ci
    ) {
        entity.prevRotationYaw = trollHack$prevRotationYaw;
        entity.prevRotationPitch = trollHack$prevRotationPitch;
        entity.prevRenderYawOffset = trollHack$prevRenderYawOffset;
    }
}
