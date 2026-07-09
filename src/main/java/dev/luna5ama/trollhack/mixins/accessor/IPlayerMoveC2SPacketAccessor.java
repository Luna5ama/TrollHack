package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface IPlayerMoveC2SPacketAccessor {
    @Mutable
    @Accessor("onGround")
    void setOnGround(boolean onGround);

    @Mutable
    @Accessor("xRot")
    void setPitch(float pitch);

    @Mutable
    @Accessor("yRot")
    void setYaw(float yaw);

    @Mutable
    @Accessor("x")
    void setX(double x);

    @Mutable
    @Accessor("y")
    void setY(double Y);

    @Mutable
    @Accessor("z")
    void setZ(double Z);



    @Mutable
    @Accessor("x")
    double getX();

    @Mutable
    @Accessor("y")
    double getY();

    @Mutable
    @Accessor("z")
    double getZ();

    @Mutable
    @Accessor("yRot")
    float getYaw();

    @Mutable
    @Accessor("xRot")
    float getPitch();


    @Mutable
    @Accessor("onGround")
    boolean getOnGround();

    @Mutable
    @Accessor("hasPos")
    boolean getChangePosition();

    @Mutable
    @Accessor("hasRot")
    boolean getChangeLook();

}
