package cum.xiaro.trollhack.gui.hudgui.elements.world

import cum.xiaro.trollhack.util.extension.fastFloor
import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.gui.hudgui.HudElement
import cum.xiaro.trollhack.manager.managers.ChunkManager
import cum.xiaro.trollhack.manager.managers.FriendManager
import cum.xiaro.trollhack.module.modules.client.GuiSetting
import cum.xiaro.trollhack.util.*
import cum.xiaro.trollhack.util.EntityUtils.isNeutral
import cum.xiaro.trollhack.util.EntityUtils.isPassive
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.graphics.RenderUtils2D
import cum.xiaro.trollhack.util.graphics.RenderUtils2D.drawCircleFilled
import cum.xiaro.trollhack.util.graphics.RenderUtils2D.drawCircleOutline
import cum.xiaro.trollhack.util.graphics.RenderUtils3D
import cum.xiaro.trollhack.util.graphics.font.renderer.MainFontRenderer
import cum.xiaro.trollhack.util.threads.runSafe
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11.*
import kotlin.math.abs

internal object Radar : HudElement(
    name = "Radar",
    category = Category.WORLD,
    description = "Shows entities and new chunks"
) {
    private val page = setting("Page", Page.ENTITY)

    private val entity0 = setting("Entity", true, page.atValue(Page.ENTITY))
    private val entity by entity0
    private val players by setting("Players", true, page.atValue(Page.ENTITY) and entity0.atTrue())
    private val passive by setting("Passive Mobs", false, page.atValue(Page.ENTITY) and entity0.atTrue())
    private val neutral by setting("Neutral Mobs", true, page.atValue(Page.ENTITY) and entity0.atTrue())
    private val hostile by setting("Hostile Mobs", true, page.atValue(Page.ENTITY) and entity0.atTrue())
    private val invisible by setting("Invisible", true, page.atValue(Page.ENTITY) and entity0.atTrue())
    private val pointSize by setting("Point Size", 4.0f, 1.0f..16.0f, 0.5f, page.atValue(Page.ENTITY) and entity0.atTrue())

    private val chunk0 = setting("Chunk", false, page.atValue(Page.CHUNK))
    private val chunk by chunk0
    private val newChunk0 = setting("New Chunk", true, page.atValue(Page.CHUNK) and chunk0.atTrue())
    private val newChunk by newChunk0
    private val newChunkColor by setting("New Chunk Color", ColorRGB(255, 31, 31, 63), true, page.atValue(Page.CHUNK) and chunk0.atTrue() and newChunk0.atTrue())
    private val unloadedChunk0 = setting("Unloaded Chunk", true, page.atValue(Page.CHUNK) and chunk0.atTrue())
    private val unloadedChunk by unloadedChunk0
    private val unloadedChunkColor by setting("Unloaded Chunk Color", ColorRGB(127, 127, 127, 31), true, page.atValue(Page.CHUNK) and chunk0.atTrue() and unloadedChunk0.atTrue())
    private val chunkGrid0 = setting("Chunk Grid", true, page.atValue(Page.CHUNK) and chunk0.atTrue())
    private val chunkGrid by chunkGrid0
    private val gridColor by setting("Grid Color", ColorRGB(127, 127, 127, 63), true, page.atValue(Page.CHUNK) and chunk0.atTrue() and chunkGrid0.atTrue())

    private val radarRange by setting("Radar Range", 64, 8..256, 1)

    private enum class Page {
        ENTITY, CHUNK
    }

    override val hudWidth: Float = 100.0f
    override val hudHeight: Float = 100.0f

    private const val halfSize = 50.0
    private const val radius = 48.0f

    override fun renderHud() {
        super.renderHud()

        runSafe {
            drawBorder()

            if (chunk) drawChunk()
            if (entity) drawEntity()

            drawLabels()
        }
    }

    private fun SafeClientEvent.drawBorder() {
        glTranslated(halfSize, halfSize, 0.0)

        drawCircleFilled(radius = radius, color = GuiSetting.backGround)
        drawCircleOutline(radius = radius, lineWidth = 1.5f, color = primaryColor)

        glRotatef(-player.rotationYaw + 180.0f, 0.0f, 0.0f, 1.0f)
    }

    private fun SafeClientEvent.drawEntity() {
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer

        val partialTicks = RenderUtils3D.partialTicks
        val playerPos = EntityUtils.getInterpolatedPos(player, partialTicks)
        val posMultiplier = radius / radarRange

        prepareGLPoint()
        buffer.begin(GL_POINTS, DefaultVertexFormats.POSITION_COLOR)

        // Player marker
        buffer.pos(0.0, 0.0, 0.0).color(primaryColor.r, primaryColor.g, primaryColor.b, primaryColor.a).endVertex()

        for (entity in getEntityList()) {
            val diff = EntityUtils.getInterpolatedPos(entity, partialTicks).subtract(playerPos)
            if (abs(diff.y) > 24.0) continue

            val color = getColor(entity)
            buffer.pos(diff.x * posMultiplier, diff.z * posMultiplier, 0.0).color(color.r, color.g, color.b, color.a).endVertex()
        }

        GlStateUtils.useProgram(0)
        tessellator.draw()
        releaseGLPoint()
    }

    private fun getEntityList(): List<EntityLivingBase> {
        val playerTargets = arrayOf(players, true, true) // Enable friends and sleeping
        val mobTargets = arrayOf(true, passive, neutral, hostile) // Enable mobs
        return EntityUtils.getTargetList(playerTargets, mobTargets, invisible, radarRange.toFloat(), ignoreSelf = true)
    }

    private fun prepareGLPoint() {
        RenderUtils2D.prepareGl()
        glPointSize(pointSize)
        glEnable(GL_POINT_SMOOTH)
        glHint(GL_POINT_SMOOTH_HINT, GL_NICEST)
    }

    private fun releaseGLPoint() {
        RenderUtils2D.releaseGl()
        glPointSize(1.0f)
        glDisable(GL_POINT_SMOOTH)
        glHint(GL_POINT_SMOOTH_HINT, GL_DONT_CARE)
    }

    private fun SafeClientEvent.drawChunk() {
        RenderUtils2D.prepareGl()

        val interpolatedPos = EntityUtils.getInterpolatedPos(player, RenderUtils3D.partialTicks)
        val chunkX = (interpolatedPos.x / 16.0).fastFloor()
        val chunkZ = (interpolatedPos.z / 16.0).fastFloor()

        val posMultiplier = radius / radarRange
        val diffX = (chunkX * 16 - interpolatedPos.x) * posMultiplier
        val diffZ = (chunkZ * 16 - interpolatedPos.z) * posMultiplier

        val chunks = drawChunkGrid(diffX, diffZ)
        drawChunkFilled(chunks, chunkX, chunkZ)

        RenderUtils2D.releaseGl()
    }

    private fun drawChunkGrid(diffX: Double, diffZ: Double): List<Pair<Pair<Int, Int>, Quad<Double, Double, Double, Double>>> {
        val chunkDist = radarRange / 16
        val posMultiplier = radius / radarRange
        val chunkPosMultiplier = posMultiplier * 16.0
        val rangeSq = (radarRange * posMultiplier) * (radarRange * posMultiplier)

        val chunks = ArrayList<Pair<Pair<Int, Int>, Quad<Double, Double, Double, Double>>>()

        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer
        val drawGrid = chunkGrid

        if (drawGrid) buffer.begin(GL_LINES, DefaultVertexFormats.POSITION_COLOR)

        for (chunkX in -chunkDist..chunkDist) {
            for (chunkZ in -chunkDist..chunkDist) {
                val x1 = chunkX * chunkPosMultiplier + diffX
                val y1 = chunkZ * chunkPosMultiplier + diffZ
                val x2 = (chunkX + 1) * chunkPosMultiplier + diffX
                val y2 = (chunkZ + 1) * chunkPosMultiplier + diffZ

                if (calcMaxDistSq(x1, y1, x2, y2) >= rangeSq) continue

                if (drawGrid) {
                    buffer.pos(x1, y1, 0.0).color(gridColor.r, gridColor.g, gridColor.b, gridColor.a).endVertex()
                    buffer.pos(x1, y2, 0.0).color(gridColor.r, gridColor.g, gridColor.b, gridColor.a).endVertex()

                    buffer.pos(x1, y2, 0.0).color(gridColor.r, gridColor.g, gridColor.b, gridColor.a).endVertex()
                    buffer.pos(x2, y2, 0.0).color(gridColor.r, gridColor.g, gridColor.b, gridColor.a).endVertex()

                    buffer.pos(x2, y2, 0.0).color(gridColor.r, gridColor.g, gridColor.b, gridColor.a).endVertex()
                    buffer.pos(x2, y1, 0.0).color(gridColor.r, gridColor.g, gridColor.b, gridColor.a).endVertex()

                    buffer.pos(x2, y1, 0.0).color(gridColor.r, gridColor.g, gridColor.b, gridColor.a).endVertex()
                    buffer.pos(x1, y1, 0.0).color(gridColor.r, gridColor.g, gridColor.b, gridColor.a).endVertex()
                }

                chunks.add((chunkX to chunkZ) to Quad(x1, y1, x2, y2))
            }
        }

        if (drawGrid) {
            GlStateUtils.useProgram(0)
            tessellator.draw()
        }

        return chunks
    }

    private fun calcMaxDistSq(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val maxX = if (x1 + x2 >= 0.0) x2 else x1
        val maxY = if (y1 + y2 >= 0.0) y2 else y1
        return maxX * maxX + maxY * maxY
    }

    private fun SafeClientEvent.drawChunkFilled(chunks: List<Pair<Pair<Int, Int>, Quad<Double, Double, Double, Double>>>, chunkX: Int, chunkZ: Int) {
        if (unloadedChunk || newChunk) {
            val tessellator = Tessellator.getInstance()
            val buffer = tessellator.buffer

            buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_COLOR)

            for ((chunkPos, quad) in chunks) {
                val chunk = world.getChunk(chunkX + chunkPos.first, chunkZ + chunkPos.second)

                if (unloadedChunk && (chunk.isEmpty || !chunk.isLoaded)) {
                    buffer.drawChunkQuad(quad, unloadedChunkColor)
                }

                if (newChunk && ChunkManager.isNewChunk(chunk)) {
                    buffer.drawChunkQuad(quad, newChunkColor)
                }
            }

            GlStateUtils.useProgram(0)
            tessellator.draw()
        }
    }

    private fun BufferBuilder.drawChunkQuad(quad: Quad<Double, Double, Double, Double>, color: ColorRGB) {
        pos(quad.first, quad.second, 0.0).color(color.r, color.g, color.b, color.a).endVertex()
        pos(quad.first, quad.fourth, 0.0).color(color.r, color.g, color.b, color.a).endVertex()
        pos(quad.third, quad.fourth, 0.0).color(color.r, color.g, color.b, color.a).endVertex()
        pos(quad.third, quad.second, 0.0).color(color.r, color.g, color.b, color.a).endVertex()
    }

    private fun drawLabels() {
        drawLabel("-Z")
        drawLabel("+X")
        drawLabel("+Z")
        drawLabel("-X")
    }

    private fun drawLabel(name: String) {
        MainFontRenderer.drawString(name, MainFontRenderer.getWidth(name, 0.8f) * -0.5f, -radius.toFloat(), secondaryColor, 0.8f)
        glRotatef(90.0f, 0.0f, 0.0f, 1.0f)
    }

    private fun getColor(entity: EntityLivingBase): ColorRGB {
        return if (entity.isPassive || FriendManager.isFriend(entity.name)) {
            // Green
            ColorRGB(32, 224, 32, 224)
        } else if (entity.isNeutral) {
            // Yellow
            ColorRGB(255, 240, 32)
        } else {
            // Red
            ColorRGB(255, 32, 32)
        }
    }

}