package dev.luna5ama.trollhack.utils.input

import org.lwjgl.glfw.GLFW

enum class CursorStyle(val cursor: Long) {
    DEFAULT(GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR)),
    CLICK(GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR)),
    TYPE(GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR)),
    DRAG_X(GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_EW_CURSOR)),
    DRAG_Y(GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NS_CURSOR)),
    CROSSHAIR(GLFW.glfwCreateStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR))
}
