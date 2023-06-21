package dev.luna5ama.trollhack.gui.clickgui

import dev.luna5ama.trollhack.gui.AbstractTrollGui
import dev.luna5ama.trollhack.gui.clickgui.component.ModuleButton
import dev.luna5ama.trollhack.gui.rgui.Component
import dev.luna5ama.trollhack.gui.rgui.windows.ListWindow
import dev.luna5ama.trollhack.module.ModuleManager
import dev.luna5ama.trollhack.module.modules.client.ClickGUI
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.extension.remove
import org.lwjgl.input.Keyboard

object TrollClickGui : AbstractTrollGui() {
    private val moduleWindows = ArrayList<ListWindow>()

    init {
        val allButtons = ModuleManager.modules
            .groupBy { it.category.displayName }
            .mapValues { (_, modules) -> modules.map { ModuleButton(this, it) } }

        var posX = 0.0f
        var posY = 0.0f
        val screenWidth = mc.displayWidth / GuiSetting.scaleFactor

        for ((category, buttons) in allButtons) {
            val window = ListWindow(this, category, Component.SettingGroup.CLICK_GUI)
            window.forcePosX = posX
            window.forcePosY = posY
            window.forceWidth = 80.0f
            window.forceHeight = 400.0f

            window.children.addAll(buttons)
            moduleWindows.add(window)
            posX += 80.0f

            if (posX > screenWidth) {
                posX = 0.0f
                posY += 100.0f
            }
        }

        windows.addAll(moduleWindows)
    }

    override fun onGuiClosed() {
        super.onGuiClosed()
        setModuleButtonVisibility { true }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == ClickGUI.bind.value.key && !searching && lastClicked?.keybordListening == null) {
            ClickGUI.disable()
        } else {
            super.keyTyped(typedChar, keyCode)

            val string = typedString.remove(' ')

            if (string.isNotEmpty()) {
                setModuleButtonVisibility { moduleButton ->
                    moduleButton.module.name.contains(string, true)
                        || moduleButton.module.alias.any { it.contains(string, true) }
                }
            } else {
                setModuleButtonVisibility { true }
            }
        }
    }

    private fun setModuleButtonVisibility(function: (ModuleButton) -> Boolean) {
        windows.asSequence()
            .filterIsInstance<ListWindow>()
            .flatMap { it.children.asSequence() }
            .filterIsInstance<ModuleButton>()
            .forEach { it.visible = function(it) }
    }
}