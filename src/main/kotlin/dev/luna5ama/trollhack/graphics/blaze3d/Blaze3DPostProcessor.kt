package dev.luna5ama.trollhack.graphics.blaze3d

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import dev.luna5ama.trollhack.Metadata
import dev.luna5ama.trollhack.modules.impl.visual.AntiAlias
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.DynamicUniformStorage
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import java.nio.ByteBuffer
import java.util.OptionalInt

object Blaze3DPostProcessor {
    private val fxaaPipeline: RenderPipeline by lazy {
        RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(id("pipeline/fxaa"))
            .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
            .withFragmentShader(id("core/fxaa"))
            .withUniform("FxaaInfo", UniformType.UNIFORM_BUFFER)
            .withSampler("InputSampler")
            .withCull(false)
            .build()
    }
    private var fxaaInput: TextureTarget? = null
    private val fxaaUniforms = DynamicUniformStorage<FxaaInfo>("trollhack-fxaa", 16, 1)

    fun apply(target: RenderTarget = Minecraft.getInstance().mainRenderTarget) {
        if (target.width <= 0 || target.height <= 0 || target.colorTextureView == null) return
        if (!AntiAlias.isEnabled) return
        try {
            renderFxaa(target)
        } finally {
            fxaaUniforms.endFrame()
        }
    }

    fun close() {
        fxaaInput?.destroyBuffers()
        fxaaInput = null
        fxaaUniforms.close()
    }

    private fun renderFxaa(target: RenderTarget) {
        val input = ensureInput(target)
        val sourceTexture = target.colorTexture ?: return
        val sourceView = target.colorTextureView ?: return
        val inputTexture = input.colorTexture ?: return
        val inputView = input.colorTextureView ?: return
        val encoder = RenderSystem.getDevice().createCommandEncoder()
        encoder.copyTextureToTexture(sourceTexture, inputTexture, 0, 0, 0, 0, 0, target.width, target.height)
        val fxaaInfo = fxaaUniforms.writeUniform(FxaaInfo(target.width.toFloat(), target.height.toFloat()))
        encoder.createRenderPass({ "TrollHack FXAA" }, sourceView, OptionalInt.empty()).use { pass ->
            pass.setPipeline(fxaaPipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("FxaaInfo", fxaaInfo)
            pass.bindTexture("InputSampler", inputView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            pass.draw(0, 3)
        }
    }

    private fun ensureInput(target: RenderTarget): TextureTarget {
        val current = fxaaInput
        if (current == null) {
            return TextureTarget(
                "TrollHack FXAA Input",
                target.width,
                target.height,
                false
            ).also {
                fxaaInput = it
            }
        }
        if (current.width != target.width || current.height != target.height) {
            current.resize(target.width, target.height)
        }
        return current
    }

    private fun id(path: String) = Identifier.fromNamespaceAndPath(Metadata.ID, path)

    private data class FxaaInfo(val width: Float, val height: Float) : DynamicUniformStorage.DynamicUniform {
        override fun write(buffer: ByteBuffer) {
            buffer.putFloat(width).putFloat(height).putFloat(1f / width).putFloat(1f / height)
        }
    }
}
