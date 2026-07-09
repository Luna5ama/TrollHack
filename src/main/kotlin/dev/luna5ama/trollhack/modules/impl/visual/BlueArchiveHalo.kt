package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.RS
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.CoreRender3DEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.graphics.GLHelper
import dev.luna5ama.trollhack.graphics.VertexCache
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects.draw
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.matrix.rotatef
import dev.luna5ama.trollhack.graphics.matrix.scope
import dev.luna5ama.trollhack.graphics.matrix.translatef
import dev.luna5ama.trollhack.utils.extension.yaw
import dev.luna5ama.trollhack.utils.math.toRadian
import dev.luna5ama.trollhack.utils.math.vectors.Vec2d
import dev.luna5ama.trollhack.utils.math.vectors.Vec3f
import dev.luna5ama.trollhack.utils.world.EntityUtils
import org.lwjgl.opengl.GL11.*
import kotlin.math.sin

object BlueArchiveHalo : Module("Blue Archive Halo", category = Category.VISUAL, hidden = false) {
    val floatingSpeed by setting("Floating Speed", 0.5f)
    val color by setting("Color" ,ColorRGBA(0x78d1c7))
    val circle1 = VertexCache.createCircle(Vec2d(0.0, 0.0), 0.38)
    val circle2 = VertexCache.createCircle(Vec2d(0.0, 0.0), 0.2)

    init {
        nonNullHandler<CoreRender3DEvent> {
            RS.matrixLayer.scope {
                val interpolatedPos = EntityUtils.getInterpolatedPos(player, it.ticksDelta)
                GLHelper.lineSmooth = ClientSettings.useGlLineSmooth
                glLineWidth(2f)
                GL_LINE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
                    circle1.forEach {
                        universal(interpolatedPos.x.toFloat() + it.x.toFloat(), interpolatedPos.y.toFloat() + 2.0f,
                            interpolatedPos.z.toFloat() + it.y.toFloat(), color)
                    }
                }
                val speedFactor = 1 / floatingSpeed
                val floatingDelta = 0.05f * sin(((System.currentTimeMillis() % (360 * speedFactor).toInt()) / speedFactor).toRadian())
                GL_LINE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
                    circle2.forEach {
                        universal(interpolatedPos.x.toFloat() + it.x.toFloat(), interpolatedPos.y.toFloat() + 1.93f + floatingDelta,
                            interpolatedPos.z.toFloat() + it.y.toFloat(), color)
                    }
                }
                glLineWidth(3.5f)
                RS.matrixLayer.scope {
                    translatef(interpolatedPos.x.toFloat(), interpolatedPos.y.toFloat(), interpolatedPos.z.toFloat())
                    rotatef(-player.yaw, Vec3f(0f, 1f, 0f))
                    GL_LINE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
                        universal(0.36f, 2.01f, 0f, color)
                        universal(0.43f, 2.01f, 0f, color)
                    }
                    GL_LINE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
                        universal(-0.36f, 2.01f, 0f, color)
                        universal(-0.43f, 2.01f, 0f, color)
                    }
                    GL_LINE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
                        universal(0f, 2.01f, 0.36f, color)
                        universal(0f, 2.01f, 0.43f, color)
                    }
                    GL_LINE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
                        universal(0f, 2.01f, -0.36f, color)
                        universal(0f, 2.01f, -0.43f, color)
                    }
                }
                glLineWidth(1f)
            }
        }
    }
}