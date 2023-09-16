package dev.luna5ama.trollhack.gui.rgui.windows

import dev.luna5ama.trollhack.graphics.GlStateUtils
import dev.luna5ama.trollhack.graphics.RenderUtils2D
import dev.luna5ama.trollhack.graphics.RenderUtils3D
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.color.ColorUtils
import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.gui.rgui.WindowComponent
import dev.luna5ama.trollhack.gui.rgui.component.Button
import dev.luna5ama.trollhack.gui.rgui.component.SettingSlider
import dev.luna5ama.trollhack.gui.rgui.component.Slider
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.setting.settings.impl.number.IntegerSetting
import dev.luna5ama.trollhack.setting.settings.impl.other.ColorSetting
import dev.luna5ama.trollhack.util.math.MathUtils
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP

class ColorPicker(
    screen: IGuiScreen,
    private val parent: WindowComponent,
    private val setting: ColorSetting
) : TitledWindow(
    screen,
    "Color Picker",
    UiSettingGroup.NONE
) {
    override val resizable: Boolean get() = false
    override val minimizable: Boolean get() = false

    private var hoveredChild: Slider? = null
        set(value) {
            if (value == field) return
            field?.onLeave(screen.mousePos)
            value?.onHover(screen.mousePos)
            field = value
        }

    // Positions
    private var fieldHeight = 0.0f
    private var fieldPos = Pair(Vec2f.ZERO, Vec2f.ZERO)
    private var huePos = Pair(Vec2f.ZERO, Vec2f.ZERO)
    private var hueLinePos = Pair(Vec2f.ZERO, Vec2f.ZERO)
    private var prevColorPos = Pair(Vec2f.ZERO, Vec2f.ZERO)
    private var currentColorPos = Pair(Vec2f.ZERO, Vec2f.ZERO)

    // Main values
    private var hue = 0.0f
    private var saturation = 1.0f
    private var brightness = 1.0f
    private var prevHue = 0.0f
    private var prevSaturation = 1.0f
    private var prevBrightness = 1.0f

    // Sliders
    private val r = IntegerSetting("Red", setting.value.r, 0..255, 1)
    private val g = IntegerSetting("Green", setting.value.g, 0..255, 1)
    private val b = IntegerSetting("Blue", setting.value.b, 0..255, 1)
    private val a = IntegerSetting("Alpha", setting.value.a, 0..255, 1, { setting.hasAlpha })
    private val sliderR = SettingSlider(screen, r)
    private val sliderG = SettingSlider(screen, g)
    private val sliderB = SettingSlider(screen, b)
    private val sliderA = SettingSlider(screen, a)

    // Buttons
    private val buttonOkay = Button(screen, "Okay").action { _, _ -> actionOk() }
    private val buttonCancel = Button(screen, "Cancel").action { _, _ -> actionCancel() }
    private val buttonApply = Button(screen, "Apply").action { _, _ -> actionApply() }

    private val components = arrayOf(sliderR, sliderG, sliderB, sliderA, buttonOkay, buttonCancel, buttonApply)

    override fun onDisplayed() {
        r.value = setting.value.r
        g.value = setting.value.g
        b.value = setting.value.b
        a.value = setting.value.a

        updatePos()
        updateHSBFromRGB()
        super.onDisplayed()
        for (component in components) component.onDisplayed()
        updatePos()
    }

    override fun onTick() {
        super.onTick()
        prevHue = hue
        prevSaturation = saturation
        prevBrightness = brightness
        for (component in components) component.onTick()
        if (hoveredChild != null) updateHSBFromRGB()
        if ((keybordListening as? Slider?)?.listening == false) keybordListening = null
    }

    override fun onMouseInput(mousePos: Vec2f) {
        super.onMouseInput(mousePos)

        hoveredChild = components.firstOrNull {
            it.visible
                && preDragMousePos.x in it.posX..it.posX + it.width
                && preDragMousePos.y in it.posY..it.posY + it.height
        }?.also {
            it.onMouseInput(mousePos)
        }
    }

    override fun onClick(mousePos: Vec2f, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        val relativeMousePos = mousePos.minus(posX, posY)

        hoveredChild?.let {
            it.onClick(relativeMousePos.minus(it.posX, it.posY), buttonId)
        } ?: run {
            updateValues(relativeMousePos, relativeMousePos)
        }
    }

    override fun onRelease(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        val relativeMousePos = mousePos.minus(posX, posY)

        hoveredChild?.let {
            it.onRelease(relativeMousePos.minus(it.posX, it.posY), clickPos, buttonId)
            if (it.listening) keybordListening = it
        } ?: run {
            updateValues(relativeMousePos, relativeMousePos)
        }
    }

    override fun onDrag(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        super.onDrag(mousePos, clickPos, buttonId)
        val relativeMousePos = mousePos.minus(posX, posY)
        val relativeClickPos = clickPos.minus(posX, posY)

        hoveredChild?.let {
            it.onDrag(relativeMousePos.minus(it.posX, it.posY), relativeClickPos.minus(it.posX, it.posY), buttonId)
        } ?: run {
            updateValues(relativeMousePos, relativeClickPos)
        }
    }

    private fun updateValues(mousePos: Vec2f, clickPos: Vec2f) {
        val relativeX = mousePos.x - 4.0f
        val relativeY = mousePos.y - draggableHeight - 4.0f
        val fieldHeight = fieldHeight

        if (isInPair(clickPos, fieldPos)) {
            saturation = (relativeX / fieldHeight).coerceIn(0.0f, 1.0f)
            brightness = (1.0f - relativeY / fieldHeight).coerceIn(0.0f, 1.0f)
            updateRGBFromHSB()
        } else if (isInPair(clickPos, huePos)) {
            hue = (relativeY / fieldHeight).coerceIn(0.0f, 1.0f)
            updateRGBFromHSB()
        }
    }

    private fun isInPair(mousePos: Vec2f, pair: Pair<Vec2f, Vec2f>) =
        mousePos.x in pair.first.x..pair.second.x && mousePos.y in pair.first.y..pair.second.y

    override fun onKeyInput(keyCode: Int, keyState: Boolean) {
        super.onKeyInput(keyCode, keyState)
        keybordListening?.onKeyInput(keyCode, keyState)
    }

    override fun onRender(absolutePos: Vec2f) {
        super.onRender(absolutePos)

        GlStateUtils.texture2d(false)
        GlStateUtils.smooth(true)

        drawColorField()
        drawHueSlider()
        drawColorPreview()

        GlStateUtils.smooth(false)
        GlStateUtils.texture2d(true)

        for (component in components) {
            if (!component.visible) continue
            GlStateManager.pushMatrix()
             GlStateManager.translate(component.renderPosX, component.renderPosY, 0.0f)
            component.onRender(absolutePos.plus(component.renderPosX, component.renderPosY))
            GlStateManager.popMatrix()
        }
    }

    override fun onPostRender(absolutePos: Vec2f) {
        super.onPostRender(absolutePos)

        for (component in components) {
            if (!component.visible) continue
            GlStateManager.pushMatrix()
             GlStateManager.translate(component.renderPosX, component.renderPosY, 0.0f)
            component.onPostRender(absolutePos.plus(component.renderPosX, component.renderPosY))
            GlStateManager.popMatrix()
        }
    }

    private fun drawColorField() {
        RenderUtils2D.prepareGL()

        // Saturation
        val interpolatedHue = prevHue + (hue - prevHue) * mc.renderPartialTicks
        val rightColor = ColorUtils.hsbToRGB(interpolatedHue, 1.0f, 1.0f, 1.0f)
        val leftColor = ColorRGB(255, 255, 255)

        RenderUtils2D.putVertex(fieldPos.first, leftColor) // Top left
        RenderUtils2D.putVertex(Vec2f(fieldPos.first.x, fieldPos.second.y), leftColor) // Bottom left
        RenderUtils2D.putVertex(Vec2f(fieldPos.second.x, fieldPos.first.y), rightColor) // Top right
        RenderUtils2D.putVertex(fieldPos.second, rightColor) // Bottom right
        RenderUtils2D.draw(GL_TRIANGLE_STRIP)

        // Brightness
        val topColor = ColorRGB(0, 0, 0, 0)
        val bottomColor = ColorRGB(0, 0, 0, 255)
        RenderUtils2D.putVertex(fieldPos.first, topColor) // Top left
        RenderUtils2D.putVertex(Vec2f(fieldPos.first.x, fieldPos.second.y), bottomColor) // Bottom left
        RenderUtils2D.putVertex(Vec2f(fieldPos.second.x, fieldPos.first.y), topColor) // Top right
        RenderUtils2D.putVertex(fieldPos.second, bottomColor) // Bottom right
        RenderUtils2D.draw(GL_TRIANGLE_STRIP)

        RenderUtils2D.releaseGL()

        // Circle pointer
        val interpolatedSaturation = MathUtils.lerp(prevSaturation, saturation, RenderUtils3D.partialTicks)
        val interpolatedBrightness = MathUtils.lerp(prevBrightness, brightness, RenderUtils3D.partialTicks)
        val relativeBrightness = ((1.0f - (1.0f - interpolatedSaturation) * interpolatedBrightness) * 255.0f).toInt()
        val circleColor = ColorRGB(relativeBrightness, relativeBrightness, relativeBrightness)
        val circlePos = Vec2f(
            fieldPos.first.x + fieldHeight * interpolatedSaturation,
            fieldPos.first.y + fieldHeight * (1.0f - interpolatedBrightness)
        )
        RenderUtils2D.drawCircleOutline(circlePos, 4.0f, 32, 1.5f, circleColor)
    }

    private fun drawHueSlider() {
        val color1 = ColorRGB(255, 0, 0) // 0.0
        val color2 = ColorRGB(255, 255, 0) // 0.1666
        val color3 = ColorRGB(0, 255, 0) // 0.3333
        val color4 = ColorRGB(0, 255, 255) // 0.5
        val color5 = ColorRGB(0, 0, 255) // 0.6666
        val color6 = ColorRGB(255, 0, 255) // 0.8333
        val partHeight = (huePos.second.y - huePos.first.y) / 6.0f

        // Hue slider
        RenderUtils2D.prepareGL()
        RenderUtils2D.putVertex(huePos.first, color1)
        RenderUtils2D.putVertex(huePos.first.plus(8.0f, 0.0f), color1)

        RenderUtils2D.putVertex(huePos.first.plus(0.0f, partHeight), color2)
        RenderUtils2D.putVertex(huePos.first.plus(8.0f, partHeight), color2)

        RenderUtils2D.putVertex(huePos.first.plus(0.0f, partHeight * 2.0f), color3)
        RenderUtils2D.putVertex(huePos.first.plus(8.0f, partHeight * 2.0f), color3)

        RenderUtils2D.putVertex(huePos.first.plus(0.0f, partHeight * 3.0f), color4)
        RenderUtils2D.putVertex(huePos.first.plus(8.0f, partHeight * 3.0f), color4)

        RenderUtils2D.putVertex(huePos.first.plus(0.0f, partHeight * 4.0f), color5)
        RenderUtils2D.putVertex(huePos.first.plus(8.0f, partHeight * 4.0f), color5)

        RenderUtils2D.putVertex(huePos.first.plus(0.0f, partHeight * 5.0f), color6)
        RenderUtils2D.putVertex(huePos.first.plus(8.0f, partHeight * 5.0f), color6)

        RenderUtils2D.putVertex(huePos.first.plus(0.0f, partHeight * 6.0f), color1)
        RenderUtils2D.putVertex(huePos.first.plus(8.0f, partHeight * 6.0f), color1)
        RenderUtils2D.draw(GL_TRIANGLE_STRIP)
        RenderUtils2D.releaseGL()

        // Arrow pointer
        val interpolatedHue = prevHue + (hue - prevHue) * mc.renderPartialTicks
        val pointerPosY = huePos.first.y + fieldHeight * interpolatedHue
        RenderUtils2D.drawTriangleOutline(
            Vec2f(huePos.first.x - 5.0f, pointerPosY - 2.0f),
            Vec2f(huePos.first.x - 5.0f, pointerPosY + 2.0f),
            Vec2f(huePos.first.x - 1.0f, pointerPosY),
            1.5f,
            GuiSetting.primary
        )
        RenderUtils2D.drawTriangleOutline(
            Vec2f(huePos.second.x + 1.0f, pointerPosY),
            Vec2f(huePos.second.x + 5.0f, pointerPosY + 2.0f),
            Vec2f(huePos.second.x + 5.0f, pointerPosY - 2.0f),
            1.5f,
            GuiSetting.primary
        )
    }

    private fun drawColorPreview() {
        RenderUtils2D.prepareGL()

        // Previous color
        val prevColor = setting.value.alpha(255)
        RenderUtils2D.drawRectFilled(
            prevColorPos.first.x,
            prevColorPos.first.y,
            prevColorPos.second.x,
            prevColorPos.second.y,
            prevColor
        )

        // Current color
        val currentColor = ColorRGB(r.value, g.value, b.value)
        RenderUtils2D.drawRectFilled(
            currentColorPos.first.x,
            currentColorPos.first.y,
            currentColorPos.second.x,
            currentColorPos.second.y,
            currentColor
        )

        // Previous hex

        RenderUtils2D.releaseGL()
    }

    private fun actionOk() {
        setting.value = ColorRGB(r.value, g.value, b.value, a.value)
        screen.closeWindow(this)
        screen.lastClicked = parent
    }

    private fun actionCancel() {
        screen.closeWindow(this)
        screen.lastClicked = parent
    }

    private fun actionApply() {
        setting.value = ColorRGB(r.value, g.value, b.value, a.value)
    }

    private fun updateRGBFromHSB() {
        val color = ColorUtils.hsbToRGB(hue, saturation, brightness)
        r.value = color.r
        g.value = color.g
        b.value = color.b
    }

    private fun updateHSBFromRGB() {
        val color = ColorUtils.rgbToHSB(r.value, g.value, b.value, 0)
        hue = color.h
        saturation = color.s
        brightness = color.b
    }

    private fun updatePos() {
        // Red slider
        sliderR.forcePosY = draggableHeight
        sliderR.forceWidth = 128.0f

        // Green slider
        sliderG.forcePosY = sliderR.forcePosY + sliderR.forceHeight + 2.0f
        sliderG.forceWidth = 128.0f

        // Blue slider
        sliderB.forcePosY = sliderG.forcePosY + sliderG.forceHeight + 2.0f
        sliderB.forceWidth = 128.0f

        // Alpha slider
        sliderA.forcePosY = sliderB.forcePosY + sliderB.forceHeight + 2.0f
        sliderA.forceWidth = 128.0f

        // Okay button
        buttonOkay.forcePosY = sliderA.forcePosY + sliderA.forceHeight + 2.0f
        buttonOkay.forceWidth = 50.0f

        // Cancel button
        buttonCancel.forcePosY = buttonOkay.forcePosY + buttonOkay.forceHeight + 2.0f
        buttonCancel.forceWidth = 50.0f

        // Apply button
        buttonApply.forcePosY = buttonCancel.forcePosY + buttonCancel.forceHeight + 2.0f
        buttonApply.forceWidth = 50.0f

        // Main window
        dockingH = dev.luna5ama.trollhack.graphics.HAlign.CENTER
        dockingV = dev.luna5ama.trollhack.graphics.VAlign.CENTER
        relativePosX = 0.0f
        relativePosY = 0.0f
        forceHeight = buttonApply.forcePosY + buttonApply.forceHeight + 4.0f
        forceWidth = forceHeight - draggableHeight + 4.0f + 8.0f + 4.0f + 128.0f + 8.0f

        // PositionX of components
        sliderR.forcePosX = forceWidth - 4.0f - 128.0f
        sliderG.forcePosX = forceWidth - 4.0f - 128.0f
        sliderB.forcePosX = forceWidth - 4.0f - 128.0f
        sliderA.forcePosX = forceWidth - 4.0f - 128.0f
        buttonOkay.forcePosX = forceWidth - 4.0f - 50.0f
        buttonCancel.forcePosX = buttonOkay.forcePosX
        buttonApply.forcePosX = buttonOkay.forcePosX

        // Variables
        fieldHeight = forceHeight - draggableHeight - 4.0f
        fieldPos = Pair(
            Vec2f(4.0f, draggableHeight),
            Vec2f(4.0f + fieldHeight, draggableHeight + fieldHeight)
        )
        huePos = Pair(
            Vec2f(4.0f + fieldHeight + 6.0f,  draggableHeight),
            Vec2f(4.0f + fieldHeight + 6.0f + 8.0f, draggableHeight + fieldHeight)
        )
        prevColorPos = Pair(
            Vec2f(sliderR.forcePosX, buttonOkay.forcePosY),
            Vec2f(sliderR.forcePosX + 35.0f, forceHeight - 4.0f)
        )
        currentColorPos = Pair(
            Vec2f(sliderR.forcePosX + 35.0f + 4.0f, buttonOkay.forcePosY),
            Vec2f(sliderR.forcePosX + 35.0f + 4.0f + 35.0f, forceHeight - 4.0f)
        )
    }
}