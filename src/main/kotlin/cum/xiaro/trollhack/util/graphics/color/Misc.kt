package cum.xiaro.trollhack.util.graphics.color

import cum.xiaro.trollhack.util.graphics.ColorRGB
import net.minecraft.client.renderer.GlStateManager

fun ColorRGB.setGLColor() {
    GlStateManager.color(rFloat, gFloat, bFloat, aFloat)
}