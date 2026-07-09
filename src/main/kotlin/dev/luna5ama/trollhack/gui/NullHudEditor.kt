package dev.luna5ama.trollhack.gui

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.impl.render.Render2DEvent
import dev.luna5ama.trollhack.manager.managers.ModuleManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.impl.client.HudEditor
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.utils.math.vectors.Vec2i
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

object NullHudEditor : Screen(Component.literal("TrollHack HudEditor")), AlwaysListening {
    private val hudModules: List<HudModule>
        get() = ModuleManager.getModulesByCategory(Category.HUD).filterIsInstance<HudModule>()

    init {
        nonNullHandler<Render2DEvent> {
            if (mc.screen !== this) {
                hudModules.forEach {
                    if (it.isEnabled) it.onRender2D(it._x, it._y)
                }
            }
        }
    }

    fun open() {
        mc.setScreen(this)
    }

    fun reloadPanel() {
        // Legacy compatibility hook. HUD modules are read live from ModuleManager.
    }

    override fun isPauseScreen(): Boolean {
        return HudEditor.pauseGame
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0x66000000)
        hudModules.forEach {
            if (it.isEnabled) it.onRender2D(it._x, it._y)
        }
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hudModules.forEach {
            if (it.onMouseMove(Vec2i(mouseX, mouseY))) return
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mouse = Vec2i(event.x(), event.y())
        hudModules.forEach {
            if (it.onMouseClicked(mouse)) return true
        }
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        val mouse = Vec2i(event.x(), event.y())
        hudModules.forEach {
            if (it.onMouseRelease(mouse)) return true
        }
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            HudEditor.disable()
            minecraft?.setScreen(null)
            return true
        }
        return true
    }

    override fun removed() {
        if (HudEditor.isEnabled && minecraft?.screen !== this) {
            HudEditor.disable()
        }
    }
}
