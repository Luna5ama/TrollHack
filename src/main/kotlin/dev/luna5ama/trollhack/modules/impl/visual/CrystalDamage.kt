package dev.luna5ama.trollhack.modules.impl.visual

import com.mojang.blaze3d.opengl.GlStateManager
import dev.luna5ama.trollhack.RenderSystem
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.CoreRender2DEvent
import dev.luna5ama.trollhack.manager.managers.UnicodeFontManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.graphics.buffer.Render3DUtils
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.matrix.scope
import dev.luna5ama.trollhack.graphics.matrix.translatef
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.utils.math.vectors.VectorUtils.minus
import dev.luna5ama.trollhack.utils.world.explosion.advanced.DamageCalculation
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.phys.Vec3

object CrystalDamage : Module("Crystal Damage", description = "不能代表真实伤害", category = Category.VISUAL) {
    init {
        nonNullHandler<CoreRender2DEvent> { event ->
            EntityManager.entity.filterIsInstance<EndCrystal>()
                .forEach {
                    val damage = DamageCalculation(this, player, player.position())
                        .calcDamage(it.x, it.y, it.z, false, BlockPos.MutableBlockPos())
                    RenderSystem.matrixLayer.scope {
                        val textPos = Render3DUtils.worldToScreen(it.position() - Vec3(.0, 0.5, .0))
                        translatef(textPos.x.toFloat(), textPos.y.toFloat(), 0.0f)
                        val font = UnicodeFontManager.CURRENT_FONT
                        val damageText = damage.toString()
                        translatef(-(font.getWidth(damageText) / 2.0f), 0.0f, 0.0f)
                        GlStateManager._disableDepthTest()
                        font.drawStringWithShadow(damageText, 1f, 1f, ColorRGBA.WHITE.awt)
                        GlStateManager._enableDepthTest()
                    }
                }
        }
    }
}