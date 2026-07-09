package dev.luna5ama.trollhack.script

import dev.luna5ama.trollhack.graphics.buffer.Render2DUtils
import dev.luna5ama.trollhack.graphics.buffer.Render3DUtils
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction

object RenderLibrary : TwoArgFunction() {
    private val render2DUtils = Render2DUtils.toLua()
    private val render3DUtils = Render3DUtils.toLua()

    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        env["render2DUtils"] = render2DUtils
        env["render3DUtils"] = render3DUtils
        return env
    }
}