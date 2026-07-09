package dev.luna5ama.trollhack.utils.input

import imgui.flag.ImGuiMouseCursor
import org.lwjgl.glfw.GLFW

enum class CursorStyle(val cursor: Long, val imgui: Int) {
    DEFAULT(GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR), ImGuiMouseCursor.Arrow),
    CLICK(GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR), ImGuiMouseCursor.Hand),
    TYPE(GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR), ImGuiMouseCursor.TextInput),
    DRAG_X(GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_EW_CURSOR), ImGuiMouseCursor.ResizeEW),
    DRAG_Y(GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NS_CURSOR), ImGuiMouseCursor.ResizeNS),
    CROSSHAIR(GLFW.glfwCreateStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR), ImGuiMouseCursor.ResizeAll)
}