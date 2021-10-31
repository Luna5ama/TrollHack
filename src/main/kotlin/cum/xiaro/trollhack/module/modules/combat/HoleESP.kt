package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.render.Render3DEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.HoleManager
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.EntityUtils.eyePosition
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.atValue
import cum.xiaro.trollhack.util.combat.HoleInfo
import cum.xiaro.trollhack.util.combat.HoleType
import cum.xiaro.trollhack.util.graphics.ESPRenderer
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.graphics.RenderUtils3D
import cum.xiaro.trollhack.util.graphics.mask.EnumFacingMask
import cum.xiaro.trollhack.util.threads.defaultScope
import kotlinx.coroutines.launch
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11.*

internal object HoleESP : Module(
    name = "HoleESP",
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
    private val flatOutline by setting("Flat Outline", true)
    private val width by setting("Width", 2.0f, 1.0f..8.0f, 0.1f, outline0.atTrue())
    private val range by setting("Range", 16, 4..32, 1)

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

        GlStateUtils.cull(false)
        renderGlowESPFilled()
        RenderUtils3D.draw(GL_QUADS)
        GlStateUtils.cull(true)

        renderGlowESPOutline()
        RenderUtils3D.draw(GL_LINES)

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
        defaultScope.launch {
            renderer.aFilled = if (filled) aFilled else 0
            renderer.aOutline = if (outline) aOutline else 0
            renderer.thickness = width

            val eyePos = player.eyePosition
            val rangeSq = range * range
            val cached = ArrayList<ESPRenderer.Info>()
            val side = if (renderMode != RenderMode.FLAT) EnumFacingMask.ALL
            else EnumFacingMask.DOWN

            for (holeInfo in HoleManager.holeInfos) {
                if (eyePos.squareDistanceTo(holeInfo.center) > rangeSq) continue
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