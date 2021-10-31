package cum.xiaro.trollhack.util.text

import cum.xiaro.trollhack.util.graphics.color.EnumTextColor
import net.minecraft.util.text.TextFormatting

fun formatValue(value: String) = TextFormatting.GRAY format "[$value]"

fun formatValue(value: Char) = TextFormatting.GRAY format "[$value]"

fun formatValue(value: Any) = TextFormatting.GRAY format "[$value]"

fun formatValue(value: Int) = TextFormatting.GRAY format "($value)"

infix fun TextFormatting.format(value: Any) = "$this$value${TextFormatting.RESET}"

infix fun TextFormatting.format(value: Int) = "$this$value${TextFormatting.RESET}"

infix fun EnumTextColor.format(value: Any) = "$this$value${TextFormatting.RESET}"