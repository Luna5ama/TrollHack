package dev.luna5ama.trollhack.manager

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import dev.luna5ama.trollhack.command.CommandManager
import dev.luna5ama.trollhack.gui.NullClickGui
import dev.luna5ama.trollhack.gui.NullHudEditor
import dev.luna5ama.trollhack.manager.managers.*
import dev.luna5ama.trollhack.utils.Profiler
import dev.luna5ama.trollhack.graphics.font.UnicodeFontRenderer
import dev.luna5ama.trollhack.utils.threads.RenderThreadCoroutine

object ManagerLoader {
    private val managers = buildList {
        add { UnicodeFontManager }
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
        NullHudEditor.reloadPanel()
        NullClickGui.reloadPanel()
        runBlocking {
            RenderThreadCoroutine.async {
                Profiler.BootstrapProfiler("Render Utils") {
                    UnicodeFontRenderer.refresh()
                }
            }.await()
        }
    }
}