package dev.luna5ama.trollhack.manager

import dev.luna5ama.trollhack.command.CommandManager
import dev.luna5ama.trollhack.gui.TrollClickGui
import dev.luna5ama.trollhack.gui.TrollHudEditor
import dev.luna5ama.trollhack.manager.managers.*
import dev.luna5ama.trollhack.utils.Profiler

object ManagerLoader {
    private val managers = buildList {
        add { EntityManager }
        add { EntityMovementManager }
        add { ModuleManager }
        add { ConfigManager }
        add { FriendManager }
        add { CommandManager }
        add { GuiManager }
        add { HotbarSwitchManager }
        add { PlayerPacketManager }
        add { CombatManager }
        add { PlayerPopTotemManager }
    }

    /** may be called out of main thread, so delegate render operations to render thread. **/
    fun load() {
        managers.forEach {
            Profiler.BootstrapProfiler(it()::class.simpleName.toString()) {
                it().load(this)
            }
        }
        TrollHudEditor.reloadPanel()
        TrollClickGui.reloadPanel()
    }
}
