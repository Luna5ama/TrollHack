package dev.luna5ama.trollhack

import dev.luna5ama.trollhack.graphics.skia.GuiProjection
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import net.minecraft.client.Minecraft

typealias RS = RenderSystem

object RenderSystem {
    private val mc: Minecraft
        get() = Minecraft.getInstance()

    val guiProjection = GuiProjection()
    private val currentGuiProjection: GuiProjection
        get() = guiProjection.update(width, height, ClientSettings.guiScale)

    val renderScaleF: Float
        get() = currentGuiProjection.scale
    val renderScale: Double
        get() = renderScaleF.toDouble()
    val width: Int
        get() = mc.window.width
    val height: Int
        get() = mc.window.height
    val widthF: Float
        get() = width.toFloat()
    val heightF: Float
        get() = height.toFloat()
    val widthD: Double
        get() = width.toDouble()
    val heightD: Double
        get() = height.toDouble()
    val scaledWidth: Double
        get() = currentGuiProjection.width.toDouble()
    val scaledHeight: Double
        get() = currentGuiProjection.height.toDouble()
    val scaledWidthF: Float
        get() = currentGuiProjection.width
    val scaledHeightF: Float
        get() = currentGuiProjection.height
    val mouseX: Double
        get() = mc.mouseHandler.xpos() / renderScale
    val mouseXF: Float
        get() = mouseX.toFloat()
    val mouseY: Double
        get() = mc.mouseHandler.ypos() / renderScale
    val mouseYF: Float
        get() = mouseY.toFloat()

    init {
        System.setProperty("joml.forceUnsafe", "true")
        System.setProperty("joml.fastmath", "true")
        System.setProperty("joml.sinLookup", "true")
        System.setProperty("joml.format", "false")
        System.setProperty("joml.useMathFma", "true")
    }

    fun init() = Unit

    fun getCurrentMaxFps(original: Int): Int = original
}
