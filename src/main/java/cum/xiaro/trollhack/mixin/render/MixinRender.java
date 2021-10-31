package cum.xiaro.trollhack.mixin.render;

import cum.xiaro.trollhack.module.modules.render.ESP;
import cum.xiaro.trollhack.module.modules.render.Nametags;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Render.class)
abstract class MixinRender<T extends Entity> {
    @Inject(method = "renderName", at = @At("HEAD"), cancellable = true)
    protected void renderName$Inject$HEAD(T entity, double x, double y, double z, CallbackInfo ci) {
        if (Nametags.INSTANCE.isEnabled() && Nametags.INSTANCE.checkEntityType(entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    public void getTeamColor$Inject$HEAD(T entityIn, CallbackInfoReturnable<Integer> cir) {
        Integer color = ESP.getEspColor(entityIn);
        if (color != null) cir.setReturnValue(color);
    }
}
