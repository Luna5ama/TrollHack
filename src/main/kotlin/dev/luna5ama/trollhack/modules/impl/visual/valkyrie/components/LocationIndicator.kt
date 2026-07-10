package dev.luna5ama.trollhack.modules.impl.visual.valkyrie.components

import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.Dimensions
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.HudComponent
import dev.luna5ama.trollhack.utils.NonNullContext

class LocationIndicator(private val dim: Dimensions) : HudComponent() {
    context(ctx: NonNullContext)
    override fun render(partial: Float): Unit = ctx.run {
        val x = dim.wScreen * 0.2f
        val y = dim.hScreen * 0.8f

        val xLoc = player.blockPosition().x
        val zLoc = player.blockPosition().z

        drawFont("$xLoc / $zLoc".format(xLoc, zLoc), x, y)
    }
}
