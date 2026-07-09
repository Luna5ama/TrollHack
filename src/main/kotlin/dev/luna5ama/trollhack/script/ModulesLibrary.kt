package dev.luna5ama.trollhack.script

import dev.luna5ama.trollhack.manager.managers.ModuleManager
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

object ModulesLibrary : TwoArgFunction() {
    private val getModuleByName = object : OneArgFunction() {
        override fun call(name: LuaValue): LuaValue {
            return ModuleManager.getModuleByName(name.checkjstring())?.toLua() ?: LuaValue.NIL
        }
    }
    override fun call(arg1: LuaValue, env: LuaValue): LuaValue {
        val modules = LuaValue.tableOf()
        modules["get_module_by_name"] = getModuleByName
        env["modules"] = modules
        return modules
    }
}