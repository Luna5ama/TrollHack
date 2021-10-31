package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.render.RenderOverlayEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeConcurrentListener
import cum.xiaro.trollhack.manager.managers.EntityManager
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.accessor.mapBossInfos
import cum.xiaro.trollhack.util.accessor.render
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import net.minecraft.client.gui.BossInfoClient
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.RenderGameOverlayEvent
import org.lwjgl.opengl.GL11.*
import kotlin.math.abs
import kotlin.math.roundToInt

internal object BossStack : Module(
    name = "BossStack",
    description = "Modify the boss health GUI to take up less space",
    category = Category.RENDER
) {
    private val mode by setting("Mode", BossStackMode.STACK)
    private val scale by setting("Scale", 1.0f, 0.1f..5.0f, 0.25f)
    private val xOffset by setting("X Offset", 0, -500..500, 1)
    private val yOffset by setting("Y Offset", 0, 0..1000, 1)
    private val censor by setting("Censor", false)

    @Suppress("unused")
    private enum class BossStackMode {
        REMOVE, MINIMIZE, STACK
    }

    private val texture = ResourceLocation("textures/gui/bars.png")
    private var bossInfoList = emptyList<Pair<BossInfoClient, Int>>()

    init {
        listener<RenderOverlayEvent.Pre> {
            if (it.type != RenderGameOverlayEvent.ElementType.BOSSHEALTH) return@listener

            it.cancel()
            drawHealthBar()
        }

        safeConcurrentListener<TickEvent.Post> {
            updateBossInfoMap()
        }
    }

    private fun SafeClientEvent.updateBossInfoMap() {
        val newList = ArrayList<Pair<BossInfoClient, Int>>()
        val bossInfoList = mc.ingameGUI.bossOverlay.mapBossInfos?.values ?: return

        when (mode) {
            BossStackMode.MINIMIZE -> {
                val closest = getMatchBoss(bossInfoList) ?: return
                newList.add(closest to -1)
            }
            BossStackMode.STACK -> {
                val cacheMap = HashMap<String, ArrayList<BossInfoClient>>()

                for (bossInfo in bossInfoList) {
                    val name = if (censor) "Boss" else bossInfo.name.formattedText
                    val list = cacheMap.getOrPut(name, ::ArrayList)
                    list.add(bossInfo)
                }

                for ((name, list) in cacheMap) {
                    val closest = getMatchBoss(list, name) ?: continue
                    newList.add(closest to list.size)
                }
            }
            else -> {
            }
        }

        BossStack.bossInfoList = newList
    }

    private fun SafeClientEvent.getMatchBoss(list: Collection<BossInfoClient>, name: String? = null): BossInfoClient? {
        val closestBossHealth = getClosestBoss(name)?.let {
            it.health / it.maxHealth
        } ?: return null

        return list.minByOrNull {
            abs(it.percent - closestBossHealth)
        }
    }

    private fun SafeClientEvent.getClosestBoss(name: String?): EntityLivingBase? {
        return EntityManager.entity.asSequence()
            .filterIsInstance<EntityLivingBase>()
            .filterNot { it.isNonBoss }
            .run {
                if (name != null) filter { it.displayName.formattedText == name }
                else this
            }
            .minByOrNull { it.getDistanceSq(player) }
    }

    private fun drawHealthBar() {
        mc.profiler.startSection("bossHealth")

        val width = ScaledResolution(mc).scaledWidth
        var posY = 12 + yOffset

        GlStateUtils.blend(true)
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

        for ((bossInfo, count) in bossInfoList) {
            val posX = (width / scale / 2.0f - 91.0f).roundToInt() + xOffset
            val name = if (censor) "Boss" else bossInfo.name.formattedText
            val text = (name) + if (count != -1) " x$count" else ""
            val textPosX = width / scale / 2.0f - mc.fontRenderer.getStringWidth(text) / 2.0f
            val textPosY = posY - 9.0f

            GlStateManager.pushMatrix()
            glScalef(scale, scale, 1.0f)
            mc.textureManager.bindTexture(texture)
            mc.ingameGUI.bossOverlay.render(posX, posY, bossInfo)
            mc.fontRenderer.drawStringWithShadow(text, textPosX, textPosY, 0xffffff)
            GlStateManager.popMatrix()

            posY += 10 + mc.fontRenderer.FONT_HEIGHT
        }

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        mc.profiler.endSection()
    }
}