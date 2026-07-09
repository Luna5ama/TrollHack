/*
 * Copyright (c) 2023-2024 CLion 保留所有权利�?All Right Reserved.
 */

package dev.luna5ama.trollhack.utils.input

import org.lwjgl.glfw.GLFW

object KeyboardUtil {

    fun isStringKey(key: Int): Boolean {
        return when (key) {
            GLFW.GLFW_KEY_0 -> true
            GLFW.GLFW_KEY_1 -> true
            GLFW.GLFW_KEY_2 -> true
            GLFW.GLFW_KEY_3 -> true
            GLFW.GLFW_KEY_4 -> true
            GLFW.GLFW_KEY_5 -> true
            GLFW.GLFW_KEY_6 -> true
            GLFW.GLFW_KEY_7 -> true
            GLFW.GLFW_KEY_8 -> true
            GLFW.GLFW_KEY_9 -> true
            GLFW.GLFW_KEY_A -> true
            GLFW.GLFW_KEY_B -> true
            GLFW.GLFW_KEY_C -> true
            GLFW.GLFW_KEY_D -> true
            GLFW.GLFW_KEY_E -> true
            GLFW.GLFW_KEY_F -> true
            GLFW.GLFW_KEY_G -> true
            GLFW.GLFW_KEY_H -> true
            GLFW.GLFW_KEY_I -> true
            GLFW.GLFW_KEY_J -> true
            GLFW.GLFW_KEY_K -> true
            GLFW.GLFW_KEY_L -> true
            GLFW.GLFW_KEY_M -> true
            GLFW.GLFW_KEY_N -> true
            GLFW.GLFW_KEY_O -> true
            GLFW.GLFW_KEY_P -> true
            GLFW.GLFW_KEY_Q -> true
            GLFW.GLFW_KEY_R -> true
            GLFW.GLFW_KEY_S -> true
            GLFW.GLFW_KEY_T -> true
            GLFW.GLFW_KEY_U -> true
            GLFW.GLFW_KEY_V -> true
            GLFW.GLFW_KEY_W -> true
            GLFW.GLFW_KEY_X -> true
            GLFW.GLFW_KEY_Y -> true
            GLFW.GLFW_KEY_Z -> true
            else -> false
        }
    }

}