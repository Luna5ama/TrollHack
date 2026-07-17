package dev.luna5ama.trollhack.mixins.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luna5ama.trollhack.graphics.blaze3d.ShaderHolder;
import dev.luna5ama.trollhack.modules.impl.visual.Shaders;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChestRenderer.class)
public class MixinChestRenderer {
    @Redirect(
            method = "submit*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;IIILnet/minecraft/client/resources/model/sprite/SpriteId;Lnet/minecraft/client/resources/model/sprite/SpriteGetter;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
            )
    )
    private <S> void applyShadersChestOutline(
            SubmitNodeCollector submitNodeCollector,
            Model<S> model,
            S modelState,
            PoseStack poseStack,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            SpriteId sprite,
            SpriteGetter sprites,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
            ChestRenderState state
    ) {
        Shaders shaders = Shaders.INSTANCE;
        int finalOutlineColor = shaders.isEnabled() && Shaders.shouldRenderChest(state.blockPos)
                ? ShaderHolder.TROLLHACK_CHEST_OUTLINE_MARKER
                : outlineColor;
        submitNodeCollector.submitModel(
                model,
                modelState,
                poseStack,
                lightCoords,
                overlayCoords,
                tintedColor,
                sprite,
                sprites,
                finalOutlineColor,
                crumblingOverlay
        );
    }
}
