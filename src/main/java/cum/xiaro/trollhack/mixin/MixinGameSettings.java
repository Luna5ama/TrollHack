package cum.xiaro.trollhack.mixin;

import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameSettings.class)
public class MixinGameSettings {
    @Inject(method = "setOptionValue", at = @At("HEAD"), cancellable = true)
    public void setOptionValue$Inject$HEAD(GameSettings.Options settingsOption, int value, CallbackInfo ci) {
        if (settingsOption == GameSettings.Options.NARRATOR) {
            ci.cancel();
        }
    }
}
