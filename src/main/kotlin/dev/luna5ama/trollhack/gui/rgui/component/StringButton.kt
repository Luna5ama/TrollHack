package dev.luna5ama.trollhack.gui.rgui.component

import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.gui.rgui.MouseState
import dev.luna5ama.trollhack.setting.settings.impl.primitive.StringSetting
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import org.lwjgl.input.Keyboard
import kotlin.math.max

class StringButton(
    screen: IGuiScreen,
    val setting: StringSetting
) : BooleanSlider(screen, setting.name, setting.description, setting.visibility) {
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

    override fun onRelease(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        when (buttonId) {
            0 -> {
                if (listening) {
                    onStopListening(true)
                } else {
                    startListening()
                }
            }
            1 -> {
                if (listening) {
                    onStopListening(false)
                } else {
                    startListening()
                }
            }
        }
    }

    private fun startListening() {
        listening = true
        inputField = setting.value
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