package cum.xiaro.trollhack.gui.rgui.windows

import cum.xiaro.trollhack.util.interfaces.Nameable
import cum.xiaro.trollhack.module.modules.client.GuiSetting
import cum.xiaro.trollhack.setting.GuiConfig
import cum.xiaro.trollhack.setting.configs.AbstractConfig
import cum.xiaro.trollhack.util.graphics.RenderUtils2D
import cum.xiaro.trollhack.util.graphics.shaders.WindowBlurShader
import cum.xiaro.trollhack.util.math.vector.Vec2f

/**
 * Window with rectangle rendering
 */
open class BasicWindow(
    name: CharSequence,
    posX: Float,
    posY: Float,
    width: Float,
    height: Float,
    settingGroup: SettingGroup,
    config: AbstractConfig<out Nameable> = GuiConfig
) : CleanWindow(name, posX, posY, width, height, settingGroup, config) {
    override fun onRender(absolutePos: Vec2f) {
        super.onRender(absolutePos)
        if (GuiSetting.windowBlur) {
            WindowBlurShader.render(renderWidth, renderHeight)
        }
        if (GuiSetting.titleBar) {
            RenderUtils2D.drawRectFilled(Vec2f(0.0f, draggableHeight), Vec2f(renderWidth, renderHeight), GuiSetting.backGround)
        } else {
            RenderUtils2D.drawRectFilled(Vec2f.ZERO, Vec2f(renderWidth, renderHeight), GuiSetting.backGround)
        }
        if (GuiSetting.windowOutline) {
            RenderUtils2D.drawRectOutline(Vec2f.ZERO, Vec2f(renderWidth, renderHeight), 1.0f, GuiSetting.primary.alpha(255))
        }
        if (GuiSetting.titleBar) {
            RenderUtils2D.drawRectFilled(Vec2f.ZERO, Vec2f(renderWidth, draggableHeight), GuiSetting.primary)
        }
    }

}