package cum.xiaro.trollhack.mixin.world;

import cum.xiaro.trollhack.module.modules.misc.AntiWeather;
import cum.xiaro.trollhack.module.modules.render.NoRender;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = World.class, priority = Integer.MAX_VALUE)
public class MixinWorld {
    @Inject(method = "checkLightFor", at = @At("HEAD"), cancellable = true)
    private void checkLightForHead(EnumSkyBlock lightType, BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        if (NoRender.handleLighting(lightType)) {
            ci.setReturnValue(false);
        }
    }

    @Inject(method = "getThunderStrength", at = @At("HEAD"), cancellable = true)
    private void getThunderStrengthHead(float delta, CallbackInfoReturnable<Float> cir) {
        if (AntiWeather.INSTANCE.isEnabled()) {
            cir.setReturnValue(0.0f);
        }
    }

    @Inject(method = "getRainStrength", at = @At("HEAD"), cancellable = true)
    private void getRainStrengthHead(float delta, CallbackInfoReturnable<Float> cir) {
        if (AntiWeather.INSTANCE.isEnabled()) {
            cir.setReturnValue(0.0f);
        }
    }
}
