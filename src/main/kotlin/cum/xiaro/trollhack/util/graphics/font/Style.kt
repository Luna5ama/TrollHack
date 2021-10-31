package cum.xiaro.trollhack.util.graphics.font

import java.awt.Font

@Suppress("UNUSED")
enum class Style(val code: String, val codeChar: Char, val styleConst: Int) {
    REGULAR("§r", 'r', Font.PLAIN),
    BOLD("§l", 'l', Font.BOLD),
    ITALIC("§o", 'o', Font.ITALIC)
}