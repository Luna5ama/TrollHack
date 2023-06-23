package dev.luna5ama.trollhack.module.modules.render

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.render.Render2DEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.ProjectionUtils
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.accessor.damagedBlocks
import dev.luna5ama.trollhack.util.accessor.entityID
import dev.luna5ama.trollhack.util.math.scale
import dev.luna5ama.trollhack.util.world.getSelectedBox
import net.minecraft.client.renderer.DestroyBlockProgress
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import kotlin.math.max
import kotlin.math.pow

internal object BreakESP : Module(
    name = "Break ESP",
    description = "Highlights blocks being broken nearby",
    category = Category.RENDER
) {
    private val self by setting("Self", true)
    private val other by setting("Other", true)
    private val color by setting("Color", ColorRGB(200, 205, 210), false)
    private val aFilled by setting("Filled Alpha", 31, 0..255, 1)
    private val aOutline by setting("Outline Alpha", 255, 0..255, 1)

    private val renderer = ESPRenderer()

    init {
        safeListener<Render2DEvent.Absolute> {
            for (progress in mc.renderGlobal.damagedBlocks.values) {
                if (isInvalidBreaker(progress)) continue

                val text = "${(progress.partialBlockDamage + 1) * 10} %"
                val center = getBoundingBox(progress.position).center
                val screenPos = ProjectionUtils.toAbsoluteScreenPos(center)
                val distFactor = max(ProjectionUtils.distToCamera(center) - 1.0, 0.0)
                val scale = max(6.0f / 2.0.pow(distFactor).toFloat(), 1.0f)

                val x = MainFontRenderer.getWidth(text, scale) * -0.5f
                val y = MainFontRenderer.getHeight(scale) * -0.5f
                MainFontRenderer.drawString(text, screenPos.x.toFloat() + x, screenPos.y.toFloat() + y, scale = scale)
            }
        }

        safeListener<Render3DEvent> {
            renderer.aOutline = aOutline
            renderer.aFilled = aFilled

            for (progress in mc.renderGlobal.damagedBlocks.values) {
                if (isInvalidBreaker(progress)) continue
                val box = getBoundingBox(progress.position, progress.partialBlockDamage + 1)
                renderer.add(box, color)
            }

            renderer.render(true)
        }
    }

    private fun SafeClientEvent.getBoundingBox(pos: BlockPos, progress: Int): AxisAlignedBB {
        return getBoundingBox(pos).scale(progress / 10.0)
    }


    private fun SafeClientEvent.getBoundingBox(pos: BlockPos): AxisAlignedBB {
        return world.getSelectedBox(pos)
    }

    private fun isInvalidBreaker(progress: DestroyBlockProgress): Boolean {
        val breakerID = progress.entityID

        return if (breakerID == mc.player?.entityId) {
            !self
        } else {
            !other
        }
    }
}