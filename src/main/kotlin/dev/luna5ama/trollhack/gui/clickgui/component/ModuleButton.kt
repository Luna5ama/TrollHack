package dev.luna5ama.trollhack.gui.clickgui.component

import dev.luna5ama.trollhack.gui.clickgui.TrollClickGui
import dev.luna5ama.trollhack.gui.clickgui.window.ModuleSettingWindow
import dev.luna5ama.trollhack.gui.rgui.component.Button
import dev.luna5ama.trollhack.module.AbstractModule

class ModuleButton(
    override val screen: TrollClickGui,
    val module: AbstractModule
) : Button(
    screen,
    module.name,
    module.description
) {
    override val progress: Float
        get() = if (module.isEnabled) 1.0f else 0.0f

    private val settingWindow by lazy { ModuleSettingWindow(screen, module) }

    init {
        action { _, buttonId ->
            when (buttonId) {
                0 -> module.toggle()
                1 -> screen.displayWindow(settingWindow)
            }
        }
    }
}