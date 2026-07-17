package dev.luna5ama.trollhack.gui

import dev.luna5ama.trollhack.graphics.skia.SkiaMinecraftBridge
import dev.luna5ama.trollhack.modules.impl.client.ClickGui
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

object TrollClickGui : Screen(Component.literal(ClickGui.localizedName)) {
    private var ignoreOpeningBindPress = false

    fun open() {
        val bind = ClickGui.bind
        ignoreOpeningBindPress = bind.category == dev.luna5ama.trollhack.utils.input.KeyBind.Category.KEYBOARD &&
            bind.keyCode != GLFW.GLFW_KEY_UNKNOWN &&
            GLFW.glfwGetKey(mc.window.handle(), bind.keyCode) == GLFW.GLFW_PRESS
        TrollHackCompose.show(TrollHackCompose.Mode.CLICK_GUI)
        mc.setScreen(this)
    }

    fun reloadPanel() = TrollHackCompose.refresh()

    override fun isPauseScreen() = ClickGui.pauseGame

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        SkiaMinecraftBridge.sendPointerMove(pointerX(mouseX.toDouble()), pointerY(mouseY.toDouble()))
    }

    override fun extractBackground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) = Unit

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        SkiaMinecraftBridge.sendPointerMove(pointerX(mouseX), pointerY(mouseY))
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (TrollHackCompose.consumeMouseBind(event.button())) return true
        return SkiaMinecraftBridge.sendPointerButton(
            pointerX(event.x()), pointerY(event.y()), event.button(), true, event.modifiers()
        )
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean =
        SkiaMinecraftBridge.sendPointerButton(
            pointerX(event.x()), pointerY(event.y()), event.button(), false, event.modifiers()
        )

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean =
        SkiaMinecraftBridge.sendPointerMove(pointerX(event.x()), pointerY(event.y()), event.modifiers())

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean = SkiaMinecraftBridge.sendScroll(
        pointerX(mouseX), pointerY(mouseY), horizontalAmount.toFloat(), verticalAmount.toFloat()
    )

    override fun keyPressed(event: KeyEvent): Boolean {
        if (TrollHackCompose.consumeBind(event.key(), event.scancode())) return true
        if (ignoreOpeningBindPress && event.key() == ClickGui.bind.keyCode) {
            ignoreOpeningBindPress = false
            return true
        }
        val closeKey = event.key() == GLFW.GLFW_KEY_ESCAPE ||
            event.key() == ClickGui.bind.keyCode && !TrollHackCompose.isTextInputFocused()
        if (closeKey &&
            !TrollHackCompose.handleEscape()
        ) {
            ClickGui.disable()
            minecraft?.setScreen(null)
            return true
        }
        return SkiaMinecraftBridge.sendKey(event.key(), pressed = true, modifiers = event.modifiers())
    }

    override fun keyReleased(event: KeyEvent): Boolean {
        if (event.key() == ClickGui.bind.keyCode) ignoreOpeningBindPress = false
        return SkiaMinecraftBridge.sendKey(event.key(), pressed = false, modifiers = event.modifiers())
    }

    override fun charTyped(event: CharacterEvent): Boolean =
        SkiaMinecraftBridge.sendCharacter(event.codepoint())

    override fun removed() {
        TrollHackCompose.hide()
        if (ClickGui.isEnabled && minecraft?.screen !== this) ClickGui.disable()
    }

    private fun pointerX(value: Double) = (value * mc.window.width / width.coerceAtLeast(1)).toFloat()
    private fun pointerY(value: Double) = (value * mc.window.height / height.coerceAtLeast(1)).toFloat()
}
