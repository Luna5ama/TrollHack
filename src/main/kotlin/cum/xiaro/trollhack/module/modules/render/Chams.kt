package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.render.RenderEntityEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.CombatManager
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.module.modules.combat.CombatSetting
import cum.xiaro.trollhack.util.EntityUtils
import cum.xiaro.trollhack.util.EntityUtils.mobTypeSettings
import cum.xiaro.trollhack.util.and
import cum.xiaro.trollhack.util.atFalse
import cum.xiaro.trollhack.util.atValue
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.graphics.color.HueCycler
import cum.xiaro.trollhack.util.graphics.color.setGLColor
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11.*

internal object Chams : Module(
    name = "Chams",
    category = Category.RENDER,
    description = "Modify entity rendering"
) {
    private val renderMode0 = setting("Render Mode", RenderMode.BOTH)
    private val renderMode by renderMode0
    private val page = setting("Page", Page.ENTITY_TYPE)

    /* Entity type settings */
    private val self by setting("Self", false, page.atValue(Page.ENTITY_TYPE))
    private val all0 = setting("All Entities", false, page.atValue(Page.ENTITY_TYPE))
    private val all by all0
    private val items by setting("Item", false, page.atValue(Page.ENTITY_TYPE) and all0.atFalse())
    private val players by setting("Player", true, page.atValue(Page.ENTITY_TYPE) and all0.atFalse())
    private val friends by setting("Friend", false, page.atValue(Page.ENTITY_TYPE) and all0.atFalse())
    private val mobs by setting("Mobs", true, page.atValue(Page.ENTITY_TYPE) and all0.atFalse())
    private val passive by setting("Passive", false, page.atValue(Page.ENTITY_TYPE) and all0.atFalse())
    private val neutral by setting("Neutral", true, page.atValue(Page.ENTITY_TYPE) and all0.atFalse())
    private val hostile by setting("Hostile", true, page.atValue(Page.ENTITY_TYPE) and all0.atFalse())

    /* Visible rendering settings */
    private val shadeMode0 = setting("Shade Mode", ShadeMode.BOTH, page.atValue(Page.VISIBLE_RENDERING) and renderMode0.atValue(RenderMode.VISIBLE, RenderMode.BOTH))
    private val shadeMode by shadeMode0
    private val texture by setting("Texture", false, page.atValue(Page.VISIBLE_RENDERING) and renderMode0.atValue(RenderMode.VISIBLE, RenderMode.BOTH) and shadeMode0.atValue(ShadeMode.FILLED, ShadeMode.BOTH))
    private val lighting by setting("Lighting", false, page.atValue(Page.VISIBLE_RENDERING) and renderMode0.atValue(RenderMode.VISIBLE, RenderMode.BOTH) and shadeMode0.atValue(ShadeMode.FILLED, ShadeMode.BOTH))
    private val rainbow0 = setting("Rainbow", false, page.atValue(Page.VISIBLE_RENDERING) and renderMode0.atValue(RenderMode.VISIBLE, RenderMode.BOTH))
    private val rainbow by rainbow0
    private val color by setting("Color", ColorRGB(255, 255, 255), false, page.atValue(Page.VISIBLE_RENDERING) and renderMode0.atValue(RenderMode.VISIBLE, RenderMode.BOTH) and rainbow0.atFalse())
    private val aFilled by setting("Filled Alpha", 127, 0..255, 1, page.atValue(Page.VISIBLE_RENDERING) and renderMode0.atValue(RenderMode.VISIBLE, RenderMode.BOTH) and shadeMode0.atValue(ShadeMode.FILLED, ShadeMode.BOTH))
    private val aOutline by setting("Outline Alpha", 255, 0..255, 1, page.atValue(Page.VISIBLE_RENDERING) and renderMode0.atValue(RenderMode.VISIBLE, RenderMode.BOTH) and shadeMode0.atValue(ShadeMode.OUTLINE, ShadeMode.BOTH))
    private val width by setting("Width", 2.0f, 1.0f..8.0f, 0.1f, page.atValue(Page.VISIBLE_RENDERING) and renderMode0.atValue(RenderMode.VISIBLE, RenderMode.BOTH) and shadeMode0.atValue(ShadeMode.OUTLINE, ShadeMode.BOTH))

    /* Invisible rendering settings */
    private val shadeModeInvisible0 = setting("Shade Mode Invisible", ShadeMode.OUTLINE, page.atValue(Page.INVISIBLE_RENDERING) and renderMode0.atValue(RenderMode.INVISIBLE, RenderMode.BOTH))
    private val shadeModeInvisible by shadeModeInvisible0
    private val textureInvisible by setting("Texture Invisible", false, page.atValue(Page.INVISIBLE_RENDERING) and renderMode0.atValue(RenderMode.INVISIBLE, RenderMode.BOTH) and shadeModeInvisible0.atValue(ShadeMode.FILLED, ShadeMode.BOTH))
    private val lightingInvisible by setting("Lighting Invisible", false, page.atValue(Page.INVISIBLE_RENDERING) and renderMode0.atValue(RenderMode.INVISIBLE, RenderMode.BOTH) and shadeModeInvisible0.atValue(ShadeMode.FILLED, ShadeMode.BOTH))
    private val rainbowInvisible0 = setting("Rainbow Invisible", false, page.atValue(Page.INVISIBLE_RENDERING) and renderMode0.atValue(RenderMode.INVISIBLE, RenderMode.BOTH))
    private val rainbowInvisible by rainbowInvisible0
    private val colorInvisible by setting("Color Invisible", ColorRGB(255, 255, 255), false, page.atValue(Page.INVISIBLE_RENDERING) and renderMode0.atValue(RenderMode.INVISIBLE, RenderMode.BOTH) and rainbow0.atFalse())
    private val aFilledInvisible by setting("Filled Alpha Invisible", 127, 0..255, 1, page.atValue(Page.INVISIBLE_RENDERING) and renderMode0.atValue(RenderMode.INVISIBLE, RenderMode.BOTH) and shadeModeInvisible0.atValue(ShadeMode.FILLED, ShadeMode.BOTH))
    private val aOutlineInvisible by setting("Outline Alpha Invisible", 255, 0..255, 1, page.atValue(Page.INVISIBLE_RENDERING) and renderMode0.atValue(RenderMode.INVISIBLE, RenderMode.BOTH) and shadeModeInvisible0.atValue(ShadeMode.OUTLINE, ShadeMode.BOTH))
    private val widthInvisible by setting("Width Invisible", 2.0f, 1.0f..8.0f, 0.1f, page.atValue(Page.INVISIBLE_RENDERING) and renderMode0.atValue(RenderMode.INVISIBLE, RenderMode.BOTH) and shadeModeInvisible0.atValue(ShadeMode.OUTLINE, ShadeMode.BOTH))

    private enum class RenderMode {
        VISIBLE, INVISIBLE, BOTH
    }

    private enum class ShadeMode {
        OUTLINE, FILLED, BOTH
    }

    private enum class Page {
        ENTITY_TYPE, VISIBLE_RENDERING, INVISIBLE_RENDERING
    }

    private var cycler = HueCycler(600)

    init {
        listener<RenderEntityEvent.Model.Pre> {
            if (it.cancelled || !checkEntityType(it.entity)) return@listener

            if (renderMode == RenderMode.BOTH) {
                chamsPre(true)
                GlStateManager.depthMask(false)
                renderFilled(it, shadeModeInvisible, textureInvisible, lightingInvisible, rainbowInvisible, colorInvisible, aFilledInvisible)
                renderOutline(it, shadeModeInvisible, textureInvisible, lightingInvisible, rainbowInvisible, colorInvisible, aOutlineInvisible, widthInvisible)
                GlStateManager.depthMask(true)
            }

            if (renderMode == RenderMode.INVISIBLE) {
                chamsPre(true)
                render(it, shadeModeInvisible, textureInvisible, lightingInvisible, rainbowInvisible, colorInvisible, aOutlineInvisible, aFilledInvisible, widthInvisible)
            } else {
                chamsPre(false)
                render(it, shadeMode, texture, lighting, rainbow, color, aOutline, aFilled, width)
            }
        }
        listener<RenderEntityEvent.Model.Post> {
            if (it.cancelled || !checkEntityType(it.entity)) return@listener

            chamsPost()
        }

        listener<RenderEntityEvent.All.Post> {
            if (!it.cancelled && checkEntityType(it.entity)) {
                glDepthRange(0.0, 1.0)
            }
        }

        safeListener<TickEvent.Post> {
            cycler++
        }
    }

    private fun render(event: RenderEntityEvent.Model, shadeMode: ShadeMode, texture: Boolean, lighting: Boolean, rainbow: Boolean, color: ColorRGB, aOutline: Int, aFilled: Int, width: Float) {
        when (shadeMode) {
            ShadeMode.OUTLINE -> {
                setColor(rainbow, color, aOutline)
                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
                glLineWidth(width)

                GlStateUtils.texture2d(false)
                GlStateUtils.lighting(false)
            }
            ShadeMode.FILLED -> {
                setColor(rainbow, color, aFilled)
                glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)

                GlStateUtils.texture2d(texture)
                GlStateUtils.lighting(lighting)
            }
            ShadeMode.BOTH -> {
                renderFilled(event, shadeMode, texture, lighting, rainbow, color, aFilled)

                setColor(rainbow, color, aOutline)
                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
                glLineWidth(width)

                GlStateUtils.texture2d(false)
                GlStateUtils.lighting(false)
            }
        }
    }

    private fun renderOutline(event: RenderEntityEvent.Model, shadeMode: ShadeMode, texture: Boolean, lighting: Boolean, rainbow: Boolean, color: ColorRGB, alpha: Int, width: Float) {
        if (shadeMode == ShadeMode.FILLED) return

        setColor(rainbow, color, alpha)
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
        glLineWidth(width)

        GlStateUtils.texture2d(false)
        GlStateUtils.lighting(false)

        event.render()

        GlStateUtils.texture2d(texture)
        GlStateUtils.lighting(lighting)
    }

    private fun renderFilled(event: RenderEntityEvent.Model, shadeMode: ShadeMode, texture: Boolean, lighting: Boolean, rainbow: Boolean, color: ColorRGB, alpha: Int) {
        if (shadeMode == ShadeMode.OUTLINE) return

        setColor(rainbow, color, alpha)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
        GlStateUtils.texture2d(texture)
        GlStateUtils.lighting(lighting)
        event.render()
    }

    private fun setColor(rainbow: Boolean, color: ColorRGB, alpha: Int) {
        if (rainbow) cycler.currentRgba(alpha).setGLColor()
        else glColor4f(color.r / 255.0f, color.g / 255.0f, color.b / 255.0f, alpha / 255.0f)
    }

    private fun chamsPre(invisible: Boolean) {
        if (invisible) glDepthRange(0.0, 0.01) else glDepthRange(0.0, 1.0)
        GlStateUtils.blend(true)
        glEnable(GL_LINE_SMOOTH)
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)

        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
    }

    private fun chamsPost() {
        GlStateUtils.texture2d(true)
        GlStateUtils.lighting(true)
        GlStateUtils.blend(false)
        glDisable(GL_LINE_SMOOTH)

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
        glLineWidth(1.0f)
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
    }

    private fun checkEntityType(entity: Entity) =
        (!CombatSetting.chams || entity != CombatManager.target)
            && (self || entity != mc.renderViewEntity)
            && (all
            || items && entity is EntityItem
            || players && entity is EntityPlayer && EntityUtils.playerTypeCheck(entity, friends, true)
            || mobTypeSettings(entity, mobs, passive, neutral, hostile))
}
