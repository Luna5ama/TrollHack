package cum.xiaro.trollhack.util.graphics

import cum.xiaro.trollhack.util.extension.fastCeil
import cum.xiaro.trollhack.event.AlwaysListening
import cum.xiaro.trollhack.module.modules.client.GuiSetting
import cum.xiaro.trollhack.util.interfaces.Helper

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