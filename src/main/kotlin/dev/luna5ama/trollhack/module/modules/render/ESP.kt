package dev.luna5ama.trollhack.module.modules.render

import dev.fastmc.common.sq
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.events.render.RenderEntityEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.graphics.GlStateUtils
import dev.luna5ama.trollhack.graphics.RenderUtils2D
import dev.luna5ama.trollhack.graphics.buffer.PersistentMappedVBO
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.esp.DynamicBoxRenderer
import dev.luna5ama.trollhack.graphics.shaders.DrawShader
import dev.luna5ama.trollhack.graphics.use
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.*
import dev.luna5ama.trollhack.util.EntityUtils.isHostile
import dev.luna5ama.trollhack.util.EntityUtils.isNeutral
import dev.luna5ama.trollhack.util.EntityUtils.isPassive
import dev.luna5ama.trollhack.util.accessor.entityOutlineFramebuffer
import dev.luna5ama.trollhack.util.accessor.entityOutlineShader
import dev.luna5ama.trollhack.util.accessor.listShaders
import dev.luna5ama.trollhack.util.accessor.renderOutlines
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.threads.ConcurrentScope
import dev.luna5ama.trollhack.util.threads.onMainThreadSafe
import kotlinx.coroutines.launch
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.shader.ShaderGroup
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.glUniform1f
import org.lwjgl.opengl.GL20.glUniform2f
import org.lwjgl.opengl.GL30.glBindVertexArray

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
    private val friend0 =
        setting("Friend", true, page.atValue(Page.ENTITY_TYPE) and all0.atFalse() and player0.atTrue())
    private val friend by friend0
    private val mob0 = setting("Mob", true, page.atValue(Page.ENTITY_TYPE) and all0.atFalse())
    private val mob by mob0
    private val passive0 =
        setting("Passive", false, page.atValue(Page.ENTITY_TYPE) and all0.atFalse() and mob0.atTrue())
    private val passive by passive0
    private val neutral0 = setting("Neutral", true, page.atValue(Page.ENTITY_TYPE) and all0.atFalse() and mob0.atTrue())
    private val neutral by neutral0
    private val hostile0 = setting("Hostile", true, page.atValue(Page.ENTITY_TYPE) and all0.atFalse() and mob0.atTrue())
    private val hostile by hostile0
    private val range by setting("Range", 32.0f, 8.0f..64.0f, 0.5f, page.atValue(Page.ENTITY_TYPE))

    /* Color settings */
    private val targetColor by setting("Target Color", ColorRGB(255, 32, 255), false, page.atValue(Page.COLOR))
    private val playerColor by setting(
        "Player Color",
        ColorRGB(150, 180, 255),
        false,
        page.atValue(Page.COLOR) and player0.atTrue()
    )
    private val friendColor by setting(
        "Friend Color",
        ColorRGB(150, 255, 180),
        false,
        page.atValue(Page.COLOR) and player0.atTrue() and friend0.atTrue()
    )
    private val passiveColor by setting(
        "Passive Color",
        ColorRGB(32, 255, 32),
        false,
        page.atValue(Page.COLOR) and mob0.atTrue() and passive0.atTrue()
    )
    private val neutralColor by setting(
        "Neutral Color",
        ColorRGB(255, 255, 32),
        false,
        page.atValue(Page.COLOR) and mob0.atTrue() and neutral0.atTrue()
    )
    private val hostileColor by setting(
        "Hostile Color",
        ColorRGB(255, 32, 32),
        false,
        page.atValue(Page.COLOR) and mob0.atTrue() and hostile0.atTrue()
    )
    private val itemColor by setting(
        "Item Color",
        ColorRGB(255, 160, 32),
        false,
        page.atValue(Page.COLOR) and item0.atTrue()
    )
    private val otherColor by setting("Other Color", ColorRGB(255, 255, 255), false, page.atValue(Page.COLOR))

    /* Rendering settings */
    private val hideOriginal by setting(
        "Hide Original",
        false,
        page.atValue(Page.RENDERING) and mode0.atValue(Mode.SHADER)
    )
    private val filled by setting(
        "Filled",
        false,
        page.atValue(Page.RENDERING) and mode0.atValue(Mode.BOX, Mode.SHADER)
    )
    private val outline by setting(
        "Outline",
        true,
        page.atValue(Page.RENDERING) and mode0.atValue(Mode.BOX, Mode.SHADER)
    )
    private val aFilled by setting(
        "Filled Alpha",
        63,
        0..255,
        1,
        page.atValue(Page.RENDERING) and mode0.atValue(Mode.BOX, Mode.SHADER)
    )
    private val aOutline by setting(
        "Outline Alpha",
        255,
        0..255,
        1,
        page.atValue(Page.RENDERING) and mode0.atValue(Mode.BOX, Mode.SHADER)
    )
    private val width by setting("Width", 2.0f, 1.0f..8.0f, 0.25f, page.atValue(Page.RENDERING))

    private enum class Page {
        ENTITY_TYPE, COLOR, RENDERING
    }

    private enum class Mode {
        BOX, GLOW, SHADER
    }


    private val boxRenderer = DynamicBoxRenderer()

    private var dirty = false
    val outlineESP get() = isEnabled && mode == Mode.SHADER

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
                && player.distanceSqTo(it.entity) <= range.sq
                && checkEntityType(it.entity)
            ) {
                it.cancel()
            }
        }

        safeListener<Render3DEvent>(69420) {
            when (mode) {
                Mode.BOX -> {
                    if (dirty) {
                        ConcurrentScope.launch {
                            updateBoxESP()
                        }
                    }
                    renderBoxESP()
                }
                Mode.SHADER -> {
                    drawShader()
                }
                else -> {
                    // Glow Mode
                }
            }
        }

        safeParallelListener<TickEvent.Post> {
            when (mode) {
                Mode.GLOW, Mode.SHADER -> {
                    boxRenderer.clear()
                    val rangeSq = range.sq
                    for (entity in EntityManager.entity) {
                        entity.isGlowing = player.distanceSqTo(entity) <= rangeSq && checkEntityType(entity)
                    }
                }
                Mode.BOX -> {
                    updateBoxESP()
                }
            }

            if (mode == Mode.BOX) {
                updateBoxESP()
            } else {
                boxRenderer.clear()
            }
        }

        safeListener<TickEvent.Post> {
            if (mode == Mode.GLOW) {
                for (shader in mc.renderGlobal.entityOutlineShader.listShaders) {
                    shader.shaderManager.getShaderUniform("Radius")?.set(width)
                }

                val rangeSq = range.sq
                for (entity in EntityManager.entity) {
                    entity.isGlowing = player.distanceSqTo(entity) <= rangeSq && checkEntityType(entity)
                }
            }
        }
    }

    private fun SafeClientEvent.updateBoxESP() {
        val rangeSq = range.sq

        synchronized(ESP) {
            boxRenderer.update {
                for (entity in EntityManager.entity) {
                    if (player.distanceSqTo(entity) > rangeSq) continue
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

    private fun drawShader() {
        val framebufferIn = mc.renderGlobal.entityOutlineFramebuffer
        framebufferIn.setFramebufferFilter(GL_NEAREST)
        framebufferIn.bindFramebufferTexture()

        GlStateUtils.blend(true)
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ZERO, GL_ONE)
        GlStateUtils.depth(false)

        Shader.use {
            updateUniforms()
            RenderUtils2D.putVertex(-1.0f, 1.0f, ColorRGB(0))
            RenderUtils2D.putVertex(-1.0f, -1.0f, ColorRGB(0))
            RenderUtils2D.putVertex(1.0f, 1.0f, ColorRGB(0))
            RenderUtils2D.putVertex(1.0f, -1.0f, ColorRGB(0))
            RenderUtils2D.putVertex(1.0f, 1.0f, ColorRGB(0))
            RenderUtils2D.putVertex(-1.0f, -1.0f, ColorRGB(0))

            glBindVertexArray(PersistentMappedVBO.POS2_COLOR)
            glDrawArrays(GL_TRIANGLES, PersistentMappedVBO.drawOffset, RenderUtils2D.vertexSize)
            PersistentMappedVBO.end()
            glBindVertexArray(0)
            RenderUtils2D.vertexSize = 0
        }

        GlStateUtils.depth(true)

        framebufferIn.unbindFramebufferTexture()

        framebufferIn.framebufferColor[0]= 0.0f
        framebufferIn.framebufferColor[1]= 0.0f
        framebufferIn.framebufferColor[2]= 0.0f
        framebufferIn.framebufferColor[3]= 0.0f
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
            if (mode == Mode.GLOW  || mode == Mode.SHADER) {
                resetGlow()
            }
        }

        mode0.valueListeners.add { prev, _ ->
            if (isEnabled && (prev == Mode.GLOW || prev == Mode.SHADER)) {
                resetGlow()
            }
        }
    }

    private fun resetGlow() {
        onMainThreadSafe {
            for (shader in mc.renderGlobal.entityOutlineShader.listShaders) {
                shader.shaderManager.getShaderUniform("Radius")?.set(2.0f) // default radius
            }

            for (entity in EntityManager.entity) {
                entity.isGlowing = false
            }
        }
    }

    private object Shader: DrawShader("/assets/trollhack/shaders/OutlineESP.vert.glsl", "/assets/trollhack/shaders/OutlineESP.frag.glsl") {
        fun updateUniforms() {
            glUniform2f(0, 1.0f/ (mc.displayWidth * AntiAlias.sampleLevel), 1.0f/ (mc.displayHeight * AntiAlias.sampleLevel))
            glUniform1f(1, if (!outline) 0.0f else aOutline / 255.0f)
            glUniform1f(2, if (!filled) 0.0f else aFilled / 255.0f)
            glUniform1f(3, width / 2.0f)
        }
    }

    object NoOpShaderGroup : ShaderGroup(mc.textureManager, mc.resourceManager, mc.framebuffer, ResourceLocation("shaders/post/noop.json")) {
        override fun render(partialTicks: Float) {
            // NO-OP
        }
    }
}