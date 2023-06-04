package dev.luna5ama.trollhack.mixins.accessor.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketClickWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CPacketClickWindow.class)
public interface AccessorCPacketClickWindow {
    @Accessor("clickedItem")
    void trollSetClickedItem(ItemStack clickedItem);
}
