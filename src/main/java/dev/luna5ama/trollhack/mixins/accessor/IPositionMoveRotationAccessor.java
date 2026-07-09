package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.world.entity.PositionMoveRotation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PositionMoveRotation.class)
public interface IPositionMoveRotationAccessor {
    @Mutable
    @Accessor("xRot")
    void setXRot(float value);

    @Mutable
    @Accessor("yRot")
    void setYRot(float value);
}
