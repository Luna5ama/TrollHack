package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.RenderSystem
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.Skia2DEvent
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.graphics.buffer.Render3DUtils
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.skia.SkiaDrawScope
import dev.luna5ama.trollhack.utils.math.RotationUtils
import dev.luna5ama.trollhack.utils.math.vectors.HAlign.*
import dev.luna5ama.trollhack.utils.math.vectors.VAlign
import dev.luna5ama.trollhack.utils.math.vectors.VAlign.BOTTOM
import dev.luna5ama.trollhack.utils.math.vectors.VAlign.TOP
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.world.EntityUtils
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

object Tracers : Module("Tracers", category = Category.VISUAL) {
    private val players by setting("Players", true)
    private val items by setting("Items", true)
    private val mobs by setting("Mobs", false)
    private val passive by setting("Passive", false)
    private val neutral by setting("Neutral", false)
    private val hostile by setting("Hostile", false)
    private val hAlign by setting("HAlign", CENTER)
    private val vAlign by setting("VAlign", VAlign.CENTER)
    private val thickness by setting("Thickness", 1.0f, 0.0f..5.0f, 0.1f)
    private val color by setting("Color", ColorRGBA.WHITE)
    private val friendColor by setting("Friend Color", ColorRGBA.BLUE)

    init {
        nonNullHandler<Skia2DEvent> { event ->
            EntityManager.entity.forEach { entity ->
                val interpolatedPos = EntityUtils.getInterpolatedPos(entity, event.ticksDelta)
                when {
                    entity is Player && players -> {
                        if (entity != player) {
                            drawLineTo(event.draw,
                                interpolatedPos,
                                if (FriendManager.isFriend(player.displayName?.string ?: player.name.string))
                                    friendColor else color
                            )
                        }
                    }
                    entity is ItemEntity && items -> {
                        drawLineTo(event.draw, interpolatedPos, color)
                    }
                    entity is LivingEntity -> {
                        if (EntityUtils.mobTypeSettings(entity, mobs, passive, neutral, hostile)) {
                            drawLineTo(event.draw, interpolatedPos, color)
                        }
                    }
                }
            }
        }
    }

    context(ctx: NonNullContext)
    private fun drawLineTo(draw: SkiaDrawScope, pos: Vec3, color: ColorRGBA): Unit = ctx.run {
        val camera = mc.entityRenderDispatcher.camera ?: return
        val cameraPos = camera.position()
        val x = when (hAlign) {
            LEFT -> 0.0
            CENTER -> RenderSystem.scaledWidth / 2
            RIGHT -> RenderSystem.scaledWidth
        }
        val y = when (vAlign) {
            TOP -> 0.0
            VAlign.CENTER -> RenderSystem.scaledHeight / 2
            BOTTOM -> RenderSystem.scaledHeight
        }
        if (RotationUtils.getRotationDiff(
                Vec2f(camera.yRot(), camera.xRot()),
                RotationUtils.getRotationTo(cameraPos, pos)) < mc.options.fov().get()) {
            val screenPos = Render3DUtils.worldToScreen(pos)
            draw.line(
                screenPos.x.toFloat(), screenPos.y.toFloat(),
                x.toFloat(), y.toFloat(), thickness, color
            )
        }
    }

    private enum class Position : Displayable {
        START, CENTER, END
    }
}
