package dev.luna5ama.trollhack.gui.hudgui.elements.world

import dev.fastmc.common.floorToInt
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.graphics.RenderUtils2D
import dev.luna5ama.trollhack.graphics.RenderUtils2D.drawCircleFilled
import dev.luna5ama.trollhack.graphics.RenderUtils2D.drawCircleOutline
import dev.luna5ama.trollhack.graphics.RenderUtils3D
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.gui.hudgui.HudElement
import dev.luna5ama.trollhack.manager.managers.ChunkManager
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.EntityUtils
import dev.luna5ama.trollhack.util.EntityUtils.isNeutral
import dev.luna5ama.trollhack.util.EntityUtils.isPassive
import dev.luna5ama.trollhack.util.and
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.atValue
import dev.luna5ama.trollhack.util.threads.runSafe
import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.client.renderer.GlStateManager
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
    private val pointSize by setting(
        "Point Size",
        4.0f,
        1.0f..16.0f,
        0.5f,
        page.atValue(Page.ENTITY) and entity0.atTrue()
    )

    private val chunk0 = setting("Chunk", false, page.atValue(Page.CHUNK))
    private val chunk by chunk0
    private val newChunk0 = setting("New Chunk", true, page.atValue(Page.CHUNK) and chunk0.atTrue())
    private val newChunk by newChunk0
    private val newChunkColor by setting(
        "New Chunk Color",
        ColorRGB(255, 31, 31, 63),
        true,
        page.atValue(Page.CHUNK) and chunk0.atTrue() and newChunk0.atTrue()
    )
    private val unloadedChunk0 = setting("Unloaded Chunk", true, page.atValue(Page.CHUNK) and chunk0.atTrue())
    private val unloadedChunk by unloadedChunk0
    private val unloadedChunkColor by setting(
        "Unloaded Chunk Color",
        ColorRGB(255, 127, 127, 127),
        true,
        page.atValue(Page.CHUNK) and chunk0.atTrue() and unloadedChunk0.atTrue()
    )
    private val chunkGrid0 = setting("Chunk Grid", true, page.atValue(Page.CHUNK) and chunk0.atTrue())
    private val chunkGrid by chunkGrid0
    private val gridColor by setting(
        "Grid Color",
        ColorRGB(127, 127, 127, 63),
        true,
        page.atValue(Page.CHUNK) and chunk0.atTrue() and chunkGrid0.atTrue()
    )

    private val radarRange by setting("Radar Range", 64, 8..512, 1)

    private enum class Page {
        ENTITY, CHUNK
    }

    override val hudWidth = 100.0f
    override val hudHeight = 100.0f

    private const val halfSize = 50.0
    private const val radius = 48.0f

    private val chunkPos = IntArrayList()
    private val chunkVertices = FloatArrayList()

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
        GlStateManager.translate(halfSize, halfSize, 0.0)

        drawCircleFilled(radius = radius, color = GuiSetting.backGround)
        drawCircleOutline(radius = radius, lineWidth = 1.5f, color = GuiSetting.text)

        GlStateManager.rotate(-player.rotationYaw + 180.0f, 0.0f, 0.0f, 1.0f)
    }

    private fun SafeClientEvent.drawEntity() {

        val partialTicks = RenderUtils3D.partialTicks
        val playerPos = EntityUtils.getInterpolatedPos(player, partialTicks)
        val posMultiplier = radius / radarRange

        prepareGLPoint()

        // Player marker
        RenderUtils2D.putVertex(0.0f, 0.0f, GuiSetting.text)

        for (entity in getEntityList()) {
            val diff = EntityUtils.getInterpolatedPos(entity, partialTicks).subtract(playerPos)
            if (abs(diff.y) > 24.0) continue

            val color = getColor(entity)
            RenderUtils2D.putVertex((diff.x * posMultiplier).toFloat(), (diff.z * posMultiplier).toFloat(), color)
        }

        RenderUtils2D.draw(GL_POINTS)
        releaseGLPoint()
    }

    private fun getEntityList(): List<EntityLivingBase> {
        val playerTargets = arrayOf(players, true, true) // Enable friends and sleeping
        val mobTargets = arrayOf(true, passive, neutral, hostile) // Enable mobs
        return EntityUtils.getTargetList(playerTargets, mobTargets, invisible, radarRange.toFloat(), ignoreSelf = true)
    }

    private fun prepareGLPoint() {
        RenderUtils2D.prepareGL()
        glPointSize(pointSize)
        glEnable(GL_POINT_SMOOTH)
        glHint(GL_POINT_SMOOTH_HINT, GL_NICEST)
    }

    private fun releaseGLPoint() {
        RenderUtils2D.releaseGL()
        glPointSize(1.0f)
        glDisable(GL_POINT_SMOOTH)
        glHint(GL_POINT_SMOOTH_HINT, GL_DONT_CARE)
    }

    private fun SafeClientEvent.drawChunk() {
        RenderUtils2D.prepareGL()

        val interpolatedPos = EntityUtils.getInterpolatedPos(player, RenderUtils3D.partialTicks)
        val playerChunkX = (interpolatedPos.x / 16.0).floorToInt()
        val playerChunkZ = (interpolatedPos.z / 16.0).floorToInt()

        val posMultiplier = radius / radarRange
        val diffX = (playerChunkX * 16 - interpolatedPos.x) * posMultiplier
        val diffZ = (playerChunkZ * 16 - interpolatedPos.z) * posMultiplier

        drawChunkGrid(diffX, diffZ)
        drawChunkFilled(playerChunkX, playerChunkZ)

        RenderUtils2D.releaseGL()
    }

    private fun drawChunkGrid(diffX: Double, diffZ: Double) {
        val chunkDist = radarRange / 16
        val posMultiplier = radius / radarRange
        val chunkPosMultiplier = posMultiplier * 16.0
        val rangeSq = (radarRange * posMultiplier) * (radarRange * posMultiplier)

        chunkPos.clear()
        chunkVertices.clear()

        val drawGrid = chunkGrid

        for (chunkX in -chunkDist..chunkDist) {
            for (chunkZ in -chunkDist..chunkDist) {
                val x1 = (chunkX * chunkPosMultiplier + diffX).toFloat()
                val y1 = (chunkZ * chunkPosMultiplier + diffZ).toFloat()
                val x2 = ((chunkX + 1) * chunkPosMultiplier + diffX).toFloat()
                val y2 = ((chunkZ + 1) * chunkPosMultiplier + diffZ).toFloat()

                if (calcMaxDistSq(x1, y1, x2, y2) >= rangeSq) continue

                if (drawGrid) {
                    RenderUtils2D.putVertex(x1, y1, gridColor)
                    RenderUtils2D.putVertex(x1, y2, gridColor)

                    RenderUtils2D.putVertex(x1, y2, gridColor)
                    RenderUtils2D.putVertex(x2, y2, gridColor)

                    RenderUtils2D.putVertex(x2, y2, gridColor)
                    RenderUtils2D.putVertex(x2, y1, gridColor)

                    RenderUtils2D.putVertex(x2, y1, gridColor)
                    RenderUtils2D.putVertex(x1, y1, gridColor)
                }

                chunkPos.add(chunkX)
                chunkPos.add(chunkZ)

                chunkVertices.add(x1)
                chunkVertices.add(y1)
                chunkVertices.add(x2)
                chunkVertices.add(y2)
            }
        }

        if (drawGrid) {
            RenderUtils2D.draw(GL_LINES)
        }
    }

    private fun calcMaxDistSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val maxX = if (x1 + x2 >= 0.0f) x2 else x1
        val maxY = if (y1 + y2 >= 0.0f) y2 else y1
        return maxX * maxX + maxY * maxY
    }

    private fun SafeClientEvent.drawChunkFilled(playerChunkX: Int, playerChunkZ: Int) {
        if (unloadedChunk || newChunk) {
            for (i in 0 until chunkPos.size / 2) {
                val chunkX = chunkPos.getInt(i * 2) + playerChunkX
                val chunkZ = chunkPos.getInt(i * 2 + 1) + playerChunkZ

                val chunk = world.getChunk(chunkX, chunkZ)

                val x1 = chunkVertices.getFloat(i * 4)
                val y1 = chunkVertices.getFloat(i * 4 + 1)
                val x2 = chunkVertices.getFloat(i * 4 + 2)
                val y2 = chunkVertices.getFloat(i * 4 + 3)

                if (unloadedChunk && (chunk.isEmpty || !chunk.isLoaded)) {
                    drawChunkQuad(x1, y1, x2, y2, unloadedChunkColor)
                }

                if (newChunk && ChunkManager.isNewChunk(chunk)) {
                    drawChunkQuad(x1, y1, x2, y2, newChunkColor)
                }
            }

            RenderUtils2D.draw(GL_QUADS)
        }
    }

    private fun drawChunkQuad(x1: Float, y1: Float, x2: Float, y2: Float, color: ColorRGB) {
        RenderUtils2D.putVertex(x1, y1, color)
        RenderUtils2D.putVertex(x1, y2, color)
        RenderUtils2D.putVertex(x2, y2, color)
        RenderUtils2D.putVertex(x2, y1, color)
    }

    private fun drawLabels() {
        drawLabel("-Z")
        drawLabel("+X")
        drawLabel("+Z")
        drawLabel("-X")
    }

    private fun drawLabel(name: String) {
        MainFontRenderer.drawString(
            name,
            MainFontRenderer.getWidth(name, 0.8f) * -0.5f,
            -radius,
            GuiSetting.primary,
            0.8f
        )
         GlStateManager.rotate(90.0f, 0.0f, 0.0f, 1.0f)
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