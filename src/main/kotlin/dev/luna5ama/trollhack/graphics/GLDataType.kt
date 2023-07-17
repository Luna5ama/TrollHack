package dev.luna5ama.trollhack.graphics

import org.lwjgl.opengl.GL11

enum class GLDataType(val glEnum: Int, val size: Int) {
    GL_BYTE(GL11.GL_BYTE, 1),
    GL_UNSIGNED_BYTE(GL11.GL_UNSIGNED_BYTE, 1),
    GL_SHORT(GL11.GL_SHORT, 2),
    GL_UNSIGNED_SHORT(GL11.GL_UNSIGNED_SHORT, 2),
    GL_INT(GL11.GL_INT, 4),
    GL_UNSIGNED_INT(GL11.GL_UNSIGNED_INT, 4),
    GL_FLOAT(GL11.GL_FLOAT, 4),
}