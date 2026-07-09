package dev.luna5ama.trollhack.script.settings

import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import dev.luna5ama.trollhack.modules.AbstractModule
import dev.luna5ama.trollhack.modules.LuaModule
import dev.luna5ama.trollhack.script.toList
import dev.luna5ama.trollhack.script.toLua
import dev.luna5ama.trollhack.utils.BiPredicate
import dev.luna5ama.trollhack.utils.Combiner
import dev.luna5ama.trollhack.utils.Predicate
import dev.luna5ama.trollhack.utils.extension.component6
import dev.luna5ama.trollhack.utils.extension.component7
import dev.luna5ama.trollhack.utils.extension.component8
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction

class SettingsLibrary(val module: LuaModule) : TwoArgFunction() {
    private val intSetting = object : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (name, value, min, max, step, visibility, description, consumers) = args.toList()
            val setting = with (module) {
                setting(name.checkjstring(), value.checkint(), min.checkint()..max.checkint(),
                    step.optint(1), toVisibilityFunc(visibility), description.optjstring(""),
                    consumers.opttable(tableOf()).toList().map<LuaValue, BiPredicate<Int, Int>> { toConsumerFunc(it) }.toMutableList())
            }
            return LuaSetting(setting)
        }
    }

    private val doubleSetting = object : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (name, value, min, max, step, visibility, description, consumers) = args.toList()
            val setting = with (module) {
                setting(name.checkjstring(), value.checkdouble(), min.checkdouble()..max.checkdouble(),
                    step.optdouble(0.1), toVisibilityFunc(visibility), description.optjstring(""),
                    consumers.opttable(tableOf()).toList().map<LuaValue, BiPredicate<Double, Double>> { toConsumerFunc(it) }.toMutableList())
            }
            return LuaSetting(setting)
        }
    }

    private val stringSetting = object : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (name, value, visibility, description, consumers) = args.toList()
            val setting = with (module) {
                setting(name.checkjstring(), value.checkjstring(), toVisibilityFunc(visibility), description.optjstring(""),
                    consumers.opttable(tableOf()).toList().map { toConsumerFunc(it) })
            }
            return LuaSetting(setting)
        }
    }

    private val booleanSetting = object : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (name, value, visibility, description, consumers) = args.toList()
            val setting = with (module) {
                setting(name.checkjstring(), value.checkboolean(), toVisibilityFunc(visibility), description.optjstring(""),
                    consumers.opttable(tableOf()).toList().map<LuaValue, BiPredicate<Boolean, Boolean>> { toConsumerFunc(it) }.toMutableList())
            }
            return LuaSetting(setting)
        }
    }

    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        TrollHackMod.LOGGER.info("Initializing setting library for module $module")
        val table = LuaTable.tableOf()
        table["int_setting"] = intSetting
        table["double_setting"] = doubleSetting
        table["string_setting"] = stringSetting
        table["boolean_setting"] = booleanSetting
        env["settings"] = table
        return table
    }
    
    companion object {
        fun <T> toVisibilityFunc(luafunc: LuaValue): Predicate<T> = Predicate {
            try {
                luafunc.checkfunction().invoke().checkboolean(1)
            } catch (e: Exception) {
                true
            }
        }
        
        fun <T : Any> toConsumerFunc(luafunc: LuaValue): BiPredicate<T, T> = BiPredicate { _, value ->
            try {
                luafunc.checkfunction().invoke(value.toLua())
            } catch (_: Exception) {}
            true
        }
    }
}