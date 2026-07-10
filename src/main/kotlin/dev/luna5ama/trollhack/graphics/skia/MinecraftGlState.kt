package dev.luna5ama.trollhack.graphics.skia

import com.mojang.blaze3d.opengl.GlStateManager
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL14
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL33

internal class MinecraftGlState private constructor(
    private val activeTexture: Int,
    private val textureBindings: IntArray,
    private val samplerBindings: IntArray,
    private val program: Int,
    private val vertexArray: Int,
    private val arrayBuffer: Int,
    private val elementBuffer: Int,
    private val pixelPackBuffer: Int,
    private val pixelUnpackBuffer: Int,
    private val pixelStoreValues: IntArray,
    private val drawFramebuffer: Int,
    private val readFramebuffer: Int,
    private val blend: Boolean,
    private val depth: Boolean,
    private val cull: Boolean,
    private val scissor: Boolean,
    private val stencil: Boolean,
    private val depthMask: Boolean,
    private val blendSrcRgb: Int,
    private val blendDstRgb: Int,
    private val blendSrcAlpha: Int,
    private val blendDstAlpha: Int,
    private val blendEquationRgb: Int,
    private val blendEquationAlpha: Int
) {
    fun prepareForSkia() {
        GL15.glBindBuffer(GL21_PIXEL_PACK_BUFFER, 0)
        GL15.glBindBuffer(GL21_PIXEL_UNPACK_BUFFER, 0)
        DEFAULT_PIXEL_STORE_VALUES.forEachIndexed { index, value ->
            GL11.glPixelStorei(PIXEL_STORE_PARAMETERS[index], value)
        }
    }

    fun restore() {
        textureBindings.indices.forEach { unit ->
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit)
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureBindings[unit])
            GL33.glBindSampler(unit, samplerBindings[unit])
        }
        GL13.glActiveTexture(activeTexture)

        GL20.glUseProgram(program)
        GL30.glBindVertexArray(vertexArray)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer)
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, elementBuffer)
        GL15.glBindBuffer(GL21_PIXEL_PACK_BUFFER, pixelPackBuffer)
        GL15.glBindBuffer(GL21_PIXEL_UNPACK_BUFFER, pixelUnpackBuffer)
        PIXEL_STORE_PARAMETERS.forEachIndexed { index, parameter ->
            GL11.glPixelStorei(parameter, pixelStoreValues[index])
        }
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer)
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer)

        setEnabled(GL11.GL_BLEND, blend)
        setEnabled(GL11.GL_DEPTH_TEST, depth)
        setEnabled(GL11.GL_CULL_FACE, cull)
        setEnabled(GL11.GL_SCISSOR_TEST, scissor)
        setEnabled(GL11.GL_STENCIL_TEST, stencil)
        GL11.glDepthMask(depthMask)
        GL11.glColorMask(true, true, true, true)
        GL14.glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha)
        GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha)
    }

    private fun setEnabled(capability: Int, enabled: Boolean) {
        if (enabled) GL11.glEnable(capability) else GL11.glDisable(capability)
    }

    companion object {
        private const val GL21_PIXEL_PACK_BUFFER = 0x88EB
        private const val GL21_PIXEL_UNPACK_BUFFER = 0x88EC
        private const val GL21_PIXEL_PACK_BUFFER_BINDING = 0x88ED
        private const val GL21_PIXEL_UNPACK_BUFFER_BINDING = 0x88EF

        private val PIXEL_STORE_PARAMETERS = intArrayOf(
            GL11.GL_PACK_SWAP_BYTES,
            GL11.GL_PACK_LSB_FIRST,
            GL11.GL_PACK_ROW_LENGTH,
            GL11.GL_PACK_SKIP_PIXELS,
            GL11.GL_PACK_SKIP_ROWS,
            GL11.GL_PACK_ALIGNMENT,
            GL12.GL_PACK_IMAGE_HEIGHT,
            GL12.GL_PACK_SKIP_IMAGES,
            GL11.GL_UNPACK_SWAP_BYTES,
            GL11.GL_UNPACK_LSB_FIRST,
            GL11.GL_UNPACK_ROW_LENGTH,
            GL11.GL_UNPACK_SKIP_PIXELS,
            GL11.GL_UNPACK_SKIP_ROWS,
            GL11.GL_UNPACK_ALIGNMENT,
            GL12.GL_UNPACK_IMAGE_HEIGHT,
            GL12.GL_UNPACK_SKIP_IMAGES
        )

        private val DEFAULT_PIXEL_STORE_VALUES = intArrayOf(
            0, 0, 0, 0, 0, 4, 0, 0,
            0, 0, 0, 0, 0, 4, 0, 0
        )

        fun capture(): MinecraftGlState {
            val activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)
            val textureUnits = GlStateManager.TEXTURES.size
            val textureBindings = IntArray(textureUnits)
            val samplerBindings = IntArray(textureUnits)
            repeat(textureUnits) { unit ->
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit)
                textureBindings[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
                samplerBindings[unit] = GL33.glGetIntegeri(GL33.GL_SAMPLER_BINDING, unit)
            }
            GL13.glActiveTexture(activeTexture)

            return MinecraftGlState(
                activeTexture = activeTexture,
                textureBindings = textureBindings,
                samplerBindings = samplerBindings,
                program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),
                arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING),
                elementBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING),
                pixelPackBuffer = GL11.glGetInteger(GL21_PIXEL_PACK_BUFFER_BINDING),
                pixelUnpackBuffer = GL11.glGetInteger(GL21_PIXEL_UNPACK_BUFFER_BINDING),
                pixelStoreValues = IntArray(PIXEL_STORE_PARAMETERS.size) { index ->
                    GL11.glGetInteger(PIXEL_STORE_PARAMETERS[index])
                },
                drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
                readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),
                blend = GL11.glIsEnabled(GL11.GL_BLEND),
                depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                cull = GL11.glIsEnabled(GL11.GL_CULL_FACE),
                scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),
                stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST),
                depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                blendEquationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB),
                blendEquationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA)
            )
        }
    }
}
