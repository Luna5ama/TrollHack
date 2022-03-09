package me.luna.trollhack.util.graphics

import me.luna.trollhack.event.AlwaysListening
import me.luna.trollhack.module.modules.client.GuiSetting
import me.luna.trollhack.util.extension.fastCeil
import me.luna.trollhack.util.interfaces.Helper

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