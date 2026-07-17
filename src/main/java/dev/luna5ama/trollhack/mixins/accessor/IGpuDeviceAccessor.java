package dev.luna5ama.trollhack.mixins.accessor;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GpuDevice.class)
public interface IGpuDeviceAccessor {
    @Accessor("backend")
    GpuDeviceBackend acquireBackend();
}
