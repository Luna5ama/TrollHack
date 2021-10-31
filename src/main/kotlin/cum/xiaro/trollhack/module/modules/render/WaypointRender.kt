package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.event.events.ConnectionEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.WaypointUpdateEvent
import cum.xiaro.trollhack.event.events.render.Render2DEvent
import cum.xiaro.trollhack.event.events.render.Render3DEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.WaypointManager
import cum.xiaro.trollhack.manager.managers.WaypointManager.Waypoint
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.*
import cum.xiaro.trollhack.util.graphics.*
import cum.xiaro.trollhack.util.graphics.font.TextComponent
import cum.xiaro.trollhack.util.math.vector.Vec2f
import cum.xiaro.trollhack.util.math.vector.distanceSqTo
import cum.xiaro.trollhack.util.math.vector.distanceTo
import cum.xiaro.trollhack.util.math.vector.toVec3dCenter
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import org.lwjgl.opengl.GL11.*
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal object WaypointRender : Module(
    name = "WaypointRender",
    description = "Render saved waypoints",
    category = Category.RENDER
) {
    private val page = setting("Page", Page.INFO_BOX)

    /* Page one */
    private val dimension = setting("Dimension", Dimension.CURRENT, page.atValue(Page.INFO_BOX))
    private val showName = setting("Show Name", true, page.atValue(Page.INFO_BOX))
    private val showDate = setting("Show Date", false, page.atValue(Page.INFO_BOX))
    private val showCoords = setting("Show Coords", true, page.atValue(Page.INFO_BOX))
    private val showDist = setting("Show Distance", true, page.atValue(Page.INFO_BOX))
    private val textScale = setting("Text Scale", 1.0f, 0.0f..2.0f, 0.1f, page.atValue(Page.INFO_BOX))
    private val infoBoxRange = setting("Info Box Range", 512, 128..2048, 64, page.atValue(Page.INFO_BOX))

    /* Page two */
    private val renderRange = setting("Render Range", true, page.atValue(Page.ESP))
    private val espRange = setting("Range", 4096, 1024..16384, 1024, page.atValue(Page.ESP) and renderRange.atTrue())
    private val filled = setting("Filled", true, page.atValue(Page.ESP))
    private val outline = setting("Outline", true, page.atValue(Page.ESP))
    private val tracer = setting("Tracer", true, page.atValue(Page.ESP))
    private val r = setting("Red", 31, 0..255, 1, page.atValue(Page.ESP))
    private val g = setting("Green", 200, 0..255, 1, page.atValue(Page.ESP))
    private val b = setting("Blue", 63, 0..255, 1, page.atValue(Page.ESP))
    private val aFilled = setting("Filled Alpha", 63, 0..255, 1, page.atValue(Page.ESP) and filled.atTrue())
    private val aOutline = setting("Outline Alpha", 160, 0..255, 1, page.atValue(Page.ESP) and outline.atTrue())
    private val aTracer = setting("Tracer Alpha", 200, 0..255, 1, page.atValue(Page.ESP) and tracer.atTrue())
    private val thickness = setting("Line Thickness", 2.0f, 0.25f..8.0f, 0.25f, page.atValue(Page.ESP) and (outline.atTrue() or tracer.atTrue()))

    private enum class Dimension {
        CURRENT, ANY
    }

    private enum class Page {
        INFO_BOX, ESP
    }

    // This has to be sorted so the further ones doesn't overlaps the closer ones
    private val waypointMap = TreeMap<Waypoint, TextComponent>(compareByDescending {
        mc.player?.distanceTo(it.pos) ?: it.pos.distanceSqTo(0, -69420, 0)
    })
    private var currentServer: String? = null
    private var timer = TickTimer(TimeUnit.SECONDS)
    private var prevDimension = -2
    private val lockObject = Any()

    override fun getHudInfo(): String {
        return waypointMap.size.toString()
    }

    init {
        listener<Render3DEvent> {
            if (waypointMap.isEmpty()) return@listener
            val color = ColorRGB(r.value, g.value, b.value)
            val renderer = ESPRenderer()
            renderer.aFilled = if (filled.value) aFilled.value else 0
            renderer.aOutline = if (outline.value) aOutline.value else 0
            renderer.aTracer = if (tracer.value) aTracer.value else 0
            renderer.thickness = thickness.value
            GlStateUtils.depth(false)

            for (waypoint in waypointMap.keys) {
                val distance = mc.player.distanceTo(waypoint.pos)
                if (renderRange.value && distance > espRange.value) continue
                renderer.add(AxisAlignedBB(waypoint.pos), color) /* Adds pos to ESPRenderer list */
                drawVerticalLines(waypoint.pos, color) /* Draw lines from y 0 to y 256 */
            }
            GlStateManager.glLineWidth(thickness.value)
            RenderUtils3D.draw(GL_LINES)

            GlStateUtils.depth(true)
            renderer.render(true)
        }
    }

    private fun drawVerticalLines(pos: BlockPos, color: ColorRGB) {
        val box = AxisAlignedBB(pos.x.toDouble(), 0.0, pos.z.toDouble(), pos.x + 1.0, 256.0, pos.z + 1.0)
        RenderUtils3D.drawOutline(box, color.alpha(aOutline.value))
    }

    init {
        listener<Render2DEvent.Absolute> {
            if (waypointMap.isEmpty() || !showCoords.value && !showName.value && !showDate.value && !showDist.value) return@listener
            for ((waypoint, textComponent) in waypointMap) {
                val distance = sqrt(mc.player.getDistanceSqToCenter(waypoint.pos))
                if (distance > infoBoxRange.value) continue
                drawText(waypoint.pos, textComponent, distance.roundToInt())
            }
        }
    }

    private fun drawText(pos: BlockPos, textComponentIn: TextComponent, distance: Int) {
        glPushMatrix()

        val screenPos = ProjectionUtils.toAbsoluteScreenPos(pos.toVec3dCenter())
        glTranslatef(screenPos.x.toFloat(), screenPos.y.toFloat(), 0f)
        glScalef(textScale.value * 2f, textScale.value * 2f, 0f)

        val textComponent = TextComponent(textComponentIn).apply { if (showDist.value) add("$distance m") }
        val stringWidth = textComponent.getWidth()
        val stringHeight = textComponent.getHeight(2)
        val pos1 = Vec2f(stringWidth * -0.5f - 4.0f, stringHeight * -0.5f - 4.0f)
        val pos2 = Vec2f(stringWidth * 0.5f + 4.0f, stringHeight * 0.5f + 4.0f)

        RenderUtils2D.drawRectFilled(pos1, pos2, ColorRGB(32, 32, 32, 172))
        RenderUtils2D.drawRectOutline(pos1, pos2, 2f, ColorRGB(80, 80, 80, 232))
        textComponent.draw(horizontalAlign = HAlign.CENTER, verticalAlign = VAlign.CENTER)

        glPopMatrix()
    }

    init {
        onEnable {
            timer.reset(-10000L) // Update the map immediately and thread safely
        }

        onDisable {
            currentServer = null
        }

        safeListener<TickEvent.Post> {
            if (WaypointManager.genDimension() != prevDimension || timer.tick(10L)) {
                updateList()
            }
        }

        listener<WaypointUpdateEvent> {
            // This could be called from another thread so we have to synchronize the map
            synchronized(lockObject) {
                when (it.type) {
                    WaypointUpdateEvent.Type.ADD -> updateTextComponent(it.waypoint)
                    WaypointUpdateEvent.Type.REMOVE -> waypointMap.remove(it.waypoint)
                    WaypointUpdateEvent.Type.CLEAR -> waypointMap.clear()
                    WaypointUpdateEvent.Type.RELOAD -> updateList()
                    else -> {
                        // this is fine, Java meme
                    }
                }
            }
        }

        listener<ConnectionEvent.Disconnect> {
            currentServer = null
        }
    }

    private fun updateList() {
        timer.reset()
        prevDimension = WaypointManager.genDimension()
        if (currentServer == null) {
            currentServer = WaypointManager.genServer()
        }

        val cacheList = WaypointManager.waypoints.filter {
            (it.server == null || it.server == currentServer)
                && (dimension.value == Dimension.ANY || it.dimension == prevDimension)
        }
        waypointMap.clear()

        for (waypoint in cacheList) updateTextComponent(waypoint)
    }

    private fun updateTextComponent(waypoint: Waypoint?) {
        if (waypoint == null) return

        // Don't wanna update this continuously
        waypointMap.computeIfAbsent(waypoint) {
            TextComponent().apply {
                if (showName.value) addLine(waypoint.name)
                if (showDate.value) addLine(waypoint.date)
                if (showCoords.value) addLine(waypoint.toString())
            }
        }
    }

    init {
        with(
            { synchronized(lockObject) { updateList() } } // This could be called from another thread so we have to synchronize the map
        ) {
            showName.listeners.add(this)
            showDate.listeners.add(this)
            showCoords.listeners.add(this)
            showDist.listeners.add(this)
        }

        dimension.listeners.add {
            synchronized(lockObject) {
                waypointMap.clear(); updateList()
            }
        }
    }
}