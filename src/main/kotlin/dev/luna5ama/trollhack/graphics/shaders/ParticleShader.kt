package dev.luna5ama.trollhack.graphics.shaders

import dev.luna5ama.trollhack.event.AlwaysListening
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.graphics.RenderUtils3D
import org.lwjgl.input.Mouse

object ParticleShader : GLSLSandbox("/assets/trollhack/shaders/gui/Particle.fsh"), AlwaysListening {
    private val initTime = System.currentTimeMillis()
    private var prevMouseX = 0.0f
    private var prevMouseY = 0.0f
    private var mouseX = 0.0f
    private var mouseY = 0.0f

    init {
        listener<TickEvent.Post>(true) {
            prevMouseX = mouseX
            prevMouseY = mouseY

            mouseX = Mouse.getX() - 1.0f
            mouseY = mc.displayHeight - Mouse.getY() - 1.0f
        }
    }

    fun render() {
        val deltaTicks = RenderUtils3D.partialTicks
        val width = mc.displayWidth.toFloat()
        val height = mc.displayHeight.toFloat()
        val mouseX = prevMouseX + (mouseX - prevMouseX) * deltaTicks
        val mouseY = prevMouseY + (mouseY - prevMouseY) * deltaTicks

        render(width, height, mouseX, mouseY, initTime)
    }
}