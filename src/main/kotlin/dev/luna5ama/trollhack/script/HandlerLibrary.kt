package dev.luna5ama.trollhack.script

import dev.luna5ama.trollhack.event.EventClasses
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.modules.LuaModule
import dev.luna5ama.trollhack.utils.runSafe
import dev.luna5ama.trollhack.utils.shortName
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua

class HandlerLibrary(val module: LuaModule) : TwoArgFunction() {
    val handler = object : ThreeArgFunction() {
        override fun call(eventName: LuaValue, alwaysListening: LuaValue, func: LuaValue): LuaValue {
            val name = eventName.checkjstring()
            val callback = func.checkfunction()
            val clz = EventClasses.classes.find { it.shortName == name } ?: throw LuaError("Event not found: $name")
            handler(module, clz, 0, alwaysListening.checkboolean()) {
                callback.call(CoerceJavaToLua.coerce(it))
            }
            return NONE
        }
    }

    val nonNullHandler = object : ThreeArgFunction() {
        override fun call(eventName: LuaValue, alwaysListening: LuaValue, func: LuaValue): LuaValue {
            val name = eventName.checkjstring()
            val callback = func.checkfunction()
            val clz = EventClasses.classes.find { it.shortName == name } ?: throw LuaError("Event not found: $name")
            handler(module, clz, 0, alwaysListening.checkboolean()) {
                runSafe {
                    callback.call(CoerceJavaToLua.coerce(this@runSafe.cut()), CoerceJavaToLua.coerce(it))
                }
            }
            return NONE
        }
    }

    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        val handlers = LuaTable.tableOf(0, 30)
        handlers["handler"] = handler
        handlers["non_null_handler"] = nonNullHandler
        env["handlers"] = handlers
        return handlers
    }
}