package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.RenderSystem
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.event.impl.render.CoreRender3DEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.graphics.animations.BlockEasingRender
import dev.luna5ama.trollhack.graphics.buffer.Render3DUtils
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.matrix.scope
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

object BlockHighlight : Module("Block Highlight", category = Category.RENDER) {
    private val color by setting("Color", ColorRGBA.WHITE.alpha(128))
    private val lineColor by setting("Line Color", ColorRGBA.WHITE)
    private val lineWidth by setting("Line Width", 1.0f, 0.1f..4.0f)
    private val animation = BlockEasingRender(BlockPos.ZERO, 500f, 500f)

    init {
        nonNullHandler<TickEvent.Pre> {
            val target = mc.hitResult
            if (target == null || target.type != HitResult.Type.BLOCK) {
                animation.end()
            } else {
                val blockPos = (target as BlockHitResult).blockPos
                animation.begin()
                animation.updatePos(blockPos)
            }

        }

        nonNullHandler<CoreRender3DEvent> {
            val (box, _) = animation.updateVec3Box()
            if (box.getSize() <= 0.001) return@nonNullHandler
            RenderSystem.matrixLayer.scope {
                Render3DUtils.drawBox(box, color)
                Render3DUtils.drawBoxOutline(box, lineWidth, lineColor)
            }
        }
    }
}