package dev.luna5ama.trollhack.script

import dev.luna5ama.trollhack.utils.ChatUtils
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction

object BaseLibrary : TwoArgFunction() {
    private val chatUtils = ChatUtils.toLua()

    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        env["chat_utils"] = chatUtils
        return env
    }
}