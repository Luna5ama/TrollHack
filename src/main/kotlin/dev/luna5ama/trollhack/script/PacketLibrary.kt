package dev.luna5ama.trollhack.script

import dev.luna5ama.trollhack.utils.Reflections
import net.minecraft.network.protocol.Packet
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua

object PacketLibrary : TwoArgFunction() {
    private val packets = Reflections.getSubTypesOf(Packet::class.java)
        .filter { "$" !in it.name }.partition { it.simpleName.startsWith("Serverbound") }

    override fun call(arg1: LuaValue, env: LuaValue): LuaValue {
        val table = LuaTable.tableOf()
        val (c2sPackets, s2cPackets) = packets
        val c2s = LuaTable.tableOf()
        c2sPackets.forEach {
            c2s[it.simpleName] = CoerceJavaToLua.coerce(it)
        }
        val s2c = LuaTable.tableOf()
        s2cPackets.forEach {
            s2c[it.simpleName] = CoerceJavaToLua.coerce(it)
        }

        table["serverbound"] = c2s
        table["clientbound"] = s2c
        env["packets"] = table

        return table
    }
}