package dev.luna5ama.trollhack.graphics

import dev.luna5ama.trollhack.util.Quad
import dev.luna5ama.trollhack.util.Wrapper
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.glUseProgram

object GlStateUtils {
    private val mc = Wrapper.minecraft
    private var lastScissor: Quad<Int, Int, Int, Int>? = null
    private val scissorList = ArrayList<Quad<Int, Int, Int, Int>>()

    private var bindProgram = 0

    fun useProgramForce(id: Int) {
        glUseProgram(id)
        bindProgram = id
    }

    fun useProgram(id: Int) {
        if (id != bindProgram) {
            glUseProgram(id)
            bindProgram = id
        }
    }

    fun scissor(x: Int, y: Int, width: Int, height: Int) {
        lastScissor = Quad(x, y, width, height)
        glScissor(x, y, width, height)
    }

    fun pushScissor() {
        lastScissor?.let {
            scissorList.add(it)
        }
    }

    fun popScissor() {
        scissorList.removeLastOrNull()?.let {
            scissor(it.first, it.second, it.third, it.fourth)
        }
    }

    fun useVbo(): Boolean {
        return mc.gameSettings.useVbo
    }

    fun alpha(state: Boolean) {
        if (state) {
            GlStateManager.enableAlpha()
        } else {
            GlStateManager.disableAlpha()
        }
    }

    fun blend(state: Boolean) {
        if (state) {
            GlStateManager.enableBlend()
        } else {
            GlStateManager.disableBlend()
        }
    }

    fun smooth(state: Boolean) {
        if (state) {
            GlStateManager.shadeModel(GL_SMOOTH)
        } else {
            GlStateManager.shadeModel(GL_FLAT)
        }
    }

    fun lineSmooth(state: Boolean) {
        if (state) {
            glEnable(GL_LINE_SMOOTH)
            glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)
        } else {
            glDisable(GL_LINE_SMOOTH)
        }
    }

    fun depth(state: Boolean) {
        if (state) {
            GlStateManager.enableDepth()
        } else {
            GlStateManager.disableDepth()
        }
    }

    fun texture2d(state: Boolean) {
        if (state) {
            GlStateManager.enableTexture2D()
        } else {
            GlStateManager.disableTexture2D()
        }
    }

    fun cull(state: Boolean) {
        if (state) {
            GlStateManager.enableCull()
        } else {
            GlStateManager.disableCull()
        }
    }

    fun lighting(state: Boolean) {
        if (state) {
            GlStateManager.enableLighting()
        } else {
            GlStateManager.disableLighting()
        }
    }

    fun rescaleActual() {
        rescale(Wrapper.minecraft.displayWidth.toDouble(), Wrapper.minecraft.displayHeight.toDouble())
    }

    fun rescaleTroll() {
        rescale(Resolution.trollWidthF.toDouble(), Resolution.trollHeightF.toDouble())
    }

    fun rescaleMc() {
        val resolution = ScaledResolution(Wrapper.minecraft)
        rescale(resolution.scaledWidth_double, resolution.scaledHeight_double)
    }

    fun pushMatrixAll() {
        GlStateManager.matrixMode(GL_PROJECTION)
        GlStateManager.pushMatrix()
        GlStateManager.matrixMode(GL_MODELVIEW)
        GlStateManager.pushMatrix()
    }

    fun popMatrixAll() {
        GlStateManager.matrixMode(GL_PROJECTION)
        GlStateManager.popMatrix()
        GlStateManager.matrixMode(GL_MODELVIEW)
        GlStateManager.popMatrix()
    }

    fun rescale(width: Double, height: Double) {
        GlStateManager.clear(GL_DEPTH_BUFFER_BIT)
        GlStateManager.matrixMode(GL_PROJECTION)
        GlStateManager.loadIdentity()
        GlStateManager.ortho(0.0, width, height, 0.0, 1000.0, 3000.0)
        GlStateManager.matrixMode(GL_MODELVIEW)
        GlStateManager.loadIdentity()
        GlStateManager.translate(0.0f, 0.0f, -2000.0f)
    }
}