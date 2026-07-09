package dev.luna5ama.trollhack.graphics

import dev.luna5ama.trollhack.RenderSystem
import org.lwjgl.opengl.ARBDebugOutput
import org.lwjgl.opengl.ARBDirectStateAccess
import org.lwjgl.opengl.GL43
import org.lwjgl.opengl.GL45

object OpenGLWrapper {
    fun glDebugMessageCallback(
        callback: (Int, Int, Int, Int, Int, Long, Long) -> Unit,
        userParam: Long
    ) {
        if (RenderSystem.compatibility.openGL43) GL43.glDebugMessageCallback(callback, userParam)
        else if (RenderSystem.compatibility.arbDebugOutput) ARBDebugOutput.glDebugMessageCallbackARB(callback, userParam)
        else {
//            throw IllegalStateException("glDebugMessageCallback is not supported")
        }
    }

    fun glDebugMessageControl(
        source: Int,
        type: Int,
        severity: Int,
        ids: IntArray?,
        enabled: Boolean
    ) {
        if (RenderSystem.compatibility.openGL43) GL43.glDebugMessageControl(source, type, severity, ids, enabled)
        else if (RenderSystem.compatibility.arbDebugOutput) ARBDebugOutput.glDebugMessageControlARB(source, type, severity, ids, enabled)
        else {
//            throw IllegalStateException("glDebugMessageControl is not supported")
        }
    }

    fun glBlitNamedFramebuffer(
        readFramebuffer: Int,
        drawFramebuffer: Int,
        srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int,
        dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int,
        mask: Int, filter: Int
    ) {
        if (RenderSystem.compatibility.openGL45)
            GL45.glBlitNamedFramebuffer(
                readFramebuffer,
                drawFramebuffer,
                srcX0, srcY0, srcX1, srcY1,
                dstX0, dstY0, dstX1, dstY1,
                mask, filter
            )
        else if (RenderSystem.compatibility.arbDirectStateAccess)
            ARBDirectStateAccess.glBlitNamedFramebuffer(
                readFramebuffer, drawFramebuffer,
                srcX0, srcY0, srcX1, srcY1,
                dstX0, dstY0, dstX1, dstY1,
                mask, filter
            )
        else throw IllegalStateException("glBlitNamedFramebuffer is not supported")
    }
}