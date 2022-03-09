package me.luna.trollhack.util.graphics

import me.luna.trollhack.TrollHackMod
import me.luna.trollhack.event.AlwaysListening
import me.luna.trollhack.event.events.TickEvent
import me.luna.trollhack.event.events.render.ResolutionUpdateEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.util.Wrapper
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.shader.ShaderGroup
import net.minecraft.client.shader.ShaderLinkHelper
import net.minecraft.util.ResourceLocation

class ShaderHelper(shaderIn: ResourceLocation) : AlwaysListening {
    private val mc = Wrapper.minecraft

    val shader: ShaderGroup? =
        if (!OpenGlHelper.shadersSupported) {
            TrollHackMod.logger.warn("Shaders are unsupported by OpenGL!")
            null
        } else {
            try {
                ShaderLinkHelper.setNewStaticShaderLinkHelper()

                ShaderGroup(mc.textureManager, mc.resourceManager, mc.framebuffer, shaderIn).also {
                    it.createBindFramebuffers(mc.displayWidth, mc.displayHeight)
                }
            } catch (e: Exception) {
                TrollHackMod.logger.warn("Failed to load shaders")
                e.printStackTrace()

                null
            }
        }

    private var frameBuffersInitialized = false

    init {
        listener<TickEvent.Post> {
            if (!frameBuffersInitialized) {
                shader?.createBindFramebuffers(mc.displayWidth, mc.displayHeight)

                frameBuffersInitialized = true
            }
        }

        listener<ResolutionUpdateEvent> {
            shader?.createBindFramebuffers(it.width, it.height) // this will not run if on Intel GPU or unsupported Shaders
        }
    }

    fun getFrameBuffer(name: String) = shader?.getFramebufferRaw(name)
}