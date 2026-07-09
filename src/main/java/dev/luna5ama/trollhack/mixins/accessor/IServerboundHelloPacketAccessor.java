package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundHelloPacket.class)
public interface IServerboundHelloPacketAccessor {
    @Mutable
    @Accessor("name")
    void setName(String name);
}