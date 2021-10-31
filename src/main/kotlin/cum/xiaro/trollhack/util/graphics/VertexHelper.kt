package cum.xiaro.trollhack.util.graphics

import cum.xiaro.trollhack.util.math.vector.Vec2d
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.math.Vec3d

object VertexHelper {
    private val tessellator = Tessellator.getInstance()
    private val buffer = tessellator.buffer

    fun begin(mode: Int) {
        buffer.begin(mode, DefaultVertexFormats.POSITION_COLOR)
    }

    fun put(pos: Vec3d, color: ColorRGB) {
        put(pos.x, pos.y, pos.z, color)
    }

    fun put(x: Double, y: Double, z: Double, color: ColorRGB) {
        buffer.pos(x, y, z).color(color.r, color.g, color.b, color.a).endVertex()
    }

    fun put(pos: Vec2d, color: ColorRGB) {
        put(pos.x, pos.y, color)
    }

    fun put(x: Double, y: Double, color: ColorRGB) {
        buffer.pos(x, y, 0.0).color(color.r, color.g, color.b, color.a).endVertex()
    }

    fun end() {
        GlStateUtils.useProgram(0)
        tessellator.draw()
    }
}