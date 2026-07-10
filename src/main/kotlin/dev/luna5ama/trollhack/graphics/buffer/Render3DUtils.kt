package dev.luna5ama.trollhack.graphics.buffer

import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.graphics.GLHelper
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects.draw
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.matrix.scope
import dev.luna5ama.trollhack.utils.state.FrameValue
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.glLineWidth
import dev.luna5ama.trollhack.RenderSystem as RS

object Render3DUtils {
    var lastProjectionMatrix = Matrix4f()
    var lastModelViewMatrix = Matrix4f()
    var lastWorldSpaceMatrix = Matrix4f()

    private val currentMatrix by FrameValue {
        val camera = mc.gameRenderer.mainCamera ?: return@FrameValue Matrix4f()
        val position = Matrix4f(lastWorldSpaceMatrix).translate(camera.position().toVector3f().mul(-1f))
        val projection = Matrix4f(lastProjectionMatrix)
        val modelView = Matrix4f(lastModelViewMatrix)

        projection.mul(modelView).mul(position)
    }

    fun worldToScreen(pos: Vec3): Vec3 {
//        val camera = mc.gameRenderer.camera

        val viewport = IntArray(4)
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport)

        val transformedCoordinates = Vector4f(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(), 1f)

//        val xRot = (RotationAxis.POSITIVE_X.rotationDegrees(camera.pitch))
//        val yRot = (RotationAxis.POSITIVE_Y.rotationDegrees(camera.yaw + 180))
//        val position = Matrix4f().identity().rotate(xRot).rotate(yRot).translate(camera.pos.toVector3f().mul(-1f))
//        val projection = Matrix4f(lastProjectionMatrix)
//        val modelView = Matrix4f(lastModelViewMatrix)

        val target = Vector3f()
        currentMatrix.project(
            transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target
        )

        val displayHeight = mc.window.height
        return Vec3(
            target.x.toDouble() / RS.renderScale,
            (displayHeight - target.y).toDouble() / RS.renderScale,
            target.z.toDouble()
        )
    }

    fun drawBox(box: AABB, color: ColorRGBA) {
        GLHelper.cull = false
        GLHelper.depth = false
        GLHelper.blend = true
        RS.matrixLayer.scope {
            GL11.GL_TRIANGLE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
                universal(box.maxX, box.minY, box.minZ, color)
                universal(box.maxX, box.minY, box.maxZ, color)
                universal(box.minX, box.minY, box.minZ, color)
                universal(box.minX, box.minY, box.maxZ, color)
            }
            GL11.GL_TRIANGLE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
                universal(box.maxX, box.maxY, box.minZ, color)
                universal(box.minX, box.maxY, box.minZ, color)
                universal(box.maxX, box.maxY, box.maxZ, color)
                universal(box.minX, box.maxY, box.maxZ, color)
            }
            GL11.GL_TRIANGLE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
                universal(box.maxX, box.minY, box.minZ, color)
                universal(box.minX, box.minY, box.minZ, color)
                universal(box.maxX, box.maxY, box.minZ, color)
                universal(box.minX, box.maxY, box.minZ, color)
            }
            GL11.GL_TRIANGLE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
                universal(box.minX, box.minY, box.maxZ, color)
                universal(box.maxX, box.minY, box.maxZ, color)
                universal(box.minX, box.maxY, box.maxZ, color)
                universal(box.maxX, box.maxY, box.maxZ, color)
            }
            GL11.GL_TRIANGLE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
                universal(box.minX, box.minY, box.minZ, color)
                universal(box.minX, box.minY, box.maxZ, color)
                universal(box.minX, box.maxY, box.minZ, color)
                universal(box.minX, box.maxY, box.maxZ, color)
            }
            GL11.GL_TRIANGLE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
                universal(box.maxX, box.minY, box.maxZ, color)
                universal(box.maxX, box.minY, box.minZ, color)
                universal(box.maxX, box.maxY, box.maxZ, color)
                universal(box.maxX, box.maxY, box.minZ, color)
            }
        }
        GLHelper.cull = true
    }

    fun drawBoxOutline(box: AABB, lineWidth: Float, color: ColorRGBA) {
        GLHelper.cull = false
        GLHelper.depth = false
        GLHelper.blend = true
        GLHelper.lineSmooth = true
        glLineWidth(lineWidth)
        val x1 = box.minX
        val x2 = box.maxX
        val y1 = box.minY
        val y2 = box.maxY
        val z1 = box.minZ
        val z2 = box.maxZ
        RS.matrixLayer.scope {
            GL11.GL_LINES.draw(PMVBObjects.VertexMode.Universal) {
                universal(x1, y1, z1, color)
                universal(x2, y1, z1, color)

                universal(x2, y1, z1, color)
                universal(x2, y1, z2, color)

                universal(x2, y1, z2, color)
                universal(x1, y1, z2, color)

                universal(x1, y1, z2, color)
                universal(x1, y1, z1, color)

                universal(x1, y1, z1, color)
                universal(x1, y2, z1, color)

                universal(x2, y1, z1, color)
                universal(x2, y2, z1, color)

                universal(x2, y1, z2, color)
                universal(x2, y2, z2, color)

                universal(x1, y1, z2, color)
                universal(x1, y2, z2, color)

                universal(x1, y2, z1, color)
                universal(x2, y2, z1, color)

                universal(x2, y2, z1, color)
                universal(x2, y2, z2, color)

                universal(x2, y2, z2, color)
                universal(x1, y2, z2, color)

                universal(x1, y2, z2, color)
                universal(x1, y2, z1, color)
            }
        }
        GLHelper.cull = true
    }
}
