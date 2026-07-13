package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.Skia2DEvent
import dev.luna5ama.trollhack.graphics.blaze3d.WorldProjection
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
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

object Tracers : Module("Tracers", category = Category.RENDER) {
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
                            drawLineTo(event,
                                interpolatedPos,
                                if (FriendManager.isFriend(entity.displayName?.string ?: entity.name.string))
                                    friendColor else color
                            )
                        }
                    }
                    entity is ItemEntity && items -> {
                        drawLineTo(event, interpolatedPos, color)
                    }
                    entity is LivingEntity -> {
                        if (EntityUtils.mobTypeSettings(entity, mobs, passive, neutral, hostile)) {
                            drawLineTo(event, interpolatedPos, color)
                        }
                    }
                }
            }
        }
    }

    context(ctx: NonNullContext)
    private fun drawLineTo(event: Skia2DEvent, pos: Vec3, color: ColorRGBA): Unit = ctx.run {
        val camera = mc.entityRenderDispatcher.camera ?: return
        val cameraPos = camera.position()
        val x = when (hAlign) {
            LEFT -> 0.0f
            CENTER -> event.width * 0.5f
            RIGHT -> event.width
        }
        val y = when (vAlign) {
            TOP -> 0.0f
            VAlign.CENTER -> event.height * 0.5f
            BOTTOM -> event.height
        }
        if (RotationUtils.getRotationDiff(
                Vec2f(camera.yRot(), camera.xRot()),
                RotationUtils.getRotationTo(cameraPos, pos)) < mc.options.fov().get()) {
            val screenPos = WorldProjection.worldToScreen(
                pos,
                event.framebufferWidth,
                event.framebufferHeight,
                event.width,
                event.height,
            ) ?: return
            event.draw.line(
                screenPos.x, screenPos.y,
                x, y, thickness, color
            )
        }
    }

    private enum class Position : Displayable {
        START, CENTER, END
    }
}
