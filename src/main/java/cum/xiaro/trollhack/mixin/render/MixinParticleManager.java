package cum.xiaro.trollhack.mixin.render;

import cum.xiaro.trollhack.module.modules.render.NoRender;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class MixinParticleManager {
    @Inject(method = "addEffect", at = @At("HEAD"), cancellable = true)
    public void addEffect(Particle effect, CallbackInfo ci) {
        if (NoRender.shouldHideParticles(effect)) {
            ci.cancel();
        }
    }
}
