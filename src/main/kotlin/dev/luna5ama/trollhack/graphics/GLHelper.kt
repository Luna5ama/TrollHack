package dev.luna5ama.trollhack.graphics

import com.mojang.blaze3d.opengl.GlStateManager
import dev.luna5ama.trollhack.utils.Helper
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.glUseProgram
import org.lwjgl.opengl.GL30
import kotlin.math.max
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object GLHelper : Helper {

    // Disabled in core mode
    var blend by GLState(false) {
        if (it) {
            GlStateManager._enableBlend()
            glEnable(GL_BLEND)
        } else {
            GlStateManager._disableBlend()
            glDisable(GL_BLEND)
        }
    }
    var depth by GLState(false) {
        if (it) {
            GlStateManager._enableDepthTest()
            glEnable(GL_DEPTH_TEST)
        } else {
            GlStateManager._disableDepthTest()
            glDisable(GL_DEPTH_TEST)
        }
    }
    var cull by GLState(false) {
        if (it) {
            GlStateManager._enableCull()
            glEnable(GL_CULL_FACE)
        } else {
            GlStateManager._disableCull()
            glDisable(GL_CULL_FACE)
        }
    }
    var lineSmooth by GLState(false) { if (it) glEnable(GL_LINE_SMOOTH) else glDisable(GL_LINE_SMOOTH) }
    var polygonMode by GLState(GL_FILL) { glPolygonMode(GL_FRONT_AND_BACK, it) }
    var vSync by GLState(true) { if (it) GLFW.glfwSwapInterval(1) else GLFW.glfwSwapInterval(0) }
    var fullScreen by GLState(false) {
        if (it) {
            val xArray = IntArray(1)
            val yArray = IntArray(1)
            GLFW.glfwGetWindowPos(mc.window.handle(), xArray, yArray)
            windowedXPos = xArray[0]
            windowedYPos = yArray[0]
            GLFW.glfwGetWindowSize(mc.window.handle(), xArray, yArray)
            windowedWidth = xArray[0]
            windowedHeight = yArray[0]
            val monitor = GLFW.glfwGetPrimaryMonitor()
            val mode = GLFW.glfwGetVideoModes(monitor)!!.last()
            GLFW.glfwSetWindowMonitor(mc.window.handle(), monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate())
        } else GLFW.glfwSetWindowMonitor(
            mc.window.handle(),
            0L,
            windowedXPos,
            windowedYPos,
            windowedWidth,
            windowedHeight,
            GLFW.GLFW_DONT_CARE
        )
    }

    private var windowedXPos = 0
    private var windowedYPos = 0
    private var windowedWidth = 0
    private var windowedHeight = 0
    var bindProgram = -1;
    var bindFBO = -1;
    var bindVAO = -1;
    var mouseMode = GLFW.GLFW_CURSOR_NORMAL; private set

    fun bindVertexArray(vao: Int, force: Boolean = false) {
        if (force || vao != bindVAO) {
            GL30.glBindVertexArray(vao)
            bindVAO = vao
        }
    }

    fun bindFramebuffer(fbo: Int, force: Boolean = true) {
        if (force || fbo != bindFBO) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo)
            bindFBO = fbo
        }
    }

    fun useProgram(id: Int, force: Boolean = false) {
        if (force || id != bindProgram) {
            glUseProgram(id)
            bindProgram = id
        }
    }

    fun mouseMode(mode: Int) {
        if (mode != mouseMode) {
            mouseMode = mode
            GLFW.glfwSetInputMode(mc.window.handle(), GLFW.GLFW_CURSOR, mode)
            if (mode == GLFW.GLFW_CURSOR_NORMAL) GLFW.glfwSetCursorPos(
                mc.window.handle(),
                mc.window.width / 2.0,
                mc.window.height / 2.0
            )
        }
    }

    fun unbindProgram() = useProgram(0, true)

    fun scissor(x: Int, y: Int, x1: Int, y1: Int) {
        val scale = mc.window.guiScale
        glScissor(
            (x * scale).toInt(),
            (mc.window.height - y1 * scale).toInt(),
            max(0.0, (x1 - x) * scale.toDouble()).toInt(),
            max(0.0, (y1 - y) * scale.toDouble()).toInt()
        )
    }

}

class GLState<T>(valueIn: T, private val action: (T) -> Unit) : ReadWriteProperty<Any?, T> {
    private var value = valueIn

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (value != this.value) action.invoke(value)
    }
}
