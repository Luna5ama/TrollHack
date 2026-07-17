package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.player.PlayerPopEvent
import dev.luna5ama.trollhack.event.impl.render.Render3DEvent
import dev.luna5ama.trollhack.graphics.blaze3d.Render3DScheduler
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import net.minecraft.world.phys.AABB
import net.minecraft.world.entity.player.Player
import java.util.UUID
import kotlin.math.max

object PopChams : Module("Pop Chams", category = Category.RENDER) {
    private val ignoreSelf by setting("Ignore Self", true)
    private val renderTime by setting("Render Time", 1.0f, 0.1f..6.0f, 0.1f)
    private val yModifier by setting("Y Modifier", 0.75f, -4.0f..4.0f, 0.05f)
    private val scaleModifier by setting("Scale Modifier", -0.25f, -4.0f..4.0f, 0.05f)
    private val fadeOut by setting("Fade Out", true)
    private val sideColor by setting("Side Color", ColorRGBA(255, 255, 255, 25))
    private val lineColor by setting("Line Color", ColorRGBA(255, 255, 255, 127))
    private val ghosts = ArrayList<Ghost>()

    init {
        onDisabled { ghosts.clear() }
        nonNullHandler<PlayerPopEvent> {
            if (ignoreSelf && it.player == player) return@nonNullHandler
            ghosts += Ghost(it.player.uuid, it.player.boundingBox, System.nanoTime())
        }
        nonNullHandler<Render3DEvent> {
            val now = System.nanoTime()
            ghosts.removeIf { ghost ->
                val elapsed = (now - ghost.started) / 1_000_000_000.0f
                if (elapsed >= renderTime) return@removeIf true
                val progress = (elapsed / renderTime).coerceIn(0f, 1f)
                val scale = 1.0 + scaleModifier * elapsed
                if (scale <= 0.0) return@removeIf true
                val center = ghost.box.center
                val box = ghost.box.move(0.0, (yModifier * renderTime * progress).toDouble(), 0.0)
                    .inflate((scale - 1.0) * ghost.box.xsize * 0.5, (scale - 1.0) * ghost.box.ysize * 0.5, (scale - 1.0) * ghost.box.zsize * 0.5)
                val fade = if (fadeOut) max(0f, 1f - progress) else 1f
                Render3DScheduler.addFilledBox(box, sideColor.alpha((sideColor.a * fade).toInt()), through = true)
                Render3DScheduler.addOutlineBox(box, lineColor.alpha((lineColor.a * fade).toInt()), 2f, through = true)
                false
            }
        }
    }

    @JvmStatic
    fun outlineArgb(entity: net.minecraft.world.entity.Entity): Int {
        if (!isEnabled || entity !is Player || lineColor.a <= 0) return 0
        val now = System.nanoTime()
        val ghost = ghosts.lastOrNull { it.playerId == entity.uuid } ?: return 0
        val elapsed = (now - ghost.started) / 1_000_000_000.0f
        if (elapsed >= renderTime) return 0
        val alpha = if (fadeOut) (lineColor.a * (1f - elapsed / renderTime)).toInt() else lineColor.a
        if (alpha <= 0) return 0
        return (255 shl 24) or (lineColor.r shl 16) or (lineColor.g shl 8) or lineColor.b
    }

    private data class Ghost(val playerId: UUID, val box: AABB, val started: Long)
}
