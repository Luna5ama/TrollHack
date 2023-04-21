package dev.luna5ama.trollhack.util.graphics

import dev.luna5ama.trollhack.event.AlwaysListening
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.extension.fastCeil
import dev.luna5ama.trollhack.util.interfaces.Helper

object Resolution : AlwaysListening, Helper {
    val widthI
        get() = mc.displayWidth

    val heightI
        get() = mc.displayHeight

    val heightF
        get() = mc.displayHeight.toFloat()

    val widthF
        get() = mc.displayWidth.toFloat()

    val trollWidthF
        get() = widthF / GuiSetting.scaleFactorFloat

    val trollHeightF
        get() = heightF / GuiSetting.scaleFactorFloat

    val trollWidthI
        get() = trollWidthF.fastCeil()

    val trollHeightI
        get() = trollHeightF.fastCeil()
}