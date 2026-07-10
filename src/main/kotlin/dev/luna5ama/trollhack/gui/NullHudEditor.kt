package dev.luna5ama.trollhack.gui

import dev.luna5ama.trollhack.graphics.skia.SkiaMinecraftBridge
import dev.luna5ama.trollhack.modules.impl.client.HudEditor
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

object NullHudEditor : Screen(Component.literal("TrollHack HudEditor")) {
    fun open() {
        TrollHackCompose.show(TrollHackCompose.Mode.HUD_EDITOR)
        mc.setScreen(this)
    }

    fun reloadPanel() = TrollHackCompose.refresh()

    override fun isPauseScreen() = HudEditor.pauseGame

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        SkiaMinecraftBridge.sendPointerMove(pointerX(mouseX.toDouble()), pointerY(mouseY.toDouble()))
    }

    override fun renderBackground(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) = Unit

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
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && !TrollHackCompose.handleEscape()) {
            HudEditor.disable()
            minecraft?.setScreen(null)
            return true
        }
        return SkiaMinecraftBridge.sendKey(event.key(), pressed = true, modifiers = event.modifiers())
    }

    override fun keyReleased(event: KeyEvent): Boolean =
        SkiaMinecraftBridge.sendKey(event.key(), pressed = false, modifiers = event.modifiers())

    override fun charTyped(event: CharacterEvent): Boolean =
        SkiaMinecraftBridge.sendCharacter(event.codepoint(), event.modifiers())

    override fun removed() {
        TrollHackCompose.hide()
        if (HudEditor.isEnabled && minecraft?.screen !== this) HudEditor.disable()
    }

    private fun pointerX(value: Double) = (value * mc.window.width / width.coerceAtLeast(1)).toFloat()
    private fun pointerY(value: Double) = (value * mc.window.height / height.coerceAtLeast(1)).toFloat()
}
