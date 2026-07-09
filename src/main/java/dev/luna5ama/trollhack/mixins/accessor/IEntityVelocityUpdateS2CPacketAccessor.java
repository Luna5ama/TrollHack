package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author CuteMic
 */
@Mixin(ClientboundSetEntityMotionPacket.class)
public interface IEntityVelocityUpdateS2CPacketAccessor {

    @Mutable
    @Accessor("movement")
    void setMovement(Vec3 movement);
}
