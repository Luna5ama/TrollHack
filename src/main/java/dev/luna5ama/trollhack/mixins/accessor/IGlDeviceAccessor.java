package dev.luna5ama.trollhack.mixins.accessor;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice")
public interface IGlDeviceAccessor {
    @Accessor("directStateAccess")
    DirectStateAccess acquireDirectStateAccess();
}
