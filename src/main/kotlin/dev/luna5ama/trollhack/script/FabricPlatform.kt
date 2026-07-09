package dev.luna5ama.trollhack.script

import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import dev.luna5ama.trollhack.modules.LuaModule
import dev.luna5ama.trollhack.script.settings.SettingsLibrary
import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.LuaValue
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.jse.JseBaseLib
import org.luaj.vm2.lib.jse.JseIoLib
import org.luaj.vm2.lib.jse.JseMathLib
import org.luaj.vm2.lib.jse.LuajavaLib
import kotlin.io.path.Path

object FabricPlatform {
    fun defaultGlobals(module: LuaModule): Globals {
        val globals = emptyGlobals()
        globals.load(JseBaseLib())
        globals.load(JseIoLib())
        globals.load(JseMathLib())
        globals.load(HandlerLibrary(module))
        globals.load(BaseLibrary)
        globals.load(RenderLibrary)
        globals.load(SettingsLibrary(module))
        globals.load(PacketLibrary)
        PackageLib.DEFAULT_LUA_PATH = Path(TrollHackMod.FOLDER.resolve("scripts").absolutePath, "dummy.lua").toString().replace("dummy.lua", "?.lua")
        globals.load(PackageLib())
        globals.load(LuajavaLib())
        val new = globals["luajava"]["new"]
        globals["luajava"] = LuaValue.NIL
        globals["java"] = LuaValue.tableOf()
        globals["java"]["new"] = new
        return globals
    }

    fun emptyGlobals(): Globals {
        val globals = Globals()
        LoadState.install(globals)
        LuaC.install(globals)
        return globals
    }

    fun handlerGlobals(module: LuaModule): Globals {
        val globals = emptyGlobals()
        globals.load(HandlerLibrary(module))
        return globals
    }
}