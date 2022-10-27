package me.luna.trollhack.gui.rgui.component

import me.luna.trollhack.setting.settings.impl.primitive.StringSetting
import me.luna.trollhack.util.math.vector.Vec2f
import org.lwjgl.input.Keyboard
import kotlin.math.max

class StringButton(val setting: StringSetting) : BooleanSlider(setting.name, setting.description, setting.visibility) {
    override val progress: Float
        get() = if (!listening) 1.0f else 0.0f

    override fun onStopListening(success: Boolean) {
        if (success) {
            setting.setValue(inputField)
        }

        super.onStopListening(success)
        inputField = ""
    }

    override fun onMouseInput(mousePos: Vec2f) {
        super.onMouseInput(mousePos)
        if (!listening) {
            inputField = if (mouseState == MouseState.NONE) ""
            else setting.value
        }
    }

    override fun onTick() {
        super.onTick()
        if (!listening) {
            inputField = if (mouseState != MouseState.NONE) setting.value
            else ""
        }
    }

    override fun onRelease(mousePos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, buttonId)
        if (buttonId == 1) {
            if (!listening) {
                listening = true
                inputField = setting.value
            } else {
                onStopListening(false)
            }
        } else if (buttonId == 0 && listening) {
            onStopListening(true)
        }
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
                }
                else -> if (typedChar >= ' ') {
                    inputField += typedChar
                }
            }
        }
    }
}