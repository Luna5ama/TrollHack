package dev.luna5ama.trollhack.graphics.imgui

import imgui.ImGui
import imgui.ImVec4
import imgui.type.ImBoolean
import kotlin.reflect.KProperty

fun ImVec4.toFloatArray() = floatArrayOf(x, y, z, w)

// Color Utilities
fun colorConvertU32ToFloat4(`in`: Int): ImVec4 {
    val value = ImVec4()
    ImGui.colorConvertU32ToFloat4(value, `in`)
    return value
}

operator fun ImBoolean.getValue(thisRef: Any?, property: KProperty<*>): Boolean = this.get()
operator fun ImBoolean.setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) = this.set(value)