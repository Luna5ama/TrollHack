package dev.luna5ama.trollhack.module.modules.combat

import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.events.render.RenderEntityEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.GlStateUtils
import dev.luna5ama.trollhack.graphics.RenderUtils3D
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.color.setGLColor
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.viewEntity
import dev.luna5ama.trollhack.util.accessor.renderPosX
import dev.luna5ama.trollhack.util.accessor.renderPosY
import dev.luna5ama.trollhack.util.accessor.renderPosZ
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.item.EntityEnderCrystal
import org.lwjgl.opengl.GL20.glUseProgram

internal object CrystalChams : Module(
    name = "Crystal Chams",
    description = "Renders chams for End Crystals",
    category = Category.COMBAT
) {
    val scale by setting("Scale", 1.0f, 0.1f..4.0f, 0.1f)
    val spinSpeed by setting("Spin Speed", 1.0f, 0.0f..4.0f, 0.1f)
    val floatSpeed by setting("Float Speed", 1.0f, 0.0f..4.0f, 0.1f)
    val filled by setting("Filled", true)
    val filledDepth by setting("Filled Depth", true, ::filled)
    private val filledColor by setting("Filled Color", ColorRGB(133, 255, 200, 63), true, ::filled)
    val outline by setting("Outline", true)
    val outlineDepth by setting("Outline Depth", false, ::outline)
    private val outlineColor by setting("Outline Color", ColorRGB(133, 255, 200, 200), true, ::outline)
    private val width by setting("Width", 2.0f, 0.25f..4.0f, 0.25f, ::outline)
    private val range by setting("Range", 16.0f, 0.0f..16.0f, 0.5f)

    init {
        safeListener<RenderEntityEvent.All.Pre> {
            if (it.entity is EntityEnderCrystal && viewEntity.distanceSqTo(it.entity) <= range * range) {
                it.cancel()
            }
        }

        safeListener<Render3DEvent> {
            val partialTicks = RenderUtils3D.partialTicks
            val rangeSq = range * range
            val renderer =
                mc.renderManager.getEntityClassRenderObject<EntityEnderCrystal>(EntityEnderCrystal::class.java)
                    ?: return@safeListener

            GlStateUtils.alpha(true)
            GlStateManager.glLineWidth(width)
            glUseProgram(0)

            for (crystal in EntityManager.entity) {
                if (crystal !is EntityEnderCrystal) continue
                if (viewEntity.distanceSqTo(crystal) > rangeSq) continue

                renderer.doRender(
                    crystal,
                    crystal.posX - mc.renderManager.renderPosX,
                    crystal.posY - mc.renderManager.renderPosY,
                    crystal.posZ - mc.renderManager.renderPosZ,
                    0.0f,
                    partialTicks
                )
            }

            GlStateUtils.depth(false)
            GlStateUtils.alpha(false)
        }
    }

    @JvmStatic
    fun setFilledColor() {
        filledColor.setGLColor()
    }

    @JvmStatic
    fun setOutlineColor() {
        outlineColor.setGLColor()
    }
}