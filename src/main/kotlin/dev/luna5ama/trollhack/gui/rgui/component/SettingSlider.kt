package dev.luna5ama.trollhack.gui.rgui.component

import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.gui.rgui.MouseState
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.setting.settings.impl.number.FloatSetting
import dev.luna5ama.trollhack.setting.settings.impl.number.IntegerSetting
import dev.luna5ama.trollhack.setting.settings.impl.number.NumberSetting
import dev.luna5ama.trollhack.util.math.MathUtils
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import org.lwjgl.input.Keyboard
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

class SettingSlider(
    screen: IGuiScreen,
    val setting: NumberSetting<*>
) : Slider(screen, setting.name, setting.description, setting.visibility) {
    private val range = setting.range.endInclusive.toDouble() - setting.range.start.toDouble()
    private val settingStep = if (setting.step.toDouble() > 0.0) setting.step else getDefaultStep()
    private val stepDouble = settingStep.toDouble()
    private val fineStepDouble = setting.fineStep.toDouble()
    private val places = when (setting) {
        is IntegerSetting -> 1
        is FloatSetting -> MathUtils.decimalPlaces(settingStep.toFloat())
        else -> MathUtils.decimalPlaces(settingStep.toDouble())
    }

    private var preDragMousePos = Vec2f(0.0f, 0.0f)

    override val progress: Float
        get() {
            if (!setting.isVisible) {
                return 0.0f
            }

            if (mouseState != MouseState.DRAG && !listening) {
                val min = setting.range.start.toDouble()
                var flooredValue =
                    floor((renderProgress.current * range + setting.range.start.toDouble()) / stepDouble) * stepDouble
                if (abs(flooredValue) == 0.0) flooredValue = 0.0

                if (abs(flooredValue - setting.value.toDouble()) >= stepDouble) {
                    return ((setting.value.toDouble() - min) / range).toFloat()
                }
            }
            return Float.NaN
        }

    private fun getDefaultStep() = when (setting) {
        is IntegerSetting -> range / 20
        is FloatSetting -> range / 20.0f
        else -> range / 20.0
    }

    override fun onDisplayed() {
        protectedWidth = MainFontRenderer.getWidth(setting.toString(), 0.75f)
        super.onDisplayed()
    }

    override fun onStopListening(success: Boolean) {
        if (success) {
            inputField.toDoubleOrNull()?.let { setting.setValue(it.toString()) }
        }

        super.onStopListening(success)
        inputField = ""
    }

    override fun onClick(mousePos: Vec2f, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        if (buttonId == 0) {
            preDragMousePos = mousePos
            updateValue(mousePos)
        }
    }

    override fun onRelease(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        if (buttonId == 1) {
            if (!listening) {
                listening = true
                inputField = setting.value.toString()
            } else {
                onStopListening(false)
            }
        } else if (buttonId == 0 && listening) {
            onStopListening(true)
        }
    }

    override fun onDrag(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        super.onDrag(mousePos, clickPos, buttonId)
        if (!listening && buttonId == 0) updateValue(mousePos)
    }

    private fun updateValue(mousePos: Vec2f) {
        val value = if (!Keyboard.isKeyDown(Keyboard.KEY_LMENU)) mousePos.x / width
        else (preDragMousePos.x + (mousePos.x - preDragMousePos.x) * 0.1f) / width

        val step = if (Keyboard.isKeyDown(Keyboard.KEY_LMENU)) fineStepDouble else stepDouble
        var roundedValue =
            MathUtils.round(round((value * range + setting.range.start.toDouble()) / step) * step, places)
        if (abs(roundedValue) == 0.0) roundedValue = 0.0

        setting.setValue(roundedValue)
        renderProgress.update(value)
    }

    override fun onKeyInput(keyCode: Int, keyState: Boolean) {
        super.onKeyInput(keyCode, keyState)
        val typedChar = Keyboard.getEventCharacter()
        if (keyState) {
            when (keyCode) {
                Keyboard.KEY_RETURN -> {
                    onStopListening(true)
                }
                Keyboard.KEY_BACK, Keyboard.KEY_DELETE -> {
                    inputField = inputField.substring(0, max(inputField.length - 1, 0))
                    if (inputField.isBlank()) inputField = "0"
                }
                else -> if (isNumber(typedChar)) {
                    if (inputField == "0" && (typedChar.isDigit() || typedChar == '-')) {
                        inputField = ""
                    }
                    inputField += typedChar
                }
            }
        }
    }

    private fun isNumber(char: Char) =
        char.isDigit()
            || char == '-'
            || char == '.'
            || char.equals('e', true)

    override fun onRender(absolutePos: Vec2f) {
        val valueText = setting.toString()
        protectedWidth = MainFontRenderer.getWidth(valueText, 0.75f)

        super.onRender(absolutePos)
        if (!listening) {
            val posX = renderWidth - protectedWidth - 2.0f
            val posY = renderHeight - 2.0f - MainFontRenderer.getHeight(0.75f)
            MainFontRenderer.drawString(valueText, posX, posY, GuiSetting.text, 0.75f)
        }
    }
}