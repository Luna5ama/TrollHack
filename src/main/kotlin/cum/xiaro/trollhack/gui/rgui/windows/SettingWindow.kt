package cum.xiaro.trollhack.gui.rgui.windows

import cum.xiaro.trollhack.gui.rgui.component.*
import cum.xiaro.trollhack.setting.settings.AbstractSetting
import cum.xiaro.trollhack.setting.settings.impl.number.NumberSetting
import cum.xiaro.trollhack.setting.settings.impl.other.BindSetting
import cum.xiaro.trollhack.setting.settings.impl.other.ColorSetting
import cum.xiaro.trollhack.setting.settings.impl.primitive.BooleanSetting
import cum.xiaro.trollhack.setting.settings.impl.primitive.EnumSetting
import cum.xiaro.trollhack.setting.settings.impl.primitive.StringSetting
import cum.xiaro.trollhack.util.math.vector.Vec2f
import org.lwjgl.input.Keyboard

abstract class SettingWindow<T : Any>(
    name: CharSequence,
    val element: T,
    posX: Float,
    posY: Float,
    settingGroup: SettingGroup
) : ListWindow(name, posX, posY, 150.0f, 200.0f, settingGroup) {

    override val minWidth: Float get() = 100.0f
    override val minHeight: Float get() = draggableHeight

    override val minimizable get() = false

    var listeningChild: Slider? = null; private set
    private var initialized = false

    protected abstract fun getSettingList(): List<AbstractSetting<*>>

    override fun onGuiInit() {
        super.onGuiInit()
        if (!initialized) {
            for (setting in getSettingList()) {
                when (setting) {
                    is BooleanSetting -> SettingButton(setting)
                    is NumberSetting -> SettingSlider(setting)
                    is EnumSetting -> EnumSlider(setting)
                    is ColorSetting -> Button(setting.name, { displayColorPicker(setting) }, setting.description, setting.visibility)
                    is StringSetting -> StringButton(setting)
                    is BindSetting -> BindButton(setting)
                    else -> null
                }?.also {
                    children.add(it)
                }
            }
            initialized = true
        }
    }

    private fun displayColorPicker(colorSetting: ColorSetting) {
        ColorPicker.visible = true
        ColorPicker.setting = colorSetting
        ColorPicker.onDisplayed()
    }

    override fun onDisplayed() {
        super.onDisplayed()
        lastActiveTime = System.currentTimeMillis() + 1000L
    }

    override fun onRelease(mousePos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, buttonId)
        (hoveredChild as? Slider)?.let {
            if (it != listeningChild) {
                listeningChild?.onStopListening(false)
                listeningChild = it.takeIf { it.listening }
            }
        }
    }

    override fun onTick() {
        super.onTick()
        if (listeningChild?.listening == false) listeningChild = null
        Keyboard.enableRepeatEvents(listeningChild != null)
    }

    override fun onClosed() {
        super.onClosed()
        listeningChild = null
        ColorPicker.visible = false
    }

    override fun onKeyInput(keyCode: Int, keyState: Boolean) {
        listeningChild?.onKeyInput(keyCode, keyState)
    }

}