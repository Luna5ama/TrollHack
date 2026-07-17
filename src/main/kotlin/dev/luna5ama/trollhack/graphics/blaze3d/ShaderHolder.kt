package dev.luna5ama.trollhack.graphics.blaze3d

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import dev.luna5ama.trollhack.Metadata
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.modules.impl.visual.Shaders
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.DynamicUniformStorage
import net.minecraft.client.renderer.OutlineBufferSource
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.minecraft.util.Util
import java.nio.ByteBuffer
import java.util.OptionalInt

object ShaderHolder {
    const val TROLLHACK_CHEST_OUTLINE_MARKER: Int = 0x01000001

    private val paramsUniformSize = Std140SizeCalculator()
        .putVec2()
        .putVec2()
        .putFloat()
        .putFloat()
        .putFloat()
        .putFloat()
        .putFloat()
        .putFloat()
        .putFloat()
        .putFloat()
        .putFloat()
        .putVec2()
        .get()

    private val colorUniformSize = Std140SizeCalculator()
        .putVec4()
        .putVec4()
        .putVec4()
        .putVec4()
        .putVec4()
        .putVec4()
        .get()

    private var defaultPipeline: RenderPipeline? = null
    private var smokePipeline: RenderPipeline? = null
    private var gradientPipeline: RenderPipeline? = null
    private var snowPipeline: RenderPipeline? = null
    private var fadePipeline: RenderPipeline? = null
    private var copyPipeline: RenderPipeline? = null
    private var shaderSwap: RenderTarget? = null
    private var handTarget: RenderTarget? = null
    private var chestTarget: RenderTarget? = null
    private var paramsUniforms: DynamicUniformStorage<ShaderParams>? = null
    private var colorUniforms: DynamicUniformStorage<ShaderColors>? = null
    private val chestOutlineBufferSource = OutlineBufferSource()
    private var renderingHands = false
    private var capturedHands = false
    private var renderingChests = false
    private var capturedChests = false
    private var preparedChests = false
    private var uniformsWritten = false
    private var closed = false

    @JvmStatic
    fun processEntityOutlineTarget(target: RenderTarget?, shader: Shaders.ShaderMode) {
        if (closed || target == null || target.width <= 0 || target.height <= 0 || target.colorTextureView == null) return

        ensurePrograms()
        val swap = ensureSwap(target.width, target.height)
        val uniforms = writeShaderUniforms(target.width, target.height)

        renderPass("trollhack_shader_effect", target, swap, pipeline(shader), uniforms, usesColors(shader))
        renderPass("trollhack_shader_copy", swap, target, copyPipeline!!, null, false)
    }

    @JvmStatic
    fun beginHandOutlineCapture(width: Int, height: Int) {
        if (closed || !Shaders.shouldRenderHands()) return

        val target = ensureHandTarget(width.coerceAtLeast(1), height.coerceAtLeast(1))
        val colorTexture = target.colorTexture ?: return
        val depthTexture = target.depthTexture ?: return
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
            colorTexture,
            0,
            depthTexture,
            1.0
        )
        renderingHands = true
        capturedHands = true
    }

    @JvmStatic
    fun endHandOutlineCapture() {
        renderingHands = false
    }

    @JvmStatic
    fun isRenderingHands(): Boolean = renderingHands

    @JvmStatic
    fun getHandOutlineTarget(): RenderTarget? = if (renderingHands) handTarget else null

    @JvmStatic
    fun getChestOutlineTarget(): RenderTarget? = if (renderingChests) chestTarget else null

    @JvmStatic
    fun getChestOutlineBufferSource(): OutlineBufferSource {
        if (!preparedChests) {
            prepareChestOutlineTarget(Minecraft.getInstance().mainRenderTarget)
            preparedChests = true
        }
        capturedChests = true
        return chestOutlineBufferSource
    }

    @JvmStatic
    fun endChestOutlineBatch() {
        if (!capturedChests || chestTarget == null) return

        renderingChests = true
        try {
            chestOutlineBufferSource.endOutlineBatch()
        } finally {
            renderingChests = false
        }
    }

    @JvmStatic
    fun processHandOutlineTarget(mainTarget: RenderTarget?) {
        if (!capturedHands) return
        capturedHands = false

        val target = handTarget
        if (!Shaders.shouldRenderHands() || target == null) return
        val mainView = mainTarget?.colorTextureView ?: return

        processEntityOutlineTarget(target, Shaders.handsMode)
        target.blitAndBlendToTexture(mainView)
    }

    @JvmStatic
    fun prepareChestOutlineTarget(referenceTarget: RenderTarget?) {
        if (closed || referenceTarget == null || referenceTarget.width <= 0 || referenceTarget.height <= 0) return

        val target = ensureChestTarget(referenceTarget.width, referenceTarget.height)
        val colorTexture = target.colorTexture ?: return
        val depthTexture = target.depthTexture ?: return
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
            colorTexture,
            0,
            depthTexture,
            1.0
        )
    }

    @JvmStatic
    fun processChestOutlineTarget(mainTarget: RenderTarget?) {
        if (capturedChests) {
            capturedChests = false
            val target = chestTarget
            val mainView = mainTarget?.colorTextureView
            if (target != null && mainView != null) {
                processEntityOutlineTarget(target, Shaders.chestMode)
                target.blitAndBlendToTexture(mainView)
            }
        }
        preparedChests = false
    }

    @JvmStatic
    fun endFrame() {
        if (!uniformsWritten) return
        uniformsWritten = false
        paramsUniforms?.endFrame()
        colorUniforms?.endFrame()
    }

    @JvmStatic
    fun discardCaptures() {
        renderingHands = false
        capturedHands = false
        renderingChests = false
        capturedChests = false
        preparedChests = false
    }

    @JvmStatic
    @Synchronized
    fun close() {
        if (closed) return
        closed = true
        discardCaptures()

        shaderSwap?.destroyBuffers()
        shaderSwap = null
        handTarget?.destroyBuffers()
        handTarget = null
        chestTarget?.destroyBuffers()
        chestTarget = null
        paramsUniforms?.close()
        paramsUniforms = null
        colorUniforms?.close()
        colorUniforms = null
    }

    private fun renderPass(
        name: String,
        input: RenderTarget,
        output: RenderTarget,
        pipeline: RenderPipeline,
        uniforms: ShaderUniforms?,
        bindColors: Boolean
    ) {
        val inputView = input.colorTextureView ?: return
        val outputView = output.colorTextureView ?: return
        val encoder = RenderSystem.getDevice().createCommandEncoder()
        val sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)

        encoder.createRenderPass({ name }, outputView, OptionalInt.empty()).use { pass ->
            pass.setPipeline(pipeline)
            RenderSystem.bindDefaultUniforms(pass)
            if (uniforms != null) {
                pass.setUniform("ShaderParams", uniforms.params)
                if (bindColors) pass.setUniform("ShaderColors", uniforms.colors)
            }
            pass.bindTexture("InputSampler", inputView, sampler)
            pass.draw(0, 3)
        }
    }

    private fun writeShaderUniforms(screenWidth: Int, screenHeight: Int): ShaderUniforms {
        val width = screenWidth.coerceAtLeast(1).toFloat()
        val height = screenHeight.coerceAtLeast(1).toFloat()
        val window = Minecraft.getInstance().window
        val params = ShaderParams(
            width,
            height,
            Shaders.quality.toFloat(),
            Shaders.lineWidth.toFloat(),
            if (Shaders.smokeGlow) -1.0f else Shaders.outlineColor.aFloat,
            Shaders.fillAlpha / 255.0f,
            Shaders.alpha2 / 255.0f,
            (Util.getMillis() % 100_000L) / 1000.0f,
            Shaders.factor.toFloat(),
            Shaders.gradient.toFloat(),
            Shaders.octaves.toFloat(),
            window.guiScaledWidth.coerceAtLeast(1).toFloat(),
            window.guiScaledHeight.coerceAtLeast(1).toFloat()
        )
        val colors = ShaderColors(
            Shaders.outlineColor,
            Shaders.smokeOutlineColor1,
            Shaders.smokeOutlineColor2,
            Shaders.fillColor1,
            Shaders.fillColor2,
            Shaders.fillColor3
        )
        uniformsWritten = true
        return ShaderUniforms(paramsUniforms!!.writeUniform(params), colorUniforms!!.writeUniform(colors))
    }

    private fun ensurePrograms() {
        if (defaultPipeline != null) return

        defaultPipeline = pipeline("outline", true)
        smokePipeline = pipeline("smoke", true)
        gradientPipeline = pipeline("gradient", false)
        snowPipeline = pipeline("snow", true)
        fadePipeline = pipeline("fade", true)
        copyPipeline = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(id("pipelines/shader_copy"))
            .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
            .withFragmentShader(id("shader_copy"))
            .withSampler("InputSampler")
            .withCull(false)
            .build()
        paramsUniforms = DynamicUniformStorage("trollhack-shader-params", paramsUniformSize, 8)
        colorUniforms = DynamicUniformStorage("trollhack-shader-colors", colorUniformSize, 8)
    }

    private fun ensureSwap(width: Int, height: Int): RenderTarget {
        var target = shaderSwap
        if (target == null) {
            target = TextureTarget("TrollHack Shader Swap", width, height, false)
            shaderSwap = target
        } else if (target.width != width || target.height != height) {
            target.resize(width, height)
        }
        return target
    }

    private fun ensureHandTarget(width: Int, height: Int): RenderTarget {
        var target = handTarget
        if (target == null) {
            target = TextureTarget("TrollHack Shader Hands", width, height, true)
            handTarget = target
        } else if (target.width != width || target.height != height) {
            target.resize(width, height)
        }
        return target
    }

    private fun ensureChestTarget(width: Int, height: Int): RenderTarget {
        var target = chestTarget
        if (target == null) {
            target = TextureTarget("TrollHack Shader Chests", width, height, true)
            chestTarget = target
        } else if (target.width != width || target.height != height) {
            target.resize(width, height)
        }
        return target
    }

    private fun pipeline(shaderName: String, useColors: Boolean): RenderPipeline {
        val builder = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(id("pipelines/shader_$shaderName"))
            .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
            .withFragmentShader(id("shader_$shaderName"))
            .withCull(false)
            .withUniform("ShaderParams", UniformType.UNIFORM_BUFFER)

        if (useColors) builder.withUniform("ShaderColors", UniformType.UNIFORM_BUFFER)
        return builder.withSampler("InputSampler").build()
    }

    private fun pipeline(shader: Shaders.ShaderMode): RenderPipeline = when (shader) {
        Shaders.ShaderMode.SMOKE -> smokePipeline!!
        Shaders.ShaderMode.GRADIENT -> gradientPipeline!!
        Shaders.ShaderMode.SNOW -> snowPipeline!!
        Shaders.ShaderMode.FADE -> fadePipeline!!
        Shaders.ShaderMode.DEFAULT -> defaultPipeline!!
    }

    private fun usesColors(shader: Shaders.ShaderMode): Boolean = shader != Shaders.ShaderMode.GRADIENT

    private fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(Metadata.ID, path)

    private data class ShaderUniforms(val params: GpuBufferSlice, val colors: GpuBufferSlice)

    private data class ShaderParams(
        val width: Float,
        val height: Float,
        val quality: Float,
        val lineWidth: Float,
        val outlineAlpha: Float,
        val fillAlpha: Float,
        val gradientAlpha: Float,
        val time: Float,
        val gradientFactor: Float,
        val gradientScale: Float,
        val octaves: Float,
        val resolutionWidth: Float,
        val resolutionHeight: Float
    ) : DynamicUniformStorage.DynamicUniform {
        override fun write(buffer: ByteBuffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec2(width, height)
                .putVec2(1.0f / width, 1.0f / height)
                .putFloat(quality)
                .putFloat(lineWidth)
                .putFloat(outlineAlpha)
                .putFloat(fillAlpha)
                .putFloat(gradientAlpha)
                .putFloat(time)
                .putFloat(gradientFactor)
                .putFloat(gradientScale)
                .putFloat(octaves)
                .putVec2(resolutionWidth, resolutionHeight)
        }
    }

    private data class ShaderColors(
        val outline: ColorRGBA,
        val smokeOutline1: ColorRGBA,
        val smokeOutline2: ColorRGBA,
        val fill: ColorRGBA,
        val smokeFill1: ColorRGBA,
        val smokeFill2: ColorRGBA
    ) : DynamicUniformStorage.DynamicUniform {
        override fun write(buffer: ByteBuffer) {
            Std140Builder.intoBuffer(buffer)
                .putColor(outline)
                .putColor(smokeOutline1)
                .putColor(smokeOutline2)
                .putColor(fill)
                .putColor(smokeFill1)
                .putColor(smokeFill2)
        }

        private fun Std140Builder.putColor(color: ColorRGBA): Std140Builder {
            return putVec4(color.rFloat, color.gFloat, color.bFloat, color.aFloat)
        }
    }
}
