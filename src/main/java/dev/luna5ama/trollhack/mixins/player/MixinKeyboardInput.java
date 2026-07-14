package dev.luna5ama.trollhack.mixins.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.luna5ama.trollhack.event.impl.player.InputUpdateEvent;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {
    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
    private Input trollhack$onInput(Input original) {
        ClientInput input = (ClientInput) (Object) this;
        input.keyPresses = original;

        InputUpdateEvent event = new InputUpdateEvent(input);
        event.post();
        return event.getCancelled() ? original : input.keyPresses;
    }
}
