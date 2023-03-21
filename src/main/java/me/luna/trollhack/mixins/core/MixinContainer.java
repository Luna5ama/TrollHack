package me.luna.trollhack.mixins.core;

import me.luna.trollhack.module.modules.player.InventorySync;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Container.class)
public class MixinContainer {
    @Inject(method = "slotClick", at = @At("HEAD"))
    public void Inject$slotClick$HEAD(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {
        InventorySync.handleSlotClick((Container) (Object) this, dragType, slotId, clickTypeIn);
    }
}
