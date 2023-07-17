package dev.luna5ama.trollhack.module.modules.render

import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.RenderUtils3D
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.mask.EnumFacingMask
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.world.getSelectedBox
import net.minecraft.util.math.RayTraceResult.Type

internal object SelectionHighlight : Module(
    name = "Selection Highlight",
    description = "Highlights object you are looking at",
    category = Category.RENDER
) {
    private val expand by setting("Expand", 0.002f, 0.0f..1.0f, 0.001f)
    private val entity by setting("Entity", true)
    private val hitSideOnly by setting("Hit Side Only", false)
    private val depth by setting("Depth", false)
    private val filled = setting("Filled", true)
    private val outline = setting("Outline", true)
    private val color by setting("Color", ColorRGB(255, 255, 255))
    private val aFilled by setting("Filled Alpha", 63, 0..255, 1, filled.atTrue())
    private val aOutline by setting("Outline Alpha", 200, 0..255, 1, outline.atTrue())
    private val width by setting("Width", 2.0f, 0.25f..5.0f, 0.25f)

    private val renderer = ESPRenderer()

    init {
        safeListener<Render3DEvent> {
            val hitObject = mc.objectMouseOver ?: return@safeListener

            when (hitObject.typeOfHit) {
                Type.ENTITY -> {
                    if (entity) {
                        val viewEntity = mc.renderViewEntity ?: player
                        val eyePos = viewEntity.getPositionEyes(RenderUtils3D.partialTicks)
                        val entity = hitObject.entityHit ?: return@safeListener
                        val lookVec = viewEntity.lookVec
                        val sightEnd = eyePos.add(lookVec.scale(6.0))
                        val hitSide = entity.entityBoundingBox.calculateIntercept(eyePos, sightEnd)?.sideHit
                            ?: return@safeListener
                        val side = if (hitSideOnly) EnumFacingMask.getMaskForSide(hitSide) else EnumFacingMask.ALL
                        renderer.add(hitObject.entityHit.entityBoundingBox.grow(expand.toDouble()), color, side)
                    }
                }
                Type.BLOCK -> {
                    val box = world.getSelectedBox(hitObject.blockPos)
                    val side = if (hitSideOnly) EnumFacingMask.getMaskForSide(hitObject.sideHit) else EnumFacingMask.ALL
                    renderer.add(box.grow(expand.toDouble()), color, side)
                }
                else -> {
                }
            }

            renderer.aFilled = if (filled.value) aFilled else 0
            renderer.aOutline = if (outline.value) aOutline else 0
            renderer.through = !depth
            renderer.thickness = width
            renderer.render(true)
        }
    }
}