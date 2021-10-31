package cum.xiaro.trollhack.gui.rgui.component

import cum.xiaro.trollhack.module.modules.client.GuiSetting
import cum.xiaro.trollhack.setting.settings.impl.other.BindSetting
import cum.xiaro.trollhack.util.graphics.font.renderer.MainFontRenderer
import cum.xiaro.trollhack.util.math.vector.Vec2f
import org.lwjgl.input.Keyboard

class BindButton(
    private val setting: BindSetting
) : Slider(setting.name, 0.0f, setting.description, setting.visibility) {

    override val renderProgress: Float
        get() = 0.0f

    override fun onRelease(mousePos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, buttonId)
        if (listening) {
            setting.value.apply {
                if (buttonId > 1) setBind(-buttonId - 1)
            }
        }

        listening = !listening
    }

    override fun onKeyInput(keyCode: Int, keyState: Boolean) {
        super.onKeyInput(keyCode, keyState)
        if (listening && keyCode != Keyboard.KEY_NONE && !keyState) {
            setting.value.apply {
                if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) clear()
                else setBind(keyCode)
                inputField = setting.nameAsString
                listening = false
            }
        }
    }

    override fun onRender(absolutePos: Vec2f) {
        super.onRender(absolutePos)

        val valueText = if (listening) "Listening" else setting.value.toString()

        protectedWidth = MainFontRenderer.getWidth(valueText, 0.75f)
        val posX = renderWidth - protectedWidth - 2.0f
        val posY = renderHeight - 2.0f - MainFontRenderer.getHeight(0.75f)
        MainFontRenderer.drawString(valueText, posX, posY, GuiSetting.text, 0.75f)
    }
}