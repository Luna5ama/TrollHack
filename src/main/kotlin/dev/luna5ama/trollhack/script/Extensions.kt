package dev.luna5ama.trollhack.script

import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.jse.CoerceJavaToLua

fun Any.toLua(): LuaValue = when (this) {
    is Boolean -> LuaValue.valueOf(this)
    is Int -> LuaValue.valueOf(this)
    is Long -> LuaValue.valueOf(this.toInt())
    is Float -> LuaValue.valueOf(this.toDouble())
    is Double -> LuaValue.valueOf(this)
    is String -> LuaValue.valueOf(this)
    is List<*> -> LuaValue.tableOf(this.map { it?.toLua() ?: LuaValue.NIL }.toTypedArray())
    else -> CoerceJavaToLua.coerce(this)
}

fun Varargs.toList() = buildList<LuaValue> {
    val num = narg()
    for (i in 1..num) {
        add(arg(i))
    }
}