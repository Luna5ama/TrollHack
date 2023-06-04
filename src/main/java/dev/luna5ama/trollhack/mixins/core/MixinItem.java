package dev.luna5ama.trollhack.mixins.core;

import dev.luna5ama.trollhack.module.modules.player.HandSwing;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class MixinItem {
    @Inject(method = "shouldCauseReequipAnimation", at = @At("HEAD"), cancellable = true, remap = false)
    private void shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged, CallbackInfoReturnable<Boolean> cir) {
        if (HandSwing.INSTANCE.isDisabled() || !HandSwing.INSTANCE.getCancelEquipAnimation()) return;

        cir.setReturnValue(slotChanged && !oldStack.equals(newStack));
    }
}
