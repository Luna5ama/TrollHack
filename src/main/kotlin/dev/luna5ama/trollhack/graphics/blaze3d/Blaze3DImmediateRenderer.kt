package dev.luna5ama.trollhack.graphics.blaze3d

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.OptionalDouble
import java.util.OptionalInt

object Blaze3DImmediateRenderer {
    fun draw(mesh: MeshData, pipeline: RenderPipeline, modelView: Matrix4f) {
        RenderSystem.assertOnRenderThread()
        val vertexBuffer = pipeline.vertexFormat.uploadImmediateVertexBuffer(mesh.vertexBuffer())
        val drawState = mesh.drawState()
        val indexBuffer: com.mojang.blaze3d.buffers.GpuBuffer
        val indexType: VertexFormat.IndexType
        val sortedIndices = mesh.indexBuffer()
        if (sortedIndices == null) {
            val sequentialIndices = RenderSystem.getSequentialBuffer(drawState.mode())
            indexBuffer = sequentialIndices.getBuffer(drawState.indexCount())
            indexType = sequentialIndices.type()
        } else {
            indexBuffer = pipeline.vertexFormat.uploadImmediateIndexBuffer(sortedIndices)
            indexType = drawState.indexType()
        }
        val dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
            modelView,
            Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
            Vector3f(),
            Matrix4f()
        )
        val target = Minecraft.getInstance().mainRenderTarget
        val colorTarget = RenderSystem.outputColorTextureOverride
            ?: requireNotNull(target.colorTextureView) { "Main render target has no color attachment" }
        val depthTarget = if (target.useDepth) {
            RenderSystem.outputDepthTextureOverride ?: target.depthTextureView
        } else {
            null
        }

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "TrollHack immediate 3D draw" },
            colorTarget,
            OptionalInt.empty(),
            depthTarget,
            OptionalDouble.empty()
        ).use { pass ->
            pass.setPipeline(pipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", dynamicTransforms)
            pass.setVertexBuffer(0, vertexBuffer)
            pass.setIndexBuffer(indexBuffer, indexType)
            pass.drawIndexed(0, 0, drawState.indexCount(), 1)
        }
    }
}
