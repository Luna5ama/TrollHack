package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.render.ResolutionUpdateEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.accessor.renderPosX
import cum.xiaro.trollhack.util.accessor.renderPosY
import cum.xiaro.trollhack.util.accessor.renderPosZ
import cum.xiaro.trollhack.util.graphics.Resolution
import cum.xiaro.trollhack.util.graphics.buffer.FrameBuffer
import cum.xiaro.trollhack.util.graphics.shaders.Shader
import cum.xiaro.trollhack.util.graphics.fastrender.tileentity.*
import cum.xiaro.trollhack.util.graphics.use
import cum.xiaro.trollhack.util.threads.TrollHackScope
import cum.xiaro.trollhack.util.threads.runSafe
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper.glGenBuffers
import net.minecraft.client.renderer.culling.ICamera
import net.minecraft.entity.Entity
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.GL_TEXTURE2
import org.lwjgl.opengl.GL14.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer

internal object FastRender : Module(
    name = "FastRender",
    description = "Fps boost",
    category = Category.RENDER
) {
    var frameBuffer = TileEntityRenderFrameBuffer()
    private val renderEntryMap = HashMap<Class<out TileEntity>, RenderEntry<out TileEntity>>()

    private val fboDrawVao: Int

    init {
        fboDrawVao = glGenVertexArrays()
        val vboID = glGenBuffers()
        val buffer = GLAllocation.createDirectByteBuffer(24)

        fun putVertex(x: Byte, y: Byte, u: Byte, v: Byte) {
            buffer.put(x)
            buffer.put(y)
            buffer.put(u)
            buffer.put(v)
        }

        putVertex(-1, -1, 0, 0)
        putVertex(1, -1, 1, 0)
        putVertex(-1, 1, 0, 1)

        putVertex(1, 1, 1, 1)
        putVertex(-1, 1, 0, 1)
        putVertex(1, -1, 1, 0)

        buffer.flip()

        glBindBuffer(GL_ARRAY_BUFFER, vboID)
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STREAM_DRAW)

        glBindVertexArray(fboDrawVao)
        glVertexAttribPointer(0, 2, GL_BYTE, false, 4, 0L)
        glVertexAttribPointer(1, 2, GL_UNSIGNED_BYTE, false, 4, 2L)

        glEnableVertexAttribArray(0)
        glEnableVertexAttribArray(1)

        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)

        register {
            BedRenderBuilder(mc.renderManager.renderPosX, mc.renderManager.renderPosY, mc.renderManager.renderPosZ)
        }
        register {
            ChestRenderBuilder(mc.renderManager.renderPosX, mc.renderManager.renderPosY, mc.renderManager.renderPosZ)
        }
        register {
            EnderChestRenderBuilder(mc.renderManager.renderPosX, mc.renderManager.renderPosY, mc.renderManager.renderPosZ)
        }
        register {
            ShulkerBoxRenderBuilder(mc.renderManager.renderPosX, mc.renderManager.renderPosY, mc.renderManager.renderPosZ)
        }
    }

    private inline fun <reified T : TileEntity> register(noinline newBuilder: () -> ITileEntityRenderBuilder<T>) {
        renderEntryMap[T::class.java] = RenderEntry(newBuilder)
    }

    init {
        onEnable {
            updateFrameBuffer(Resolution.widthI, Resolution.heightI)
        }

        listener<ResolutionUpdateEvent> {
            updateFrameBuffer(it.width, it.height)
        }

        listener<TickEvent.Post> {
            renderEntryMap.values.forEach {
                it.clear()
            }

            runSafe {
                mc.world.loadedTileEntityList.forEach { tileEntity ->
                    renderEntryMap[tileEntity.javaClass]?.let {
                        @Suppress("UNCHECKED_CAST")
                        (it as RenderEntry<TileEntity>?)?.add(tileEntity)
                    }
                }
            }

            updateRenderers()
        }
    }

    private fun updateFrameBuffer(width: Int, height: Int) {
        frameBuffer.destroy()
        frameBuffer = TileEntityRenderFrameBuffer()
        frameBuffer.use {
            glClearDepth(1.0)
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

            allocateFrameBuffer(width, height)
        }
    }

    @JvmStatic
    fun renderTileEntities() {
        AbstractTileEntityRenderBuilder.Shader.updateShaders()
        GlStateManager.disableCull()

        frameBuffer.use {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            renderEntryMap.values.forEach {
                it.render()
            }
        }

        glBindVertexArray(fboDrawVao)

        FboDraw.use {
            mc.framebuffer.bindFramebuffer(false)

            GlStateManager.setActiveTexture(GL_TEXTURE0)
            frameBuffer.bindTexture()
            GlStateManager.setActiveTexture(GL_TEXTURE2)
            frameBuffer.bindDepthTexture()

            glDrawArrays(GL_TRIANGLES, 0, 6)

            GlStateManager.setActiveTexture(GL_TEXTURE2)
            frameBuffer.unbindDepthTexture()
            GlStateManager.setActiveTexture(GL_TEXTURE0)
            frameBuffer.unbindTexture()
        }

        glBindVertexArray(0)
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    fun updateRenderers() {
        runBlocking {
            val actor = actor<Pair<RenderEntry<*>, ITileEntityRenderBuilder<*>>> {
                for ((entry, builder) in channel) {
                    entry.updateRenderer(builder.upload())
                }
            }

            coroutineScope {
                for (entry in renderEntryMap.values) {
                    entry.update(this, actor)
                }
            }

            actor.close()
        }
    }

    private class RenderEntry<T : TileEntity>(
        private val newBuilder: () -> ITileEntityRenderBuilder<T>
    ) {
        init {
            newBuilder.invoke()
        }

        private var renderer: ITileEntityRenderBuilder.Renderer? = null
        private val tileEntities = ArrayList<T>()
        private var dirty = false

        fun clear() {
            if (tileEntities.isNotEmpty()) {
                tileEntities.clear()
                dirty = true
            }
        }

        fun add(tileEntity: T) {
            tileEntities.add(tileEntity)
            dirty = true
        }

        fun remove(tileEntity: T) {
            dirty = tileEntities.remove(tileEntity) || dirty
        }

        fun update(scope: CoroutineScope, actor: SendChannel<Pair<RenderEntry<*>, ITileEntityRenderBuilder<*>>>) {
            if (!dirty) return

            if (tileEntities.isEmpty()) {
                updateRenderer(null)
            } else {
                scope.launch(TrollHackScope.context) {
                    val builder = newBuilder.invoke()

                    tileEntities.forEach {
                        builder.add(it)
                    }

                    builder.build()

                    actor.send(Pair(this@RenderEntry, builder))
                }
            }
        }

        fun updateRenderer(newRenderer: ITileEntityRenderBuilder.Renderer?) {
            renderer?.destroy()
            renderer = newRenderer
            dirty = false
        }

        fun render() {
            renderer?.render(mc.renderManager.renderPosX, mc.renderManager.renderPosY, mc.renderManager.renderPosZ)
        }
    }

    class TileEntityRenderFrameBuffer : FrameBuffer() {
        private val depthTextureID = glGenTextures()

        override fun allocateFrameBuffer(width: Int, height: Int) {
            super.allocateFrameBuffer(width, height)

            GlStateManager.bindTexture(depthTextureID)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, width, height, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, null as ByteBuffer?)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_NONE)
            GlStateManager.bindTexture(0)

            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextureID, 0)
        }

        fun bindDepthTexture() {
            GlStateManager.bindTexture(depthTextureID)
        }

        fun unbindDepthTexture() {
            GlStateManager.bindTexture(depthTextureID)
        }

        override fun destroy() {
            super.destroy()
            glDeleteTextures(depthTextureID)
        }
    }

    private object FboDraw : Shader("/assets/trollhack/shaders/general/FboDraw.vsh", "/assets/trollhack/shaders/general/FboDraw.fsh") {
        init {
            use {
                glUniform1i(glGetUniformLocation(id, "texture"), 0)
                glUniform1i(glGetUniformLocation(id, "depthTexture"), 2)
            }
        }
    }
}
