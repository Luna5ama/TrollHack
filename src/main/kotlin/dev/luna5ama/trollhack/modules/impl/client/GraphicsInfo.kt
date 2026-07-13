package dev.luna5ama.trollhack.modules.impl.client

import com.mojang.blaze3d.systems.RenderSystem
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module

object GraphicsInfo : Module("Graphics Info", category = Category.CLIENT) {
    init {
        label("Backend", { RenderSystem.getDevice().backendName }, true)
        label("GPU Vendor", { RenderSystem.getDevice().vendor }, true)
        label("GPU Name", { RenderSystem.getDevice().renderer }, true)
        label("Driver Version", { RenderSystem.getDevice().version }, true)
        label("Maximum Texture Size", { RenderSystem.getDevice().maxTextureSize.toString() }, true)
        label("Maximum Anisotropy", { RenderSystem.getDevice().maxSupportedAnisotropy.toString() }, true)
        label("Debugging", { RenderSystem.getDevice().isDebuggingEnabled.toString() }, true)
    }
}
