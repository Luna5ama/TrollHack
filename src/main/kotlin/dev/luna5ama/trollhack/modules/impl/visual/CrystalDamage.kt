package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.Skia2DEvent
import dev.luna5ama.trollhack.graphics.blaze3d.WorldProjection
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.utils.math.vectors.VectorUtils.minus
import dev.luna5ama.trollhack.utils.world.explosion.advanced.DamageCalculation
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.phys.Vec3

object CrystalDamage : Module("Crystal Damage", description = "不能代表真实伤害", category = Category.RENDER) {
    init {
        nonNullHandler<Skia2DEvent> { event ->
            EntityManager.entity.filterIsInstance<EndCrystal>()
                .forEach {
                    val damage = DamageCalculation(this, player, player.position())
                        .calcDamage(it.x, it.y, it.z, false, BlockPos.MutableBlockPos())
                    val textPos = WorldProjection.worldToScreen(
                        it.position() - Vec3(.0, 0.5, .0),
                        event.framebufferWidth,
                        event.framebufferHeight,
                        event.width,
                        event.height,
                    ) ?: return@forEach
                    event.draw.centeredText(
                        damage.toString(),
                        textPos.x,
                        textPos.y,
                        color = ColorRGBA.WHITE,
                        shadow = true
                    )
                }
        }
    }
}
