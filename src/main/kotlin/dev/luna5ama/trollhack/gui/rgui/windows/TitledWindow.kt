package dev.luna5ama.trollhack.gui.rgui.windows

import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import dev.luna5ama.trollhack.util.text.format
import net.minecraft.util.text.TextFormatting

/**
 * Window with rectangle and title rendering
 */
open class TitledWindow(
    screen: IGuiScreen,
    name: CharSequence,
    uiSettingGroup: UiSettingGroup
) : BasicWindow(screen, name, uiSettingGroup) {
    override val draggableHeight: Float get() = MainFontRenderer.getHeight() + 6.0f

    override val minimizable get() = true

    override fun onRender(absolutePos: Vec2f) {
        super.onRender(absolutePos)
        MainFontRenderer.drawString(TextFormatting.BOLD format name, 3.0f, 3.5f, GuiSetting.text)
    }
}