package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.event.impl.render.Render3DEvent
import dev.luna5ama.trollhack.event.impl.world.PlaceBlockEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.ChatUtils
import dev.luna5ama.trollhack.graphics.animations.BlockEasingRender
import dev.luna5ama.trollhack.graphics.blaze3d.Render3DScheduler
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.utils.timing.TickTimer
import dev.luna5ama.trollhack.utils.timing.TimeUnit
import net.minecraft.core.BlockPos

object PlaceRender : Module("Place Render", category = Category.RENDER) {
    private val delay by setting("Show Time S", 1.0f, 0.1f..2.0f, 0.1f)
    private val color by setting("Color", ColorRGBA.WHITE.alpha(128))
    private val lineColor by setting("Line Color", ColorRGBA.WHITE)
    private val lineWidth by setting("Line Width", 1.0f, 0.1f..4.0f)
    private val debug by setting("Debug", false)
    private val animation = BlockEasingRender(BlockPos.ZERO, 500f, 500f)
    private val timer = TickTimer()

    init {
        nonNullHandler<TickEvent.Post> {
            if (timer.tickAndReset(delay.toInt(), TimeUnit.SECONDS)) {
                animation.end()
            }
        }

        nonNullHandler<PlaceBlockEvent> {
            val renderPos = it.getBlockPos()
            timer.reset()
            animation.begin()
            animation.updatePos(renderPos)
            if (debug){
                ChatUtils.sendRawMessage(renderPos.toString())
            }
        }

        nonNullHandler<Render3DEvent> {
            val (box, _) = animation.updateVec3Box()
            Render3DScheduler.addFilledBox(box, color, through = true)
            Render3DScheduler.addOutlineBox(box, lineColor, lineWidth, through = true)
        }
    }
}
