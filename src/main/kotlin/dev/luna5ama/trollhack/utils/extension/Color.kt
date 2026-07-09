package dev.luna5ama.trollhack.utils.extension

import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import java.awt.Color

fun Color.injectAlpha(alpha: Int): ColorRGBA = ColorRGBA(this.red,this.green,this.blue,alpha)