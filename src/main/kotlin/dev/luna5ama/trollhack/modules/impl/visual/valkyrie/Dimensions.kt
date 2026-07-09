package dev.luna5ama.trollhack.modules.impl.visual.valkyrie

import net.minecraft.client.Minecraft

class Dimensions {
    var hScreen: Float = 0f
    var wScreen: Float = 0f
    var degreesPerPixel: Float = 0f
    var xMid: Float = 0f
    var yMid: Float = 0f

    var wFrame: Float = 0f
    var hFrame: Float = 0f
    var lFrame: Float = 0f
    var rFrame: Float = 0f
    var tFrame: Float = 0f
    var bFrame: Float = 0f

    fun update(client: Minecraft) {
        hScreen = client.window.guiScaledHeight.toFloat()
        wScreen = client.window.guiScaledWidth.toFloat()

        degreesPerPixel = hScreen / client.options.fov().get()
        xMid = wScreen / 2
        yMid = hScreen / 2

        wFrame = wScreen * 0.5f
        hFrame = hScreen * 0.5f

        lFrame = ((wScreen - wFrame) / 2)
        rFrame = lFrame + wFrame

        tFrame = ((hScreen - hFrame) / 2)
        bFrame = tFrame + hFrame
    }
}