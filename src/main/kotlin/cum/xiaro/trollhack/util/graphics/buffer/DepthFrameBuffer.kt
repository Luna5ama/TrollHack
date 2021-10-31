package cum.xiaro.trollhack.util.graphics.buffer

import org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24
import org.lwjgl.opengl.GL30.*

class DepthFrameBuffer : FrameBuffer() {
    private val depthBufferId = glGenRenderbuffers()

    override fun allocateFrameBuffer(width: Int, height: Int) {
        super.allocateFrameBuffer(width, height)

        glBindRenderbuffer(GL_RENDERBUFFER, depthBufferId)
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBufferId)
    }
}