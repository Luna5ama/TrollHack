package cum.xiaro.trollhack.util.graphics

import cum.xiaro.trollhack.util.Wrapper
import cum.xiaro.trollhack.util.accessor.renderPosX
import cum.xiaro.trollhack.util.accessor.renderPosY
import cum.xiaro.trollhack.util.accessor.renderPosZ
import cum.xiaro.trollhack.util.graphics.mask.EnumFacingMask
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.culling.ICamera
import net.minecraft.entity.Entity
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import org.lwjgl.opengl.GL11.GL_LINES
import org.lwjgl.opengl.GL11.GL_QUADS

/**
 * @author Xiaro
 *
 * Created by Xiaro on 30/07/20
 */
class ESPRenderer {
    private val frustumCamera: ICamera = Frustum()
    private var toRender0: MutableList<Info> = ArrayList()

    val toRender: List<Info>
        get() = toRender0

    var aFilled = 0
    var aOutline = 0
    var aTracer = 0
    var thickness = 2f
    var through = true
    var tracerOffset = 50

    val size: Int
        get() = toRender0.size

    fun add(entity: Entity, color: ColorRGB) {
        add(entity, color, EnumFacingMask.ALL)
    }

    fun add(entity: Entity, color: ColorRGB, sides: Int) {
        val partialTicks = 1.0f - RenderUtils3D.partialTicks
        val x = (entity.lastTickPosX - entity.posX) * partialTicks
        val y = (entity.lastTickPosY - entity.posY) * partialTicks
        val z = (entity.lastTickPosZ - entity.posZ) * partialTicks
        val interpolatedBox = entity.renderBoundingBox.offset(x, y, z)
        add(interpolatedBox, color, sides)
    }

    fun add(pos: BlockPos, color: ColorRGB) {
        add(pos, color, EnumFacingMask.ALL)
    }

    fun add(pos: BlockPos, color: ColorRGB, sides: Int) {
        add(AxisAlignedBB(pos), color, sides)
    }

    fun add(box: AxisAlignedBB, color: ColorRGB) {
        add(box, color, EnumFacingMask.ALL)
    }

    fun add(box: AxisAlignedBB, color: ColorRGB, sides: Int) {
        add(Info(box, color, sides))
    }

    fun add(info: Info) {
        toRender0.add(info)
    }

    fun replaceAll(list: MutableList<Info>) {
        toRender0 = list
    }

    fun clear() {
        toRender0.clear()
    }

    fun render(clear: Boolean) {
        val filled = aFilled != 0
        val outline = aOutline != 0
        val tracer = aTracer != 0
        if (toRender0.isEmpty() || (!filled && !outline && !tracer)) return

        frustumCamera.setPosition(Wrapper.minecraft.renderManager.renderPosX, Wrapper.minecraft.renderManager.renderPosY, Wrapper.minecraft.renderManager.renderPosZ)

        if (through) GlStateManager.disableDepth()
        GlStateManager.glLineWidth(thickness)

        if (filled) {
            for ((box, color, sides) in toRender0) {
                val a = (aFilled * (color.a / 255.0f)).toInt()
                if (frustumCamera.isBoundingBoxInFrustum(box)) {
                    RenderUtils3D.drawBox(box, color.alpha(a), sides)
                }
            }
            RenderUtils3D.draw(GL_QUADS)
        }

        if (outline || tracer) {
            if (outline) {
                for ((box, color, _) in toRender0) {
                    val a = (aOutline * (color.a / 255.0f)).toInt()
                    if (frustumCamera.isBoundingBoxInFrustum(box)) {
                        RenderUtils3D.drawOutline(box, color.alpha(a))
                    }
                }
            }
            if (tracer) {
                for ((box, color, _) in toRender0) {
                    val a = (aTracer * (color.a / 255.0f)).toInt()
                    val offset = (tracerOffset - 50) / 100.0 * (box.maxY - box.minY)
                    val offsetBox = box.center.add(0.0, offset, 0.0)
                    RenderUtils3D.drawLineTo(offsetBox, color.alpha(a))
                }
            }

            RenderUtils3D.draw(GL_LINES)
        }

        if (clear) clear()
        GlStateManager.enableDepth()
    }

    private enum class Type {
        FILLED, OUTLINE, TRACER
    }

    data class Info(val box: AxisAlignedBB, val color: ColorRGB, val sides: Int) {
        constructor(box: AxisAlignedBB) : this(box, ColorRGB(255, 255, 255), EnumFacingMask.ALL)
        constructor(box: AxisAlignedBB, color: ColorRGB) : this(box, color, EnumFacingMask.ALL)
        constructor(pos: BlockPos) : this(AxisAlignedBB(pos), ColorRGB(255, 255, 255), EnumFacingMask.ALL)
        constructor(pos: BlockPos, color: ColorRGB) : this(AxisAlignedBB(pos), color, EnumFacingMask.ALL)
        constructor(pos: BlockPos, color: ColorRGB, sides: Int) : this(AxisAlignedBB(pos), color, sides)
    }
}