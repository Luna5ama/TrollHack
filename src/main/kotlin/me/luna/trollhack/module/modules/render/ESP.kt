package me.luna.trollhack.module.modules.render

import kotlinx.coroutines.launch
import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.TickEvent
import me.luna.trollhack.event.events.WorldEvent
import me.luna.trollhack.event.events.render.FogColorEvent
import me.luna.trollhack.event.events.render.Render3DEvent
import me.luna.trollhack.event.events.render.RenderEntityEvent
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.event.safeParallelListener
import me.luna.trollhack.manager.managers.CombatManager
import me.luna.trollhack.manager.managers.EntityManager
import me.luna.trollhack.manager.managers.FriendManager
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.*
import me.luna.trollhack.util.EntityUtils.isHostile
import me.luna.trollhack.util.EntityUtils.isNeutral
import me.luna.trollhack.util.EntityUtils.isPassive
import me.luna.trollhack.util.accessor.*
import me.luna.trollhack.util.extension.sq
import me.luna.trollhack.util.graphics.GlStateUtils
import me.luna.trollhack.util.graphics.RenderUtils3D
import me.luna.trollhack.util.graphics.ShaderHelper
import me.luna.trollhack.util.graphics.color.ColorRGB
import me.luna.trollhack.util.graphics.esp.DynamicBoxRenderer
import me.luna.trollhack.util.math.MathUtils
import me.luna.trollhack.util.threads.defaultScope
import me.luna.trollhack.util.threads.onMainThreadSafe
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.shader.Shader
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import kotlin.math.pow

internal object ESP : Module(
    name = "ESP",
    category = Category.RENDER,
    description = "Highlights entities"
) {
    private val mode0 = setting("Mode", Mode.SHADER)
    private val mode by mode0
    private val page = setting("Page", Page.ENTITY_TYPE)

    /* Entity type settings */
    private val all0 = setting("All", false, page.atValue(Page.ENTITY_TYPE))
    private val all by all0
    private val item0 = setting("Item", true, page.atValue(Page.ENTITY_TYPE) and all0.atFalse())
    private val item by item0
    private val player0 = setting("Player", true, page.atValue(Page.ENTITY_TYPE) and all0.atFalse())
    private val player by player0
    private val friend0 = setting("Friend", true, page.atValue(Page.ENTITY_TYPE) and all0.atFalse() and player0.atTrue())
    private val friend by friend0
    private val mob0 = setting("Mob", true, page.atValue(Page.ENTITY_TYPE) and all0.atFalse())
    private val mob by mob0
    private val passive0 = setting("Passive", false, page.atValue(Page.ENTITY_TYPE) and all0.atFalse() and mob0.atTrue())
    private val passive by passive0
    private val neutral0 = setting("Neutral", true, page.atValue(Page.ENTITY_TYPE) and all0.atFalse() and mob0.atTrue())
    private val neutral by neutral0
    private val hostile0 = setting("Hostile", true, page.atValue(Page.ENTITY_TYPE) and all0.atFalse() and mob0.atTrue())
    private val hostile by hostile0
    private val range by setting("Range", 32.0f, 8.0f..64.0f, 0.5f, page.atValue(Page.ENTITY_TYPE))

    /* Color settings */
    private val targetColor by setting("Target Color", ColorRGB(255, 32, 255), false, page.atValue(Page.COLOR))
    private val playerColor by setting("Player Color", ColorRGB(150, 180, 255), false, page.atValue(Page.COLOR) and player0.atTrue())
    private val friendColor by setting("Friend Color", ColorRGB(150, 255, 180), false, page.atValue(Page.COLOR) and player0.atTrue() and friend0.atTrue())
    private val passiveColor by setting("Passive Color", ColorRGB(32, 255, 32), false, page.atValue(Page.COLOR) and mob0.atTrue() and passive0.atTrue())
    private val neutralColor by setting("Neutral Color", ColorRGB(255, 255, 32), false, page.atValue(Page.COLOR) and mob0.atTrue() and neutral0.atTrue())
    private val hostileColor by setting("Hostile Color", ColorRGB(255, 32, 32), false, page.atValue(Page.COLOR) and mob0.atTrue() and hostile0.atTrue())
    private val itemColor by setting("Item Color", ColorRGB(255, 160, 32), false, page.atValue(Page.COLOR) and item0.atTrue())
    private val otherColor by setting("Other Color", ColorRGB(255, 255, 255), false, page.atValue(Page.COLOR))

    /* Rendering settings */
    private val hideOriginal by setting("Hide Original", false, page.atValue(Page.RENDERING) and mode0.atValue(Mode.SHADER))
    private val filled by setting("Filled", false, page.atValue(Page.RENDERING) and mode0.atValue(Mode.BOX, Mode.SHADER))
    private val outline by setting("Outline", true, page.atValue(Page.RENDERING) and mode0.atValue(Mode.BOX, Mode.SHADER))
    private val aFilled by setting("Filled Alpha", 63, 0..255, 1, page.atValue(Page.RENDERING) and mode0.atValue(Mode.BOX, Mode.SHADER))
    private val aOutline by setting("Outline Alpha", 255, 0..255, 1, page.atValue(Page.RENDERING) and mode0.atValue(Mode.BOX, Mode.SHADER))
    private val width by setting("Width", 2.0f, 1.0f..8.0f, 0.25f, page.atValue(Page.RENDERING))
    private val ratio by setting("Ratio", 0.5f, 0.0f..1.0f, 0.05f, page.atValue(Page.RENDERING) and mode0.atValue(Mode.SHADER))

    private enum class Page {
        ENTITY_TYPE, COLOR, RENDERING
    }

    private enum class Mode {
        BOX, GLOW, SHADER
    }

    private val shaderHelper = ShaderHelper(ResourceLocation("shaders/post/esp_outline.json"))
    private val frameBufferFinal = shaderHelper.getFrameBuffer("final")
    private val boxRenderer = DynamicBoxRenderer()

    private var dirty = false

    init {
        onDisable {
            boxRenderer.clear()
        }

        safeListener<WorldEvent.Entity.Add> {
            synchronized(ESP) {
                dirty = true
            }
        }

        safeListener<WorldEvent.Entity.Add> {
            synchronized(ESP) {
                dirty = true
            }
        }

        safeListener<RenderEntityEvent.All.Pre> {
            if (mode == Mode.SHADER
                && !mc.renderManager.renderOutlines
                && hideOriginal
                && player.getDistanceSq(it.entity) <= range.sq
                && checkEntityType(it.entity)) {
                it.cancel()
            }
        }

        safeListener<FogColorEvent> { event ->
            shaderHelper.shader?.listFrameBuffers?.forEach {
                it.setFramebufferColor(event.red, event.green, event.blue, 0.0f)
            }
        }

        safeListener<Render3DEvent>(69420) {
            when (mode) {
                Mode.BOX -> {
                    if (dirty) {
                        defaultScope.launch {
                            updateBoxESP()
                        }
                    }
                    renderBoxESP()
                }
                Mode.SHADER -> {
                    GlStateUtils.useProgramForce(0)
                    clearFrameBuffer()
                    drawEntities()
                    drawShader()
                }
                else -> {
                    // Glow Mode
                }
            }
        }

        safeParallelListener<TickEvent.Post> {
            if (mode == Mode.BOX) {
                updateBoxESP()
            } else {
                boxRenderer.clear()
            }
        }

        safeListener<TickEvent.Post> {
            when (mode) {
                Mode.GLOW -> {
                    for (shader in mc.renderGlobal.entityOutlineShader.listShaders) {
                        shader.shaderManager.getShaderUniform("Radius")?.set(width)
                    }

                    val rangeSq = range.sq
                    for (entity in world.loadedEntityList) {
                        entity.isGlowing = player.getDistanceSq(entity) <= rangeSq && checkEntityType(entity)
                    }
                }
                Mode.SHADER -> {
                    shaderHelper.shader?.let {
                        for (shader in it.listShaders) {
                            setShaderUniforms(shader)
                        }
                    }
                }
                else -> {
                    // Box Mode
                }
            }
        }
    }

    private fun SafeClientEvent.updateBoxESP() {
        val rangeSq = range.sq

        synchronized(ESP) {
            boxRenderer.update {
                for (entity in EntityManager.entity) {
                    if (player.getDistanceSq(entity) > rangeSq) continue
                    if (!checkEntityType(entity)) continue

                    val xOffset = entity.posX - entity.lastTickPosX
                    val yOffset = entity.posY - entity.lastTickPosY
                    val zOffset = entity.posZ - entity.lastTickPosZ

                    putBox(entity.renderBoundingBox, xOffset, yOffset, zOffset, getEntityColor(entity))
                }
            }

            dirty = false
        }
    }

    private fun renderBoxESP() {
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)
        GlStateManager.glLineWidth(width)
        GlStateUtils.depth(false)

        boxRenderer.render(aFilled, aOutline)

        GlStateUtils.depth(true)
        GlStateManager.glLineWidth(1.0f)
    }

    private fun clearFrameBuffer() {
        // Bind the frame buffer and clear it
        frameBufferFinal!!.apply {
            bindFramebuffer(true)
            GlStateManager.clearColor(framebufferColor[0], framebufferColor[1], framebufferColor[2], framebufferColor[3])

            var mask = 16384
            if (useDepth) {
                GlStateManager.clearDepth(1.0)
                mask = mask or 256
            }

            GlStateManager.clear(mask)
        }
    }

    private fun SafeClientEvent.drawEntities() {
        val rangeSq = range.sq

        GlStateUtils.texture2d(true)
        GlStateUtils.alpha(true)
        GlStateUtils.depth(true)
        GlStateManager.depthMask(true)

        val camera = Frustum()
        val viewEntity = mc.renderViewEntity ?: player
        val partialTicks = RenderUtils3D.partialTicks
        val partialTicksD = RenderUtils3D.partialTicks.toDouble()
        val x = MathUtils.lerp(viewEntity.lastTickPosX, viewEntity.posX, partialTicksD)
        val y = MathUtils.lerp(viewEntity.lastTickPosY, viewEntity.posY, partialTicksD)
        val z = MathUtils.lerp(viewEntity.lastTickPosZ, viewEntity.posZ, partialTicksD)

        camera.setPosition(x, y, z)

        // Draw the entities into the framebuffer
        for (entity in world.loadedEntityList) {
            if (player.getDistanceSq(entity) > rangeSq) continue
            if (!checkEntityType(entity)) continue

            val renderer = mc.renderManager.getEntityRenderObject<Entity>(entity) ?: continue

            if (!renderer.shouldRender(entity, camera, x, y, z)) continue

            val yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks
            val pos = EntityUtils.getInterpolatedPos(entity, partialTicks)
                .subtract(mc.renderManager.renderPosX, mc.renderManager.renderPosY, mc.renderManager.renderPosZ)

            renderer.setRenderOutlines(true)
            renderer.doRender(entity, pos.x, pos.y, pos.z, yaw, partialTicks)
        }

        GlStateUtils.texture2d(false)
        GlStateUtils.alpha(false)
    }

    private fun drawShader() {
        // Push matrix
        GlStateUtils.pushMatrixAll()

        GlStateUtils.blend(true)
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)

        shaderHelper.shader!!.render(RenderUtils3D.partialTicks)

        // Re-enable blend because shader rendering will disable it at the end
        GlStateUtils.blend(true)
        GlStateUtils.depth(false)

        // Draw it on the main frame buffer
        mc.framebuffer.bindFramebuffer(false)
        frameBufferFinal!!.framebufferRenderExt(mc.displayWidth, mc.displayHeight, false)

        // Revert states
        GlStateUtils.blend(true)
        GlStateUtils.depth(true)
        GlStateUtils.texture2d(false)
        GlStateManager.depthMask(false)

        // Revert matrix
        GlStateUtils.popMatrixAll()
    }

    private fun setShaderUniforms(shader: Shader) {
        val width = width + 2.0f

        shader.shaderManager.getShaderUniform("outlineAlpha")?.set(if (outline) aOutline / 255.0f else 0.0f)
        shader.shaderManager.getShaderUniform("filledAlpha")?.set(if (filled) aFilled / 255.0f else 0.0f)
        shader.shaderManager.getShaderUniform("width")?.set(width)
        shader.shaderManager.getShaderUniform("widthSq")?.set(width.pow(2))
        shader.shaderManager.getShaderUniform("ratio")?.set((width - 1.0f) * ratio.pow(3) + 1.0f)
    }

    private fun checkEntityType(entity: Entity) =
        entity != mc.renderViewEntity && (all
            || item && entity is EntityItem
            || player && entity is EntityPlayer && (friend || FriendManager.isFriend(entity.name)))
            || EntityUtils.mobTypeSettings(entity, mob, passive, neutral, hostile)

    private fun getEntityColor(entity: Entity) =
        when (entity) {
            CombatManager.target -> {
                targetColor
            }
            is EntityItem -> {
                itemColor
            }
            is EntityPlayer -> {
                if (FriendManager.isFriend(entity.name)) friendColor
                else playerColor
            }
            else -> {
                when {
                    entity.isPassive -> passiveColor
                    entity.isNeutral -> neutralColor
                    entity.isHostile -> hostileColor
                    else -> otherColor
                }
            }
        }

    @JvmStatic
    fun getEspColor(entity: Entity): Int? {
        if (isDisabled || mode == Mode.BOX || !checkEntityType(entity)) return null
        val color = getEntityColor(entity)
        return (color.a shl 24) or (color.r shl 16) or (color.g shl 8) or (color.b)
    }

    init {
        onDisable {
            if (mode == Mode.GLOW) {
                resetGlow()
            }
        }

        mode0.valueListeners.add { prev, _ ->
            if (isEnabled && prev == Mode.GLOW) {
                resetGlow()
            }
        }
    }

    private fun resetGlow() {
        onMainThreadSafe {
            for (shader in mc.renderGlobal.entityOutlineShader.listShaders) {
                shader.shaderManager.getShaderUniform("Radius")?.set(2.0f) // default radius
            }

            for (entity in world.loadedEntityList) {
                entity.isGlowing = false
            }
        }
    }
}