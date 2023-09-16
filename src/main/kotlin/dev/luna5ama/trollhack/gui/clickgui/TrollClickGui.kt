package dev.luna5ama.trollhack.gui.clickgui

import dev.fastmc.common.EnumMap
import dev.luna5ama.trollhack.graphics.Resolution
import dev.luna5ama.trollhack.gui.AbstractTrollGui
import dev.luna5ama.trollhack.gui.clickgui.component.ModuleButton
import dev.luna5ama.trollhack.gui.rgui.Component
import dev.luna5ama.trollhack.gui.rgui.windows.ListWindow
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.ModuleManager
import dev.luna5ama.trollhack.module.modules.client.ClickGUI
import dev.luna5ama.trollhack.util.extension.remove
import dev.luna5ama.trollhack.util.threads.runSynchronized
import org.lwjgl.input.Keyboard

object TrollClickGui : AbstractTrollGui() {
    private val moduleWindows = EnumMap<Category, ListWindow>()

    override var searchString: String
        get() = super.searchString
        set(value) {
            super.searchString = value

            val string = value.remove(' ')

            if (string.isNotEmpty()) {
                setModuleButtonVisibility { moduleButton ->
                    moduleButton.module.allNames.any { it.contains(string, true) }
                }
            } else {
                setModuleButtonVisibility { true }
            }
        }

    init {
        var posX = 0.0f
        var posY = 0.0f

        for (category in Category.values()) {
            val window = ListWindow(this, category.displayName, Component.UiSettingGroup.CLICK_GUI)
            window.forcePosX = posX
            window.forcePosY = posY
            window.forceWidth = 80.0f
            window.forceHeight = 400.0f

            ModuleManager.modules.asSequence()
                .filter { it.category == category }
                .mapTo(window.children) { ModuleButton(this, it) }

            moduleWindows[category] = window
            posX += 80.0f

            if (posX > Resolution.trollWidthF) {
                posX = 0.0f
                posY += 100.0f
            }
        }

        windows.runSynchronized { addAll(moduleWindows.values) }
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
        }
    }

    private inline fun setModuleButtonVisibility(function: (ModuleButton) -> Boolean) {
        moduleWindows.values.asSequence()
            .flatMap { it.children.asSequence() }
            .filterIsInstance<ModuleButton>()
            .forEach { it.visible = function(it) }
    }
}