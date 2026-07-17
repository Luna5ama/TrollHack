package dev.luna5ama.trollhack.mixins.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.luna5ama.trollhack.modules.impl.visual.Filter;
import dev.luna5ama.trollhack.modules.impl.visual.FullBright;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Lightmap.class)
public class MixinLightmap {
    @Final
    @Shadow
    private GpuTexture texture;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void applyFullbrightAndFilter(LightmapRenderState renderState, CallbackInfo ci) {
        if (!FullBright.isGammaMode() && !Filter.INSTANCE.isLightMapMode()) return;

        int color = Filter.INSTANCE.isLightMapMode() ? Filter.lightMapArgb() : -1;
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(texture, color);
        ci.cancel();
    }
}
