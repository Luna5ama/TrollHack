package dev.luna5ama.trollhack.graphics.shader

import dev.luna5ama.trollhack.RS
import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.impl.render.FenceSyncEvent
import dev.luna5ama.trollhack.event.impl.render.ResolutionUpdateEvent
import dev.luna5ama.trollhack.graphics.GLHelper
import dev.luna5ama.trollhack.graphics.buffer.VertexFormat
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects.draw
import dev.luna5ama.trollhack.graphics.matrix.getFloatArray
import dev.luna5ama.trollhack.modules.impl.visual.MotionBlur
import dev.luna5ama.trollhack.utils.compat.colorTextureId
import dev.luna5ama.trollhack.utils.compat.frameBufferId
import dev.luna5ama.trollhack.utils.MinecraftWrapper
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL41.*
import org.lwjgl.opengl.GL45.glBlitNamedFramebuffer
import org.lwjgl.opengl.GL45.glCheckNamedFramebufferStatus
import org.lwjgl.opengl.GL45.glCreateFramebuffers
import org.lwjgl.opengl.GL45.glCreateTextures
import org.lwjgl.opengl.GL45.glNamedFramebufferTexture
import org.lwjgl.opengl.GL45.glTextureStorage2D

object MotionBlur : AlwaysListening {
    private val cameraPosition = Vector3f()
    private val previousCameraPosition = Vector3f()
    private val gbufferProjection = Matrix4f()
    private val gbufferPreviousProjection = Matrix4f()
    private val gbufferProjectionInverse = Matrix4f()
    private val gbufferModelView = Matrix4f()
    private val gbufferPreviousModelView = Matrix4f()
    private val gbufferModelViewInverse = Matrix4f()
    private var depthAttachment = glCreateTextures(GL_TEXTURE_2D)
    private var depthBuffer = glCreateFramebuffers()

    init {
        handler<FenceSyncEvent> {
            DrawBuffer.onSync()
        }

        handler<ResolutionUpdateEvent> {
            val width = it.framebufferWidth
            val height = it.framebufferHeight
            glDeleteTextures(depthAttachment)
            glDeleteFramebuffers(depthBuffer)
            depthAttachment = glCreateTextures(GL_TEXTURE_2D)
            depthBuffer = glCreateFramebuffers()
            glTextureStorage2D(depthAttachment, 1, GL_DEPTH24_STENCIL8, width, height)
            glNamedFramebufferTexture(depthBuffer, GL_DEPTH_ATTACHMENT, depthAttachment, 0)
            val status = glCheckNamedFramebufferStatus(depthBuffer, GL_FRAMEBUFFER)
            require(status == GL_FRAMEBUFFER_COMPLETE) {
                "Framebuffer is not complete: $it"
            }
        }

        glTextureStorage2D(depthAttachment, 1, GL_DEPTH24_STENCIL8, RS.width, RS.height)
        glNamedFramebufferTexture(depthBuffer, GL_DEPTH_ATTACHMENT, depthAttachment, 0)
        val status = glCheckNamedFramebufferStatus(depthBuffer, GL_FRAMEBUFFER)
        require(status == GL_FRAMEBUFFER_COMPLETE) {
            "Framebuffer is not complete: $status"
        }
    }

    fun copyDepthBuffer() {
        val fb = MinecraftWrapper.mc.mainRenderTarget
//        IntArray(1).let {
//            glGetNamedFramebufferAttachmentParameteriv(fb.fbo, GL_DEPTH_ATTACHMENT, GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE, it)
//            println(it[0])
//            glGetNamedFramebufferAttachmentParameteriv(fb.fbo, GL_DEPTH_ATTACHMENT, GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE, it)
//            println(it[0])
//            glGetNamedFramebufferAttachmentParameteriv(depthBuffer, GL_DEPTH_ATTACHMENT, GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE, it)
//            println(it[0])
//            glGetNamedFramebufferAttachmentParameteriv(depthBuffer, GL_DEPTH_ATTACHMENT, GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE, it)
//            println(it[0])
//        }
        glBlitNamedFramebuffer(
            fb.frameBufferId, depthBuffer,
            0, 0, fb.width, fb.height,
            0, 0, fb.width, fb.height,
            GL_DEPTH_BUFFER_BIT, GL_NEAREST
        )
    }

    fun draw() {
        GLHelper.depth = true
        val fb = MinecraftWrapper.mc.mainRenderTarget
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, fb.colorTextureId)
        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, depthAttachment)
        glBindFramebuffer(GL_FRAMEBUFFER, fb.frameBufferId)

        GL_TRIANGLE_FAN.draw(DrawBuffer) {
            fillBuffer()
        }

        glBindTexture(GL_TEXTURE_2D, 0)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    fun updateMatrix(modelView: Matrix4f, projection: Matrix4f) {
        gbufferPreviousModelView.set(gbufferModelView)
        gbufferPreviousProjection.set(gbufferProjection)
        gbufferModelView.set(modelView)
        gbufferProjection.set(projection)
        gbufferProjectionInverse.set(projection).invert()
        gbufferModelViewInverse.set(modelView).invert()

        val cameraPos = MinecraftWrapper.mc.gameRenderer.mainCamera.position()
        previousCameraPosition.set(cameraPosition)
        cameraPosition.set(cameraPos.x, cameraPos.y, cameraPos.z)

        val viewWidth = RS.widthF
        val viewHeight = RS.heightF

        DrawShader.apply {
            glProgramUniform1f(id, viewWidthLoc, viewWidth)
            glProgramUniform1f(id, viewHeightLoc, viewHeight)
            glProgramUniform1f(id, aspectRatioLoc, viewWidth / viewHeight)
            glProgramUniform1f(id, strengthLoc, MotionBlur.strength)

            glProgramUniform3fv(id, cameraPositionLoc, cameraPosition.getFloatArray())
            glProgramUniform3fv(id, previousCameraPositionLoc, previousCameraPosition.getFloatArray())

            glProgramUniformMatrix4fv(id, gbufferPreviousProjectionLoc, false, gbufferPreviousProjection.getFloatArray())
            glProgramUniformMatrix4fv(id, gbufferProjectionInverseLoc, false, gbufferProjectionInverse.getFloatArray())

            glProgramUniformMatrix4fv(id, gbufferModelViewLoc, false, gbufferModelView.getFloatArray())
            glProgramUniformMatrix4fv(id, gbufferPreviousModelViewLoc, false, gbufferPreviousModelView.getFloatArray())
            glProgramUniformMatrix4fv(id, gbufferModelViewInverseLoc, false, gbufferModelViewInverse.getFloatArray())
        }
    }

    private object DrawBuffer : PMVBObjects.VertexMode(VertexFormat.Pos2f.attribute, DrawShader, sizeMb = 1L) {
        fun fillBuffer() {
            val ptr = arr.ptr
            ptr[0] = 0.0f
            ptr[4] = 0.0f

            ptr[8] = 1.0f
            ptr[12] = 0.0f

            ptr[16] = 1.0f
            ptr[20] = 1.0f

            ptr[24] = 0.0f
            ptr[28] = 1.0f

            arr += 32
            vertexSize += 4
        }
    }

    private object DrawShader : Shader(
        "/assets/shader/effect/MotionBlur.vsh",
        "/assets/shader/effect/MotionBlur.fsh"
    ) {
        val viewWidthLoc = getUniformLocation("viewWidth")
        val viewHeightLoc = getUniformLocation("viewHeight")
        val aspectRatioLoc = getUniformLocation("aspectRatio")
        val strengthLoc = getUniformLocation("strength")

        val cameraPositionLoc = getUniformLocation("cameraPosition")
        val previousCameraPositionLoc = getUniformLocation("previousCameraPosition")

        val gbufferPreviousProjectionLoc = getUniformLocation("gbufferPreviousProjection")
        val gbufferProjectionInverseLoc = getUniformLocation("gbufferProjectionInverse")

        val gbufferModelViewLoc = getUniformLocation("gbufferModelView")
        val gbufferPreviousModelViewLoc = getUniformLocation("gbufferPreviousModelView")
        val gbufferModelViewInverseLoc = getUniformLocation("gbufferModelViewInverse")

        init {
            glProgramUniform1i(id, getUniformLocation("colortex0"), 0)
            glProgramUniform1i(id, getUniformLocation("depthtex1"), 1)
        }
    }
}
