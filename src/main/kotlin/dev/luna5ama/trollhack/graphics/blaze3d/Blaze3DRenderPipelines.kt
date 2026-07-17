package dev.luna5ama.trollhack.graphics.blaze3d

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import dev.luna5ama.trollhack.Metadata
import net.minecraft.resources.Identifier

object Blaze3DRenderPipelines {
    private val fillSnippet = RenderPipeline.builder()
        .withVertexShader(id("core/esp_fill"))
        .withFragmentShader(id("core/esp_fill"))
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
        .buildSnippet()

    private val lineSnippet = RenderPipeline.builder()
        .withVertexShader(id("core/esp_line"))
        .withFragmentShader(id("core/esp_line"))
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("Globals", UniformType.UNIFORM_BUFFER)
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.Mode.LINES)
        .buildSnippet()

    @JvmField
    val FILL_DEPTH: RenderPipeline = RenderPipeline.builder(fillSnippet)
        .withLocation(id("pipeline/esp_fill_depth"))
        .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build()

    @JvmField
    val FILL_THROUGH: RenderPipeline = RenderPipeline.builder(fillSnippet)
        .withLocation(id("pipeline/esp_fill_through"))
        .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .build()

    @JvmField
    val LINE_DEPTH: RenderPipeline = RenderPipeline.builder(lineSnippet)
        .withLocation(id("pipeline/esp_line_depth"))
        .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build()

    @JvmField
    val LINE_THROUGH: RenderPipeline = RenderPipeline.builder(lineSnippet)
        .withLocation(id("pipeline/esp_line_through"))
        .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .build()

    private fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(Metadata.ID, path)
}
