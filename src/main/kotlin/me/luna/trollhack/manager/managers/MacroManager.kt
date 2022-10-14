package me.luna.trollhack.manager.managers

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.luna.trollhack.TrollHackMod
import me.luna.trollhack.command.CommandManager
import me.luna.trollhack.event.events.InputEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.manager.Manager
import me.luna.trollhack.util.ConfigUtils
import me.luna.trollhack.util.text.MessageSendUtils
import java.io.File

object MacroManager : Manager() {
    private var macroMap = List<MutableList<String>>(256, ::ArrayList)
    val isEmpty get() = macroMap.isEmpty()
    val macros: List<List<String>> get() = macroMap

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val parser = JsonParser()
    private val file get() = File("${TrollHackMod.DIRECTORY}/macros.json")

    init {
        listener<InputEvent.Keyboard> {
            if (it.state) {
                sendMacro(it.key)
            }
        }
    }

    fun loadMacros(): Boolean {
        ConfigUtils.fixEmptyJson(file)

        return try {
            val jsonObject = parser.parse(file.readText())
            val newMap = List<ArrayList<String>>(256, ::ArrayList)
            for ((id, list) in jsonObject.asJsonObject.entrySet()) {
                list.asJsonArray.mapTo(newMap[id.toInt()]) { it.asString }
            }
            macroMap = newMap
            TrollHackMod.logger.info("Macro loaded")
            true
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed loading macro", e)
            false
        }
    }

    fun saveMacros(): Boolean {
        return try {
            val jsonObject = JsonObject()
            for (i in macroMap.indices) {
                jsonObject.add(i.toString(), gson.toJsonTree(macroMap[i]))
            }
            file.bufferedWriter().use {
                gson.toJson(jsonObject, it)
            }
            TrollHackMod.logger.info("Macro saved")
            true
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed saving macro", e)
            false
        }
    }

    /**
     * Sends the message or command, depending on which one it is
     * @param keyCode int keycode of the key the was pressed
     */
    private fun sendMacro(keyCode: Int) {
        val macros = getMacros(keyCode)
        for (macro in macros) {
            if (macro.startsWith(CommandManager.prefix)) { // this is done instead of just sending a chat packet so it doesn't add to the chat history
                MessageSendUtils.sendTrollCommand(macro) // ie, the false here
            } else {
                MessageManager.sendMessageDirect(macro)
            }
        }
    }

    fun getMacros(keycode: Int) = macroMap[keycode]

    fun setMacro(keycode: Int, macro: String) {
        val list = macroMap[keycode]
        list.clear()
        list.add(macro)
    }

    fun addMacro(keycode: Int, macro: String) {
        macroMap[keycode].add(macro)
    }

    fun removeMacro(keycode: Int) {
        macroMap[keycode].clear()
    }

}