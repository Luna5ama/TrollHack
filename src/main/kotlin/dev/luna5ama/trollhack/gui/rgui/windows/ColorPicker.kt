package dev.luna5ama.trollhack.gui.rgui.windows

import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.gui.rgui.WindowComponent
import dev.luna5ama.trollhack.gui.rgui.component.Button
import dev.luna5ama.trollhack.gui.rgui.component.SettingSlider
import dev.luna5ama.trollhack.gui.rgui.component.Slider
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.setting.settings.impl.number.IntegerSetting
import dev.luna5ama.trollhack.setting.settings.impl.other.ColorSetting
import dev.luna5ama.trollhack.util.graphics.*
import dev.luna5ama.trollhack.util.graphics.color.ColorRGB
import dev.luna5ama.trollhack.util.graphics.color.ColorUtils
import dev.luna5ama.trollhack.util.math.MathUtils
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import org.lwjgl.opengl.GL11.*

class ColorPicker(
    screen: IGuiScreen,
    private val parent: WindowComponent,
    private val setting: ColorSetting
) : TitledWindow(
    screen,
    "Color Picker",
    SettingGroup.NONE
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

    private val components = arrayOf(sliderR, sliderG, sliderB, sliderA, buttonOkay, buttonCancel)

    override fun onDisplayed() {
        r.value = setting.value.r
        g.value = setting.value.g
        b.value = setting.value.b
        a.value = setting.value.a

        updatePos()
        updateHSBFromRGB()
        super.onDisplayed()
        for (component in components) component.onDisplayed()
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
            glPushMatrix()
            glTranslatef(component.renderPosX, component.renderPosY, 0.0f)
            component.onRender(absolutePos.plus(component.renderPosX, component.renderPosY))
            glPopMatrix()
        }
    }

    override fun onPostRender(absolutePos: Vec2f) {
        super.onPostRender(absolutePos)

        for (component in components) {
            if (!component.visible) continue
            glPushMatrix()
            glTranslatef(component.renderPosX, component.renderPosY, 0.0f)
            component.onPostRender(absolutePos.plus(component.renderPosX, component.renderPosY))
            glPopMatrix()
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
        val height = (hueLinePos.second.y - hueLinePos.first.y) / 6.0f

        // Hue slider
        RenderUtils2D.prepareGL()
        RenderUtils2D.putVertex(hueLinePos.first, color1)
        RenderUtils2D.putVertex(hueLinePos.first.plus(8.0f, 0.0f), color1)

        RenderUtils2D.putVertex(hueLinePos.first.plus(0.0f, height), color2)
        RenderUtils2D.putVertex(hueLinePos.first.plus(8.0f, height), color2)

        RenderUtils2D.putVertex(hueLinePos.first.plus(0.0f, height * 2.0f), color3)
        RenderUtils2D.putVertex(hueLinePos.first.plus(8.0f, height * 2.0f), color3)

        RenderUtils2D.putVertex(hueLinePos.first.plus(0.0f, height * 3.0f), color4)
        RenderUtils2D.putVertex(hueLinePos.first.plus(8.0f, height * 3.0f), color4)

        RenderUtils2D.putVertex(hueLinePos.first.plus(0.0f, height * 4.0f), color5)
        RenderUtils2D.putVertex(hueLinePos.first.plus(8.0f, height * 4.0f), color5)

        RenderUtils2D.putVertex(hueLinePos.first.plus(0.0f, height * 5.0f), color6)
        RenderUtils2D.putVertex(hueLinePos.first.plus(8.0f, height * 5.0f), color6)

        RenderUtils2D.putVertex(hueLinePos.second, color1)
        RenderUtils2D.putVertex(hueLinePos.second.plus(8.0f, height * 0.0f), color1)
        RenderUtils2D.draw(GL_TRIANGLE_STRIP)
        RenderUtils2D.releaseGL()

        // Arrow pointer
        val interpolatedHue = prevHue + (hue - prevHue) * mc.renderPartialTicks
        val pointerPosY = huePos.first.y + fieldHeight * interpolatedHue
        RenderUtils2D.drawTriangleOutline(
            Vec2f(huePos.first.x - 6.0f, pointerPosY - 2.0f),
            Vec2f(huePos.first.x - 6.0, pointerPosY + 2.0),
            Vec2f(huePos.first.x - 2.0f, pointerPosY),
            1.5f,
            GuiSetting.primary
        )
        RenderUtils2D.drawTriangleOutline(
            Vec2f(huePos.second.x + 2.0f, pointerPosY),
            Vec2f(huePos.second.x + 6.0, pointerPosY + 2.0),
            Vec2f(huePos.second.x + 6.0f, pointerPosY - 2.0f),
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
        sliderR.posY = 4.0f + draggableHeight
        sliderR.width = 128.0f

        // Green slider
        sliderG.posY = sliderR.posY + sliderR.height + 4.0f
        sliderG.width = 128.0f

        // Blue slider
        sliderB.posY = sliderG.posY + sliderG.height + 4.0f
        sliderB.width = 128.0f

        // Alpha slider
        sliderA.posY = sliderB.posY + sliderB.height + 4.0f
        sliderA.width = 128.0f

        // Okay button
        buttonOkay.posY = sliderA.posY + sliderA.height + 4.0f
        buttonOkay.width = 50.0f

        // Cancel button
        buttonCancel.posY = buttonOkay.posY + (buttonOkay.height + 4.0f) * 2.0f
        buttonCancel.width = 50.0f

        // Main window
        dockingH = HAlign.CENTER
        dockingV = VAlign.CENTER
        relativePosX = 0.0f
        relativePosY = 0.0f
        height = buttonCancel.posY + buttonCancel.height + 4.0f
        width = height - draggableHeight + 4.0f + 8.0f + 4.0f + 128.0f + 8.0f

        // PositionX of components
        sliderR.posX = width - 4.0f - 128.0f
        sliderG.posX = width - 4.0f - 128.0f
        sliderB.posX = width - 4.0f - 128.0f
        sliderA.posX = width - 4.0f - 128.0f
        buttonOkay.posX = width - 4.0f - 50.0f
        buttonCancel.posX = width - 4.0f - 50.0f

        // Variables
        fieldHeight = height - 8.0f - draggableHeight
        fieldPos = Pair(
            Vec2f(4.0f, 4.0f + draggableHeight),
            Vec2f(4.0f + fieldHeight, 4.0f + fieldHeight + draggableHeight)
        )
        huePos = Pair(
            Vec2f(4.0f + fieldHeight + 8.0f, 4.0f + draggableHeight),
            Vec2f(4.0f + fieldHeight + 8.0f + 8.0f, 4.0f + fieldHeight + draggableHeight)
        )
        hueLinePos = Pair(
            Vec2f(4.0f + fieldHeight + 8.0f, 4.0f + draggableHeight),
            Vec2f(4.0f + fieldHeight + 8.0f, 4.0f + fieldHeight + draggableHeight)
        )
        prevColorPos = Pair(
            Vec2f(sliderR.posX, buttonOkay.posY),
            Vec2f(sliderR.posX + 35.0f, buttonCancel.posY - 4.0f)
        )
        currentColorPos = Pair(
            Vec2f(sliderR.posX + 35.0f + 4.0f, buttonOkay.posY),
            Vec2f(sliderR.posX + 35.0f + 4.0f + 35.0f, buttonCancel.posY - 4.0f)
        )
    }
}