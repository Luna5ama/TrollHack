package dev.luna5ama.trollhack.gui.rgui.windows

import dev.luna5ama.trollhack.graphics.AnimationFlag
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.graphics.RenderUtils2D
import dev.luna5ama.trollhack.graphics.Resolution
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.gui.rgui.WindowComponent
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.setting.GuiConfig
import dev.luna5ama.trollhack.util.delegate.FrameFloat
import dev.luna5ama.trollhack.util.math.vector.Vec2f

class DockingOverlay(screen: IGuiScreen, private val parent: WindowComponent) : WindowComponent(
    screen,
    "Docking Overlay",
    UiSettingGroup.NONE,
    GuiConfig
) {
    override var posX: Float
        get() = 0.0f
        set(_) {}

    override var posY: Float
        get() = 0.0f
        set(_) {}

    override var width: Float
        get() = Resolution.trollWidthF
        set(_) {}

    override var height: Float
        get() = Resolution.trollHeightF
        set(_) {}

    private val alphaMul = AnimationFlag(Easing.OUT_QUART, 300.0f)
    private val renderAlphaMul by FrameFloat(alphaMul::get)
    private var closing = false

    override fun onGuiClosed() {
        super.onGuiClosed()
        screen.closeWindow(this)
    }

    override fun onDisplayed() {
        super.onDisplayed()
        closing = false
        alphaMul.forceUpdate(0.0f, 1.0f)
    }

    override fun onRelease(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        closing = true
        alphaMul.update(0.0f)

        val fifthWidth = width / 5.0f
        val fifthHeight = height / 5.0f

        fun <T> List<T>.getDocking(v: Float, step: Float): T? {
            return when (v) {
                in step * 0..step * 1 -> this[0]
                in step * 2..step * 3 -> this[1]
                in step * 4..step * 5 -> this[2]
                else -> null
            }
        }

        val dockingH = dev.luna5ama.trollhack.graphics.HAlign.VALUES.getDocking(screen.mousePos.x, fifthWidth)
        val dockingV = dev.luna5ama.trollhack.graphics.VAlign.VALUES.getDocking(screen.mousePos.y, fifthHeight)

        if (dockingH != null && dockingV != null) {
            parent.dockingH = dockingH
            parent.dockingV = dockingV
        }
    }

    override fun onTick() {
        super.onTick()
        if (renderAlphaMul == 0.0f && closing) {
            screen.closeWindow(this)
        }
    }

    override fun onPostRender(absolutePos: Vec2f) {
        val fifthWidth = width / 5.0f
        val fifthHeight = height / 5.0f
        val rectColor = GuiSetting.backGround.alpha((GuiSetting.backGround.a * renderAlphaMul).toInt())

        fun drawRect(x: Int, y: Int) {
            RenderUtils2D.drawRectFilled(
                fifthWidth * x,
                fifthHeight * y,
                fifthWidth * (x + 1),
                fifthHeight * (y + 1),
                rectColor
            )
        }

        drawRect(0, 0)
        drawRect(2, 0)
        drawRect(4, 0)

        drawRect(0, 2)
        drawRect(2, 2)
        drawRect(4, 2)

        drawRect(0, 4)
        drawRect(2, 4)
        drawRect(4, 4)

        val textColor = GuiSetting.text.alpha((GuiSetting.text.a * renderAlphaMul).toInt())

        fun drawText(x: Int, y: Int, text: String) {
            val scale = 1.5f
            MainFontRenderer.drawString(
                text,
                fifthWidth * (x + 0.5f) - MainFontRenderer.getWidth(text, scale) / 2.0f,
                fifthHeight * (y + 0.5f) - MainFontRenderer.getHeight(scale) / 2.0f,
                textColor,
                scale
            )
        }

        drawText(0, 0, "Top Left")
        drawText(2, 0, "Top Center")
        drawText(4, 0, "Top Right")

        drawText(0, 2, "Middle Left")
        drawText(2, 2, "Middle Center")
        drawText(4, 2, "Middle Right")

        drawText(0, 4, "Bottom Left")
        drawText(2, 4, "Bottom Center")
        drawText(4, 4, "Bottom Right")
    }

    override fun isInWindow(mousePos: Vec2f): Boolean {
        return super.isInWindow(mousePos)
    }
}