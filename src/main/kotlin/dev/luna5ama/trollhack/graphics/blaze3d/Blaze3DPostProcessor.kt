package dev.luna5ama.trollhack.graphics.blaze3d

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import dev.luna5ama.trollhack.Metadata
import dev.luna5ama.trollhack.modules.impl.visual.AntiAlias
import dev.luna5ama.trollhack.modules.impl.visual.Filter
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
    private val filterPipeline: RenderPipeline by lazy {
        RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(id("pipeline/filter"))
            .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
            .withFragmentShader(id("core/filter"))
            .withUniform("FilterColor", UniformType.UNIFORM_BUFFER)
            .withSampler("InputSampler")
            .withCull(false)
            .build()
    }
    private var fxaaInput: TextureTarget? = null
    private var filterInput: TextureTarget? = null
    private val fxaaUniforms = DynamicUniformStorage<FxaaInfo>("trollhack-fxaa", 16, 1)
    private val filterUniforms = DynamicUniformStorage<FilterColor>("trollhack-filter", 16, 1)

    fun apply(target: RenderTarget = Minecraft.getInstance().mainRenderTarget) {
        if (target.width <= 0 || target.height <= 0 || target.colorTextureView == null) return
        val useFxaa = AntiAlias.isEnabled
        val useFilter = Filter.isShaderMode && Filter.color.a > 0
        try {
            if (useFxaa) renderFxaa(target)
            if (useFilter) renderFilter(target, Filter.color)
        } finally {
            if (useFxaa) fxaaUniforms.endFrame()
            if (useFilter) filterUniforms.endFrame()
        }
    }

    fun close() {
        fxaaInput?.destroyBuffers()
        fxaaInput = null
        filterInput?.destroyBuffers()
        filterInput = null
        fxaaUniforms.close()
        filterUniforms.close()
    }

    private fun renderFxaa(target: RenderTarget) {
        val input = ensureInput(target, true)
        val sourceTexture = target.colorTexture ?: return
        val sourceView = target.colorTextureView ?: return
        val inputTexture = input.colorTexture ?: return
        val inputView = input.colorTextureView ?: return
        val encoder = RenderSystem.getDevice().createCommandEncoder()
        encoder.copyTextureToTexture(sourceTexture, inputTexture, 0, 0, 0, 0, 0, target.width, target.height)
        encoder.createRenderPass({ "TrollHack FXAA" }, sourceView, OptionalInt.empty()).use { pass ->
            pass.setPipeline(fxaaPipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("FxaaInfo", fxaaUniforms.writeUniform(FxaaInfo(target.width.toFloat(), target.height.toFloat())))
            pass.bindTexture("InputSampler", inputView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            pass.draw(0, 3)
        }
    }

    private fun renderFilter(target: RenderTarget, color: dev.luna5ama.trollhack.graphics.color.ColorRGBA) {
        val input = ensureInput(target, false)
        val sourceTexture = target.colorTexture ?: return
        val sourceView = target.colorTextureView ?: return
        val inputTexture = input.colorTexture ?: return
        val inputView = input.colorTextureView ?: return
        val encoder = RenderSystem.getDevice().createCommandEncoder()
        encoder.copyTextureToTexture(sourceTexture, inputTexture, 0, 0, 0, 0, 0, target.width, target.height)
        encoder.createRenderPass({ "TrollHack Filter" }, sourceView, OptionalInt.empty()).use { pass ->
            pass.setPipeline(filterPipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("FilterColor", filterUniforms.writeUniform(FilterColor(color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f)))
            pass.bindTexture("InputSampler", inputView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            pass.draw(0, 3)
        }
    }

    private fun ensureInput(target: RenderTarget, fxaa: Boolean): TextureTarget {
        val current = if (fxaa) fxaaInput else filterInput
        if (current == null) {
            return TextureTarget(
                if (fxaa) "TrollHack FXAA Input" else "TrollHack Filter Input",
                target.width,
                target.height,
                false
            ).also {
                if (fxaa) fxaaInput = it else filterInput = it
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

    private data class FilterColor(val r: Float, val g: Float, val b: Float, val a: Float) : DynamicUniformStorage.DynamicUniform {
        override fun write(buffer: ByteBuffer) {
            buffer.putFloat(r).putFloat(g).putFloat(b).putFloat(a)
        }
    }
}
