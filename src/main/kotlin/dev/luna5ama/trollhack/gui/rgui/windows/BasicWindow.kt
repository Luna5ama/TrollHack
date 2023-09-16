package dev.luna5ama.trollhack.gui.rgui.windows

import dev.luna5ama.trollhack.graphics.RenderUtils2D
import dev.luna5ama.trollhack.graphics.shaders.WindowBlurShader
import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.setting.GuiConfig
import dev.luna5ama.trollhack.setting.configs.AbstractConfig
import dev.luna5ama.trollhack.util.interfaces.Nameable
import dev.luna5ama.trollhack.util.math.vector.Vec2f

/**
 * Window with rectangle rendering
 */
open class BasicWindow(
    screen: IGuiScreen,
    name: CharSequence,
    uiSettingGroup: UiSettingGroup,
    config: AbstractConfig<out Nameable> = GuiConfig
) : CleanWindow(name, screen, uiSettingGroup, config) {
    override fun onRender(absolutePos: Vec2f) {
        super.onRender(absolutePos)
        WindowBlurShader.render(renderWidth, renderHeight)
        if (GuiSetting.titleBar) {
            RenderUtils2D.drawRectFilled(0.0f, draggableHeight, renderWidth, renderHeight, GuiSetting.backGround)
        } else {
            RenderUtils2D.drawRectFilled(0.0f, 0.0f, renderWidth, renderHeight, GuiSetting.backGround)
        }
        if (GuiSetting.windowOutline) {
            RenderUtils2D.drawRectOutline(0.0f, 0.0f, renderWidth, renderHeight, 1.0f, GuiSetting.primary.alpha(255))
        }
        if (GuiSetting.titleBar) {
            RenderUtils2D.drawRectFilled(0.0f, 0.0f, renderWidth, draggableHeight, GuiSetting.primary)
        }
    }
}