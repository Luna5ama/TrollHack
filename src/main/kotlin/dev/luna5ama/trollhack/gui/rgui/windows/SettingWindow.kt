package dev.luna5ama.trollhack.gui.rgui.windows

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.gui.rgui.component.*
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.setting.groups.SettingGroup
import dev.luna5ama.trollhack.setting.settings.AbstractSetting
import dev.luna5ama.trollhack.setting.settings.impl.number.NumberSetting
import dev.luna5ama.trollhack.setting.settings.impl.other.BindSetting
import dev.luna5ama.trollhack.setting.settings.impl.other.ColorSetting
import dev.luna5ama.trollhack.setting.settings.impl.primitive.BooleanSetting
import dev.luna5ama.trollhack.setting.settings.impl.primitive.EnumSetting
import dev.luna5ama.trollhack.setting.settings.impl.primitive.StringSetting
import dev.luna5ama.trollhack.util.ClipboardUtils
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.lwjgl.input.Keyboard
import kotlin.math.max
import kotlin.math.min

abstract class SettingWindow<T : Any>(
    screen: IGuiScreen,
    name: CharSequence,
    val element: T,
    uiSettingGroup: UiSettingGroup
) : ListWindow(screen, name, uiSettingGroup) {

    override val minWidth get() = max(super.minWidth, optimalWidth)
    override val minHeight by ::optimalHeight
    override val maxHeight by ::optimalHeight

    override val minimizable get() = false

    protected abstract val elementSettingGroup: SettingGroup
    protected abstract val elementSettingList: List<AbstractSetting<*>>

    private val colorPickers = Object2ObjectOpenHashMap<ColorSetting, ColorPicker>()
    private var activeColorPicker: ColorPicker? = null

    private fun displayColorPicker(colorSetting: ColorSetting) {
        activeColorPicker?.let {
            screen.closeWindow(it)
        }

        val colorPicker = colorPickers.getOrPut(colorSetting) { ColorPicker(screen, this, colorSetting) }
        screen.displayWindow(colorPicker)
        activeColorPicker = colorPicker
    }

    override fun onDisplayed() {
        screen.lastClicked = this

        children.clear()
        for (setting in elementSettingList) {
            when (setting) {
                is BooleanSetting -> SettingButton(screen, setting)
                is NumberSetting -> SettingSlider(screen, setting)
                is EnumSetting -> EnumSlider(screen, setting)
                is ColorSetting -> Button(
                    screen,
                    setting.name,
                    setting.description,
                    setting.visibility
                ).action { _, _ -> displayColorPicker(setting) }
                is StringSetting -> StringButton(screen, setting)
                is BindSetting -> BindButton(screen, setting)
                else -> null
            }?.also {
                children.add(it)
            }
        }

        super.onDisplayed()

        val mousePos = screen.mousePos
        val screenWidth = mc.displayWidth / GuiSetting.scaleFactor
        val screenHeight = mc.displayHeight / GuiSetting.scaleFactor

        forcePosX = if (mousePos.x + width <= screenWidth) {
            mousePos.x
        } else {
            mousePos.x - this.width
        }

        forcePosY = min(mousePos.y, screenHeight - height)
    }

    override fun onRelease(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        (hoveredChild as? Slider)?.let {
            if (it != keybordListening) {
                (keybordListening as? Slider?)?.onStopListening(false)
                keybordListening = it.takeIf { it.listening }
            }
        }
    }

    override fun onTick() {
        val activeColorPicker = activeColorPicker
        if (screen.lastClicked !== this && (activeColorPicker == null || screen.lastClicked !== activeColorPicker)) {
            screen.closeWindow(this)
            return
        }

        super.onTick()
        if ((keybordListening as? Slider?)?.listening == false) keybordListening = null
        Keyboard.enableRepeatEvents(keybordListening != null)
    }

    override fun onGuiClosed() {
        super.onGuiClosed()
        screen.closeWindow(this)
    }

    override fun onClosed() {
        super.onClosed()
        keybordListening = null
        activeColorPicker?.let {
            screen.closeWindow(it)
        }
        activeColorPicker = null
    }

    override fun onKeyInput(keyCode: Int, keyState: Boolean) {
        val listening = keybordListening
        if (listening != null) {
            listening.onKeyInput(keyCode, keyState)
        } else {
            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                try {
                    when (keyCode) {
                        Keyboard.KEY_C -> {
                            val jsonString = gson.toJson(elementSettingGroup.write())
                            ClipboardUtils.copyToClipboard(jsonString)
                        }
                        Keyboard.KEY_V -> {
                            val jsonString = ClipboardUtils.pasteFromClipboard()
                            if (jsonString.isNotBlank()) {
                                val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
                                elementSettingGroup.read(jsonObject)
                            }
                        }
                    }
                } catch (e: Exception) {
                    NoSpamMessage.sendError("Failed to copy/paste settings")
                    TrollHackMod.logger.error("Failed to copy/paste settings", e)
                }
            }
        }
    }

    private companion object {
        val gson = GsonBuilder().setPrettyPrinting().create()
    }
}