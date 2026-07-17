package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface ICameraAccessor {
    @Invoker("setPosition")
    void trollhack$setPosition(Vec3 position);

    @Invoker("setRotation")
    void trollhack$setRotation(float yaw, float pitch);
}
