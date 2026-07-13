package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.Render3DEvent
import dev.luna5ama.trollhack.graphics.blaze3d.Render3DScheduler
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.math.toRadian
import dev.luna5ama.trollhack.utils.world.EntityUtils
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object BlueArchiveHalo : Module("Blue Archive Halo", category = Category.RENDER, hidden = false) {
    private const val CIRCLE_SEGMENTS = 720

    val floatingSpeed by setting("Floating Speed", 0.5f)
    val color by setting("Color" ,ColorRGBA(0x78d1c7))
    private val circle1 = createCircle(0.38)
    private val circle2 = createCircle(0.2)

    init {
        nonNullHandler<Render3DEvent> { event ->
            val interpolatedPos = EntityUtils.getInterpolatedPos(player, event.partialTicks)
            addCircle(interpolatedPos.add(0.0, 2.0, 0.0), circle1)

            val speedFactor = 1 / floatingSpeed
            val floatingDelta = 0.05f * sin(
                ((System.currentTimeMillis() % (360 * speedFactor).toInt()) / speedFactor).toRadian()
            )
            addCircle(interpolatedPos.add(0.0, 1.93 + floatingDelta, 0.0), circle2)

            val yaw = (-player.yRot).toDouble().toRadian()
            val cosYaw = cos(yaw)
            val sinYaw = sin(yaw)
            addSpoke(interpolatedPos, 1.0, 0.0, cosYaw, sinYaw)
            addSpoke(interpolatedPos, -1.0, 0.0, cosYaw, sinYaw)
            addSpoke(interpolatedPos, 0.0, 1.0, cosYaw, sinYaw)
            addSpoke(interpolatedPos, 0.0, -1.0, cosYaw, sinYaw)
        }
    }

    private fun addCircle(center: Vec3, offsets: List<Vec3>) {
        Render3DScheduler.addLineStrip(offsets, center, color, thickness = 2.0f, through = false)
    }

    private fun addSpoke(center: Vec3, x: Double, z: Double, cosYaw: Double, sinYaw: Double) {
        fun rotate(radius: Double): Vec3 {
            val localX = x * radius
            val localZ = z * radius
            return center.add(
                localX * cosYaw + localZ * sinYaw,
                2.01,
                -localX * sinYaw + localZ * cosYaw
            )
        }

        Render3DScheduler.addLine(
            rotate(0.36),
            rotate(0.43),
            color,
            thickness = 3.5f,
            through = false
        )
    }

    private fun createCircle(radius: Double): List<Vec3> {
        return List(CIRCLE_SEGMENTS + 1) { index ->
            val angle = index * 2.0 * PI / CIRCLE_SEGMENTS
            Vec3(sin(angle) * radius, 0.0, cos(angle) * radius)
        }
    }
}
