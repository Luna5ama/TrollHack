package dev.luna5ama.trollhack.gui.hudgui

import dev.luna5ama.trollhack.event.IListenerOwner
import dev.luna5ama.trollhack.event.ListenerOwner
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.gui.rgui.windows.BasicWindow
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.setting.GuiConfig
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.setting.configs.AbstractConfig
import dev.luna5ama.trollhack.translation.ITranslateSrc
import dev.luna5ama.trollhack.translation.TranslateSrc
import dev.luna5ama.trollhack.translation.TranslateType
import dev.luna5ama.trollhack.util.Bind
import dev.luna5ama.trollhack.util.extension.rootName
import dev.luna5ama.trollhack.util.graphics.RenderUtils2D
import dev.luna5ama.trollhack.util.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.util.interfaces.Alias
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.interfaces.Nameable
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import org.lwjgl.opengl.GL11.glScalef

abstract class AbstractHudElement(
    name: String,
    final override val alias: Array<out CharSequence>,
    val category: Category,
    val description: String,
    val alwaysListening: Boolean,
    enabledByDefault: Boolean,
    config: AbstractConfig<out Nameable>
) : BasicWindow(name, 20.0f, 20.0f, 100.0f, 50.0f, SettingGroup.HUD_GUI, config),
    Alias,
    IListenerOwner by ListenerOwner() {

    val bind by setting("Bind", Bind())
    val scale by setting("Scale", 1.0f, 0.1f..4.0f, 0.05f)
    val default = setting("Default", false, isTransient = true)

    override val resizable = false

    final override val minWidth: Float get() = MainFontRenderer.getHeight() * scale * 2.0f
    final override val minHeight: Float get() = MainFontRenderer.getHeight() * scale

    final override val maxWidth: Float get() = hudWidth * scale
    final override val maxHeight: Float get() = hudHeight * scale

    open val hudWidth: Float get() = 20f
    open val hudHeight: Float get() = 10f

    val settingList
        get() = GuiConfig.getGroupOrPut(SettingGroup.HUD_GUI.groupName).getGroupOrPut(internalName).getSettings()

    init {
        safeParallelListener<TickEvent.Pre> {
            if (!visible) return@safeParallelListener
            width = maxWidth
            height = maxHeight
        }
    }

    override fun onGuiInit() {
        super.onGuiInit()
        if (alwaysListening || visible) subscribe()
    }

    override fun onClosed() {
        super.onClosed()
        if (alwaysListening || visible) subscribe()
    }

    final override fun onTick() {
        super.onTick()
    }

    final override fun onRender(absolutePos: Vec2f) {
        renderFrame()
        glScalef(scale, scale, scale)
        renderHud()
    }

    open fun renderHud() {}

    open fun renderFrame() {
        RenderUtils2D.drawRectFilled(renderWidth, renderHeight, GuiSetting.backGround)
        RenderUtils2D.drawRectOutline(renderWidth, renderHeight, 1.0f, GuiSetting.primary)
    }

    init {
        visibleSetting.valueListeners.add { _, it ->
            if (it) {
                subscribe()
                lastActiveTime = System.currentTimeMillis()
            } else if (!alwaysListening) {
                unsubscribe()
            }
        }

        default.valueListeners.add { _, it ->
            if (it) {
                settingList.filter { it != visibleSetting && it != default }.forEach { it.resetValue() }
                default.value = false
                NoSpamMessage.sendMessage(Companion, "$name $defaultMessage!")
            }
        }

        if (!enabledByDefault) visible = false
    }

    enum class Category(override val displayName: CharSequence) : DisplayEnum {
        CLIENT("Client"),
        COMBAT("Combat"),
        PLAYER("Player"),
        WORLD("World"),
        MISC("Misc")
    }

    private companion object : ITranslateSrc by TranslateSrc("hud") {
        val defaultMessage = TranslateType.COMMON key ("setToDefault" to "Set to defaults")
    }
}