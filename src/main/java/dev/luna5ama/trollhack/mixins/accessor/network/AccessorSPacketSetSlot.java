package dev.luna5ama.trollhack.mixins.accessor.network;

import net.minecraft.network.play.server.SPacketSetSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SPacketSetSlot.class)
public interface AccessorSPacketSetSlot {
    @Accessor("windowId")
    void trollSetWindowId(int windowId);

    @Accessor("slot")
    void trollSetSlot(int slot);
}
