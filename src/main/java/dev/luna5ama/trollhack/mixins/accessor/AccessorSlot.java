package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Slot.class)
public interface AccessorSlot {
    @Invoker("onSwapCraft")
    void trollOnSwapCraft(int p_190900_1_);
}
