package dev.luna5ama.trollhack.utils.input

import com.mojang.blaze3d.platform.InputConstants
import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import net.minecraft.client.KeyMapping
import net.minecraft.client.KeyMapping.Category
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW


object KeyBinds {
    private val CATEGORY = Category.register(Identifier.parse(TrollHackMod.ID))
    var OPEN_GUI: KeyMapping =
        KeyMapping("Click Gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY)

    fun apply(binds: Array<KeyMapping>): Array<KeyMapping> {
        // Add key binding
        val newBinds = arrayOfNulls<KeyMapping>(binds.size + 1)
        System.arraycopy(binds, 0, newBinds, 0, binds.size)
        newBinds[binds.size] = OPEN_GUI
        return newBinds.requireNoNulls()
    }

    fun getKey(bind: KeyMapping): Int {
        return bind.defaultKey.value
    }
}
