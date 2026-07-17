package dev.luna5ama.trollhack.mixins.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.luna5ama.trollhack.graphics.blaze3d.ShaderHolder;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OutputTarget.class)
public class MixinOutputTarget {
    @Inject(method = "getRenderTarget", at = @At("HEAD"), cancellable = true)
    private void redirectShaderOutlineTarget(CallbackInfoReturnable<RenderTarget> cir) {
        if ((OutputTarget) (Object) this != OutputTarget.OUTLINE_TARGET) return;

        RenderTarget chestTarget = ShaderHolder.getChestOutlineTarget();
        if (chestTarget != null) {
            cir.setReturnValue(chestTarget);
            return;
        }

        RenderTarget handTarget = ShaderHolder.getHandOutlineTarget();
        if (handTarget != null) cir.setReturnValue(handTarget);
    }
}
