package dev.luna5ama.trollhack.modules

import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import dev.luna5ama.trollhack.script.FabricPlatform
import dev.luna5ama.trollhack.script.toList
import dev.luna5ama.trollhack.utils.SafetyWarnings
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Prototype
import org.lwjgl.glfw.GLFW

class LuaModule(
    val source: String, val prototype: Prototype, initialInfo: LuaValue
) : AbstractModule(
    name = initialInfo["module_name"].checkjstring(),
    description = initialInfo["module_desc"].optjstring(""),
    alwaysEnable = initialInfo["module_always_enable"].optboolean(false),
    alwaysListening = initialInfo["module_always_listening"].optboolean(false),
    hidden = initialInfo["module_hidden"].optboolean(false),
    enableByDefault = initialInfo["module_enable_by_default"].optboolean(false),
    alias = initialInfo["module_aliases"].opttable(LuaTable.tableOf()).unpack().toList().map { it.checkjstring() }.toSet(),
    priority = initialInfo["module_priority"].optint(0),
    internal = false,
    category = Category.SCRIPT,
    defaultBind = GLFW.GLFW_KEY_UNKNOWN
) {
    private val compiledScript: LuaValue

    init {
        val warnings = mutableListOf<String>()
        if (hidden) warnings.add("module_hidden was set to true in the script")
        if (alwaysEnable) warnings.add("module_always_enable was set to true in the script")
        if (alwaysListening) warnings.add("module_always_listening was set to true in the script")
        if (enableByDefault) warnings.add("module_enable_by_default was set to true in the script")
        if (warnings.isNotEmpty()) {
            TrollHackMod.LOGGER.warn(SafetyWarnings.build("The lua module $name", warnings))
        }
        val env = FabricPlatform.defaultGlobals(this)
        env.loader.load(prototype, name.toString(), env).call()
        compiledScript = env
        val initializer = compiledScript["init"].checkfunction()
        initializer.call()
    }
}