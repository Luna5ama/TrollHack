package dev.luna5ama.trollhack.gui.rgui.component

import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.setting.settings.impl.other.BindSetting
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import org.lwjgl.input.Keyboard

class BindButton(
    screen: IGuiScreen,
    private val setting: BindSetting
) : Slider(screen, setting.name, setting.description, setting.visibility) {
    override fun onDisplayed() {
        protectedWidth = MainFontRenderer.getWidth(setting.value.toString(), 0.75f)
        super.onDisplayed()
    }

    override fun onRelease(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
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