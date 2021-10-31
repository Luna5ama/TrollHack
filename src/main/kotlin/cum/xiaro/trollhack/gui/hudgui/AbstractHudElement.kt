package cum.xiaro.trollhack.gui.hudgui

import cum.xiaro.trollhack.util.interfaces.Alias
import cum.xiaro.trollhack.util.interfaces.DisplayEnum
import cum.xiaro.trollhack.util.interfaces.Nameable
import cum.xiaro.trollhack.event.IListenerOwner
import cum.xiaro.trollhack.event.ListenerOwner
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeParallelListener
import cum.xiaro.trollhack.gui.rgui.windows.BasicWindow
import cum.xiaro.trollhack.module.modules.client.GuiSetting
import cum.xiaro.trollhack.module.modules.client.Hud
import cum.xiaro.trollhack.setting.GuiConfig
import cum.xiaro.trollhack.setting.GuiConfig.setting
import cum.xiaro.trollhack.setting.configs.AbstractConfig
import cum.xiaro.trollhack.util.Bind
import cum.xiaro.trollhack.util.extension.rootName
import cum.xiaro.trollhack.util.graphics.RenderUtils2D
import cum.xiaro.trollhack.util.graphics.font.renderer.MainFontRenderer
import cum.xiaro.trollhack.util.math.vector.Vec2f
import cum.xiaro.trollhack.util.text.MessageSendUtils
import org.lwjgl.opengl.GL11.glScalef

abstract class AbstractHudElement(
    name: String,
    final override val alias: Array<out CharSequence>,
    val category: Category,
    val description: String,
    val alwaysListening: Boolean,
    enabledByDefault: Boolean,
    config: AbstractConfig<out Nameable>
) : BasicWindow(name, 20.0f, 20.0f, 100.0f, 50.0f, SettingGroup.HUD_GUI, config), Alias, IListenerOwner by ListenerOwner() {

    val bind by setting("Bind", Bind())
    val scale by setting("Scale", 1.0f, 0.1f..4.0f, 0.05f)
    val default = setting("Default", false)

    override val resizable = false

    final override val minWidth: Float get() = MainFontRenderer.getHeight() * scale * 2.0f
    final override val minHeight: Float get() = MainFontRenderer.getHeight() * scale

    final override val maxWidth: Float get() = hudWidth * scale
    final override val maxHeight: Float get() = hudHeight * scale

    open val hudWidth: Float get() = 20f
    open val hudHeight: Float get() = 10f

    val settingList get() = GuiConfig.getGroupOrPut(SettingGroup.HUD_GUI.groupName).getGroupOrPut(rootName).getSettings()

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
        RenderUtils2D.drawRectFilled(Vec2f.ZERO, Vec2f(renderWidth, renderHeight), GuiSetting.backGround)
        RenderUtils2D.drawRectOutline(Vec2f.ZERO, Vec2f(renderWidth, renderHeight), 1.0f, GuiSetting.outline)
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
                MessageSendUtils.sendNoSpamChatMessage("$name Set to defaults!")
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

    protected companion object {
        val primaryColor get() = Hud.primaryColor
        val secondaryColor get() = Hud.secondaryColor
    }

}