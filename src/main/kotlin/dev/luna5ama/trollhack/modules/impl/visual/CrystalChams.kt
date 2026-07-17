package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.Render3DEvent
import dev.luna5ama.trollhack.graphics.blaze3d.Render3DScheduler
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import net.minecraft.world.entity.boss.enderdragon.EndCrystal

object CrystalChams : Module("Crystal Chams", category = Category.RENDER) {
    private val sideColor by setting("Side Color", ColorRGBA(160, 120, 255, 70))
    private val lineColor by setting("Line Color", ColorRGBA(230, 220, 255, 220))
    private val scale by setting("Scale", 1.0f, 0.1f..3.0f, 0.05f)
    private val lineWidth by setting("Line Width", 2.0f, 0.5f..5.0f, 0.5f)

    init {
        nonNullHandler<Render3DEvent> {
            for (entity in world.entitiesForRendering()) {
                if (entity !is EndCrystal) continue
                val box = entity.boundingBox.inflate((scale - 1.0f).toDouble() * 0.5)
                Render3DScheduler.addFilledBox(box, sideColor, through = true)
            }
        }
    }

    @JvmStatic
    fun outlineArgb(entity: net.minecraft.world.entity.Entity): Int {
        if (!isEnabled || entity !is EndCrystal || lineColor.a <= 0) return 0
        return (255 shl 24) or (lineColor.r shl 16) or (lineColor.g shl 8) or lineColor.b
    }
}
