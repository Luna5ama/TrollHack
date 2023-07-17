package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.sq
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.GlStateUtils
import dev.luna5ama.trollhack.graphics.RenderUtils3D
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.mask.EnumFacingMask
import dev.luna5ama.trollhack.manager.managers.HoleManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.atValue
import dev.luna5ama.trollhack.util.combat.HoleInfo
import dev.luna5ama.trollhack.util.combat.HoleType
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.threads.BackgroundScope
import kotlinx.coroutines.launch
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11.*

internal object HoleESP : Module(
    name = "Hole ESP",
    category = Category.COMBAT,
    description = "Show safe holes for crystal pvp"
) {
    private val obbyHole0 = setting("Obby Hole", true)
    private val obbyHole by obbyHole0
    private val twoBlocksHole0 = setting("2 Blocks Hole", true)
    private val twoBlocksHole by twoBlocksHole0
    private val fourBlocksHole0 = setting("4 Blocks Hole", true)
    private val fourBlocksHole by fourBlocksHole0
    private val trappedHole0 = setting("Trapped Hole", true)
    private val trappedHole by trappedHole0
    private val bedrockColor by setting("Bedrock Color", ColorRGB(31, 255, 31), false)
    private val obbyColor by setting("Obby Color", ColorRGB(255, 255, 31), false, obbyHole0.atTrue())
    private val twoBlocksColor by setting("2 Blocks Color", ColorRGB(255, 127, 31), false, twoBlocksHole0.atTrue())
    private val fourBlocksColor by setting("4 Blocks Color", ColorRGB(255, 127, 31), false, fourBlocksHole0.atTrue())
    private val trappedColor by setting("Trapped Color", ColorRGB(255, 31, 31), false, trappedHole0.atTrue())
    private val renderMode0 = setting("Render Mode", RenderMode.GLOW)
    private val renderMode by renderMode0
    private val filled0 = setting("Filled", true)
    private val filled by filled0
    private val outline0 = setting("Outline", true)
    private val outline by outline0
    private val aFilled by setting("Filled Alpha", 63, 0..255, 1, filled0.atTrue())
    private val aOutline by setting("Outline Alpha", 255, 0..255, 1, outline0.atTrue())
    private val glowHeight by setting("Glow Height", 1.0f, 0.25f..4.0f, 0.25f, renderMode0.atValue(RenderMode.GLOW))
    private val flatOutline by setting("Flat Outline", true, renderMode0.atValue(RenderMode.GLOW))
    private val width by setting("Width", 2.0f, 1.0f..8.0f, 0.1f, outline0.atTrue())
    private val range by setting("Range", 16, 4..32, 1)
    private val verticleRange by setting("Vertical Range", 8, 4..16, 1)

    @Suppress("UNUSED")
    private enum class RenderMode {
        GLOW, FLAT, BLOCK_HOLE, BLOCK_FLOOR
    }

    private val renderer = ESPRenderer()
    private val timer = TickTimer()

    override fun getHudInfo(): String {
        return renderer.size.toString()
    }

    init {
        safeListener<Render3DEvent> {
            if (timer.tickAndReset(41L)) { // Avoid running this on a tick
                updateRenderer()
            }
            if (renderMode == RenderMode.GLOW) {
                renderGlowESP()
            } else {
                renderer.render(false)
            }
        }
    }

    private fun renderGlowESP() {
        GlStateUtils.depth(false)
        glShadeModel(GL_SMOOTH)
        GlStateManager.glLineWidth(width)

        if (filled) {
            GlStateUtils.cull(false)
            renderGlowESPFilled()
            RenderUtils3D.draw(GL_QUADS)
            GlStateUtils.cull(true)
        }

        if (outline) {
            renderGlowESPOutline()
            RenderUtils3D.draw(GL_LINES)
        }

        GlStateUtils.depth(true)
        GlStateManager.glLineWidth(1.0f)
    }

    private fun renderGlowESPFilled() {
        for ((box, color, _) in renderer.toRender) {
            // -Y
            RenderUtils3D.putVertex(box.maxX, box.minY, box.minZ, color.alpha(aFilled))
            RenderUtils3D.putVertex(box.maxX, box.minY, box.maxZ, color.alpha(aFilled))
            RenderUtils3D.putVertex(box.minX, box.minY, box.maxZ, color.alpha(aFilled))
            RenderUtils3D.putVertex(box.minX, box.minY, box.minZ, color.alpha(aFilled))

            // -X
            RenderUtils3D.putVertex(box.minX, box.minY, box.minZ, color.alpha(aFilled))
            RenderUtils3D.putVertex(box.minX, box.minY, box.maxZ, color.alpha(aFilled))
            RenderUtils3D.putVertex(box.minX, box.minY + glowHeight, box.maxZ, color.alpha(0))
            RenderUtils3D.putVertex(box.minX, box.minY + glowHeight, box.minZ, color.alpha(0))

            // +X
            RenderUtils3D.putVertex(box.maxX, box.minY, box.maxZ, color.alpha(aFilled))
            RenderUtils3D.putVertex(box.maxX, box.minY, box.minZ, color.alpha(aFilled))
            RenderUtils3D.putVertex(box.maxX, box.minY + glowHeight, box.minZ, color.alpha(0))
            RenderUtils3D.putVertex(box.maxX, box.minY + glowHeight, box.maxZ, color.alpha(0))

            // -Z
            RenderUtils3D.putVertex(box.maxX, box.minY, box.minZ, color.alpha(aFilled))
            RenderUtils3D.putVertex(box.minX, box.minY, box.minZ, color.alpha(aFilled))
            RenderUtils3D.putVertex(box.minX, box.minY + glowHeight, box.minZ, color.alpha(0))
            RenderUtils3D.putVertex(box.maxX, box.minY + glowHeight, box.minZ, color.alpha(0))

            // +Z
            RenderUtils3D.putVertex(box.minX, box.minY, box.maxZ, color.alpha(aFilled))
            RenderUtils3D.putVertex(box.maxX, box.minY, box.maxZ, color.alpha(aFilled))
            RenderUtils3D.putVertex(box.maxX, box.minY + glowHeight, box.maxZ, color.alpha(0))
            RenderUtils3D.putVertex(box.minX, box.minY + glowHeight, box.maxZ, color.alpha(0))
        }
    }

    private fun renderGlowESPOutline() {
        for ((box, color, _) in renderer.toRender) {
            RenderUtils3D.putVertex(box.minX, box.minY, box.minZ, color.alpha(aOutline))
            RenderUtils3D.putVertex(box.maxX, box.minY, box.minZ, color.alpha(aOutline))
            RenderUtils3D.putVertex(box.maxX, box.minY, box.minZ, color.alpha(aOutline))
            RenderUtils3D.putVertex(box.maxX, box.minY, box.maxZ, color.alpha(aOutline))
            RenderUtils3D.putVertex(box.maxX, box.minY, box.maxZ, color.alpha(aOutline))
            RenderUtils3D.putVertex(box.minX, box.minY, box.maxZ, color.alpha(aOutline))
            RenderUtils3D.putVertex(box.minX, box.minY, box.maxZ, color.alpha(aOutline))
            RenderUtils3D.putVertex(box.minX, box.minY, box.minZ, color.alpha(aOutline))

            if (!flatOutline) {
                RenderUtils3D.putVertex(box.minX, box.minY, box.minZ, color.alpha(aOutline))
                RenderUtils3D.putVertex(box.minX, box.minY + glowHeight, box.minZ, color.alpha(0))
                RenderUtils3D.putVertex(box.maxX, box.minY, box.minZ, color.alpha(aOutline))
                RenderUtils3D.putVertex(box.maxX, box.minY + glowHeight, box.minZ, color.alpha(0))
                RenderUtils3D.putVertex(box.maxX, box.minY, box.maxZ, color.alpha(aOutline))
                RenderUtils3D.putVertex(box.maxX, box.minY + glowHeight, box.maxZ, color.alpha(0))
                RenderUtils3D.putVertex(box.minX, box.minY, box.maxZ, color.alpha(aOutline))
                RenderUtils3D.putVertex(box.minX, box.minY + glowHeight, box.maxZ, color.alpha(0))
            }
        }
    }

    private fun SafeClientEvent.updateRenderer() {
        BackgroundScope.launch {
            renderer.aFilled = if (filled) aFilled else 0
            renderer.aOutline = if (outline) aOutline else 0
            renderer.thickness = width

            val eyePos = player.eyePosition
            val rangeSq = range * range
            val vRangeSq = verticleRange * verticleRange
            val cached = ArrayList<ESPRenderer.Info>()
            val side = if (renderMode != RenderMode.FLAT) EnumFacingMask.ALL
            else EnumFacingMask.DOWN

            for (holeInfo in HoleManager.holeInfos) {
                if (eyePos.distanceSqTo(holeInfo.center) > rangeSq) continue
                if ((eyePos.y - holeInfo.center.y).sq > vRangeSq) continue
                val color = getColor(holeInfo) ?: continue
                val box = if (renderMode == RenderMode.BLOCK_FLOOR) holeInfo.boundingBox.offset(0.0, -1.0, 0.0)
                else holeInfo.boundingBox

                cached.add(ESPRenderer.Info(box, color, side))
            }

            renderer.replaceAll(cached)
        }
    }

    private fun getColor(holeInfo: HoleInfo) =
        if (holeInfo.isTrapped) {
            trappedColor.takeIf { trappedHole }
        } else {
            when (holeInfo.type) {
                HoleType.NONE -> null
                HoleType.BEDROCK -> bedrockColor
                HoleType.OBBY -> obbyColor.takeIf { obbyHole }
                HoleType.TWO -> twoBlocksColor.takeIf { twoBlocksHole }
                HoleType.FOUR -> fourBlocksColor.takeIf { fourBlocksHole }
            }
        }
}