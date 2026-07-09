package dev.luna5ama.trollhack.modules.impl.client

import dev.luna5ama.trollhack.gui.NullClickGui
import dev.luna5ama.trollhack.manager.managers.ModuleManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.runSafe

object ReloadScript : Module("Reload Script", category = Category.CLIENT) {
    init {
        onEnabled {
            runSafe {
                ModuleManager.modules.removeIf {
                    it.category == Category.SCRIPT
                }
                ModuleManager.loadScript()
            }
            NullClickGui.reloadPanel()
            disable()
        }
    }
}