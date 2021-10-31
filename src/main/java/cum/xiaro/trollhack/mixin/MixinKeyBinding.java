package cum.xiaro.trollhack.mixin;

import cum.xiaro.trollhack.module.modules.player.BetterEat;
import cum.xiaro.trollhack.util.Wrapper;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBinding.class)
public class MixinKeyBinding {

    @Shadow private boolean pressed;

    // Fixes AutoEat gets cancelled in GUI
    @SuppressWarnings("ConstantConditions")
    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    public void isKeyDownHead(CallbackInfoReturnable<Boolean> cir) {
        EntityPlayerSP player = Wrapper.getPlayer();
        if (player != null
            && BetterEat.shouldCancelStopUsingItem()
            && ((Object) this) == Wrapper.getMinecraft().gameSettings.keyBindUseItem) {
            cir.setReturnValue(this.pressed);
        }
    }

}
