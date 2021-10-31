package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.util.math.MathUtils.convertRange
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.render.Render3DEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeParallelListener
import cum.xiaro.trollhack.manager.managers.FriendManager
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.EntityUtils.getTargetList
import cum.xiaro.trollhack.util.EntityUtils.isNeutral
import cum.xiaro.trollhack.util.EntityUtils.isPassive
import cum.xiaro.trollhack.util.and
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.atValue
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.graphics.esp.DynamicTracerRenderer
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11.*

internal object Tracers : Module(
    name = "Tracers",
    description = "Draws lines to other living entities",
    category = Category.RENDER
) {
    private val page = setting("Page", Page.ENTITY_TYPE)

    /* Entity type settings */
    private val players0 = setting("Players", true, page.atValue(Page.ENTITY_TYPE))
    private val players by players0
    private val friends by setting("Friends", false, page.atValue(Page.ENTITY_TYPE) and players0.atTrue())
    private val mobs0 = setting("Mobs", true, page.atValue(Page.ENTITY_TYPE))
    private val mobs by mobs0
    private val passive by setting("Passive", false, page.atValue(Page.ENTITY_TYPE) and mobs0.atTrue())
    private val neutral by setting("Neutral", true, page.atValue(Page.ENTITY_TYPE) and mobs0.atTrue())
    private val hostile by setting("Hostile", true, page.atValue(Page.ENTITY_TYPE) and mobs0.atTrue())
    private val range by setting("Range", 64, 8..512, 8, page.atValue(Page.ENTITY_TYPE))

    /* Color settings */
    private val colorPlayer by setting("Player Color", ColorRGB(255, 160, 240), false, page.atValue(Page.COLOR))
    private val colorFriend by setting("Friend Color", ColorRGB(32, 250, 32), false, page.atValue(Page.COLOR))
    private val colorPassive by setting("Passive Mob Color", ColorRGB(132, 240, 32), false, page.atValue(Page.COLOR))
    private val colorNeutral by setting("Neutral Mob Color", ColorRGB(255, 232, 0), false, page.atValue(Page.COLOR))
    private val colorHostile by setting("Hostile Mob Color", ColorRGB(250, 32, 32), false, page.atValue(Page.COLOR))
    private val colorFar by setting("Far Color", ColorRGB(255, 255, 255), false, page.atValue(Page.COLOR))

    /*Rendering settings */
    private val rangedColor0 = setting("Ranged Color", true, page.atValue(Page.RENDERING))
    private val rangedColor by rangedColor0
    private val colorChangeRange by setting("Color Change Range", 16, 8..128, 8, page.atValue(Page.RENDERING) and rangedColor0.atTrue())
    private val playerOnly by setting("Player Only", true, page.atValue(Page.RENDERING) and rangedColor0.atTrue())
    private val aFar by setting("Far Alpha", 255, 0..255, 1, page.atValue(Page.RENDERING) and rangedColor0.atTrue())
    private val tracerAlpha by setting("Tracer Alpha", 255, 0..255, 1, page.atValue(Page.RENDERING))
    private val yOffset by setting("Y Offset", 0.0f, 0.0f..1.0f, 0.05f, page.atValue(Page.RENDERING))
    private val lineWidth by setting("Line Width", 2.0f, 0.25f..8.0f, 0.25f, page.atValue(Page.RENDERING))

    private enum class Page {
        ENTITY_TYPE, COLOR, RENDERING
    }

    private val tracerRenderer = DynamicTracerRenderer()

    init {
        listener<Render3DEvent> {
            glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)
            glLineWidth(lineWidth)
            GlStateUtils.depth(false)

            tracerRenderer.render(tracerAlpha)

            GlStateUtils.cull(true)
            GlStateUtils.depth(true)
            glLineWidth(1.0f)
        }

        safeParallelListener<TickEvent.Post> {
            val player = arrayOf(players, friends, true)
            val mob = arrayOf(mobs, passive, neutral, hostile)
            val entityList = getTargetList(player, mob, true, range.toFloat(), ignoreSelf = false)

            tracerRenderer.update {
                for (entity in entityList) {
                    val xOffset = entity.posX - entity.lastTickPosX
                    val yOffset = entity.posY - entity.lastTickPosY
                    val zOffset = entity.posZ - entity.lastTickPosZ

                    val y = entity.posY + entity.height * Tracers.yOffset

                    putTracer(entity.posX, y, entity.posZ, xOffset, yOffset, zOffset, getColor(entity))
                }
            }
        }
    }

    private fun SafeClientEvent.getColor(entity: Entity): ColorRGB {
        val color = when {
            FriendManager.isFriend(entity.name) -> colorFriend
            entity is EntityPlayer -> colorPlayer
            entity.isPassive -> colorPassive
            entity.isNeutral -> colorNeutral
            else -> colorHostile
        }

        return getRangedColor(entity, color)
    }

    private fun SafeClientEvent.getRangedColor(entity: Entity, color: ColorRGB): ColorRGB {
        if (!rangedColor || playerOnly && entity !is EntityPlayer) return color
        val distance = player.getDistance(entity)

        val r = convertRange(distance, 8.0f, colorChangeRange.toFloat(), color.r.toFloat(), colorFar.r.toFloat()).toInt()
        val g = convertRange(distance, 8.0f, colorChangeRange.toFloat(), color.g.toFloat(), colorFar.g.toFloat()).toInt()
        val b = convertRange(distance, 8.0f, colorChangeRange.toFloat(), color.b.toFloat(), colorFar.b.toFloat()).toInt()
        val a = convertRange(distance, 8.0f, colorChangeRange.toFloat(), tracerAlpha.toFloat(), aFar.toFloat()).toInt()
        return ColorRGB(r, g, b, a)
    }
}
