package dev.luna5ama.trollhack

import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import dev.fastmc.common.wrapDirectByteBuffer
import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.impl.LoopEvent
import dev.luna5ama.trollhack.event.impl.render.*
import dev.luna5ama.trollhack.manager.managers.TextureManager
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.Helper
import dev.luna5ama.trollhack.utils.ResourceHelper
import dev.luna5ama.trollhack.utils.compat.bindWrite
import dev.luna5ama.trollhack.utils.compat.frameBufferId
import dev.luna5ama.trollhack.graphics.*
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects.draw
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PersistentMappedVBO
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.matrix.*
import dev.luna5ama.trollhack.graphics.texture.ArrayTexture
import dev.luna5ama.trollhack.graphics.texture.ResizableFramebuffer
import dev.luna5ama.trollhack.graphics.texture.useTexture
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.state.GuiRenderState
import org.joml.Matrix4f
import org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor
import org.lwjgl.glfw.GLFW.glfwGetVideoModes
import org.lwjgl.glfw.GLFW.glfwSetErrorCallback
import org.lwjgl.opengl.*
import org.lwjgl.opengl.ARBImaging.GL_TABLE_TOO_LARGE
import org.lwjgl.opengl.GL14.glBlendFuncSeparate
import org.lwjgl.opengl.GL43.*
import org.lwjgl.opengl.GL45.GL_CONTEXT_LOST
import org.lwjgl.system.MemoryUtil
import java.util.concurrent.LinkedBlockingQueue
import javax.imageio.ImageIO
import kotlin.properties.Delegates

typealias RS = dev.luna5ama.trollhack.RenderSystem

object RenderSystem : AlwaysListening, Helper {
    val compatibility = GLCompatibility(
        runCatching { GL.getCapabilities() }.getOrElse { GL.createCapabilities() },
        runCatching { GL.getCapabilitiesWGL() }.getOrNull(),
        runCatching { GL.getCapabilitiesGLX() }.getOrNull()
    )
    var refreshRate = 0; private set
    var forwardCompatibility by Delegates.notNull<Boolean>()

    val render2DTasks = LinkedBlockingQueue<(Float) -> Unit>()
    val render3DTasks = LinkedBlockingQueue<(Float) -> Unit>()

    val useFramebuffer get() = false

    lateinit var framebuffer: ResizableFramebuffer
    lateinit var defaultRenderLayer: ResizableFramebuffer.ResizableColorLayer

    val renderScaleF get() = mc.window.guiScale.toFloat()
    val renderScale get() = mc.window.guiScale
    val width get() = if (useFramebuffer) framebuffer.width else mc.window.width
    val height get() = if (useFramebuffer) framebuffer.height else mc.window.height
    val widthF get() = if (useFramebuffer) framebuffer.width.toFloat() else mc.window.width.toFloat()
    val heightF get() = if (useFramebuffer) framebuffer.height.toFloat() else mc.window.height.toFloat()
    val widthD get() = if (useFramebuffer) framebuffer.width.toDouble() else mc.window.width.toDouble()
    val heightD get() = if (useFramebuffer) framebuffer.height.toDouble() else mc.window.height.toDouble()
    val scaledWidth get() = widthD / renderScale
    val scaledHeight get() = heightD / renderScale
    val scaledWidthF get() = widthF / renderScaleF
    val scaledHeightF get() = heightF / renderScaleF
    val mouseX get() = mc.mouseHandler.xpos() / renderScale
    val mouseXF get() = mouseX.toFloat()
    val mouseY get() = mc.mouseHandler.ypos() / renderScale
    val mouseYF get() = mouseY.toFloat()

    val particleSystem by lazy {
        ParticleSystem(
            speed = 0.1f,
            initialColor = ColorRGBA(164, 255, 255).alpha(128)
        )
    }

    val modelView: Matrix4f = Matrix4f()
    val projection: Matrix4f = Matrix4f()
    val matrixLayer = MatrixLayerStack()

    private val scissors by lazy { GuiGraphics(mc, GuiRenderState(), width, height) } // don't use other methods
    var msaaLevel = ClientSettings.msaaSamples

    val arrayTexture: ArrayTexture
    val currentLayer by BacksideFrameCounter(120, 30)

    fun coerceLineWidth(width: Float): Float {
        return width.coerceIn(Float.MIN_VALUE, if (forwardCompatibility) 1.0f else width)
    }

    init {
        System.setProperty("joml.forceUnsafe", "true")
        System.setProperty("joml.fastmath", "true")
        System.setProperty("joml.sinLookup", "true")
        System.setProperty("joml.format", "false")
        System.setProperty("joml.useMathFma", "true")

        initDebugCallbacks()
        TrollHackMod.LOGGER.info("MAX_TEXTURE_ARRAY_LAYERS=${compatibility.maxArrayTextureLayers}")
        TrollHackMod.LOGGER.info("MAX_COMPUTE_WORK_GROUP_SIZE=${compatibility.maxComputeWorkGroupSize}")

        val images = buildList {
            repeat(120) {
                val id = "%04d".format(it)
                add(
                    ImageIO.read(
                        ResourceHelper.getResourceStream("/assets/trollhack/alien_dance/alien_dance-$id.png")
                    )
                )
            }
        }
        arrayTexture = ArrayTexture(images)

        handler<LoopEvent.Render> {
            initDebugCallbacks()
            PMVBObjects.onSync()
            PersistentMappedVBO.onSync()
            msaaLevel = ClientSettings.msaaSamples
            framebuffer.msaaLevel = msaaLevel
            mc.mainRenderTarget.bindWrite(false)
        }

        handler<ResolutionUpdateEvent> { event ->
            if (!::framebuffer.isInitialized) return@handler
            framebuffer.resize(event.framebufferWidth, event.framebufferHeight)
        }
    }

    fun init() {
        initDebugCallbacks()
        if (!compatibility.openGL45) {
            require(compatibility.arbDirectStateAccess && compatibility.arbDebugOutput)
            TrollHackMod.LOGGER.warn("OpenGL 4.5 is not available. Trying to use ARB extensions.")
        }
        forwardCompatibility =
            (glGetInteger(GL_CONTEXT_FLAGS) and GL_CONTEXT_FLAG_FORWARD_COMPATIBLE_BIT) == GL_CONTEXT_FLAG_FORWARD_COMPATIBLE_BIT
        framebuffer = ResizableFramebuffer(mc.window.width, mc.window.width, true, msaaLevel)
        defaultRenderLayer = framebuffer.generateColorLayer()
        mc.mainRenderTarget.bindWrite(false)

        TextureManager.resume()

        val monitor = glfwGetPrimaryMonitor()
        val mode = glfwGetVideoModes(monitor)!!.last()
        refreshRate = mode.refreshRate()
    }

    fun getCurrentMaxFps(original: Int): Int {
        return original
    }

    private var lastDepthMask = false

    fun preRender() {
        GLHelper.unbindProgram()
        GLHelper.bindFBO = -1
        GLHelper.bindVAO = -1
        if (useFramebuffer) {
            framebuffer.bindFramebuffer()
            glClear(GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
            OpenGLWrapper.glBlitNamedFramebuffer(
                mc.mainRenderTarget.frameBufferId, framebuffer.fbo,
                0, 0, framebuffer.width, framebuffer.height,
                0, 0, framebuffer.width, framebuffer.height,
                GL_COLOR_BUFFER_BIT, GL_NEAREST
            )
            defaultRenderLayer.bindLayer()
            glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
        } else {
            mc.mainRenderTarget.bindWrite(false)
            glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
        }
        glActiveTexture(GL_TEXTURE0)
        lastDepthMask = glGetBoolean(GL_DEPTH_WRITEMASK)
    }

    fun postRender() {
        GLHelper.blend = true
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        if (useFramebuffer) {
            GLHelper.bindFramebuffer(mc.mainRenderTarget.frameBufferId, true)
            glDrawBuffer(GL_COLOR_ATTACHMENT0)
            OpenGLWrapper.glBlitNamedFramebuffer(
                framebuffer.fbo, mc.mainRenderTarget.frameBufferId,
                0, 0, framebuffer.width, framebuffer.height,
                0, 0, framebuffer.width, framebuffer.height,
                GL_COLOR_BUFFER_BIT, GL_NEAREST
            )
            glBindFramebuffer(GL_READ_FRAMEBUFFER, 0)
        }
        glDepthMask(lastDepthMask)
        glDisable(GL_SCISSOR_TEST)
        glDisable(GL_STENCIL_TEST)
        glDisable(GL_PRIMITIVE_RESTART)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
        GLHelper.bindVertexArray(0, true)
        GLHelper.cull = false
        GLHelper.blend = true
        GLHelper.depth = true
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        mc.mainRenderTarget.bindWrite(false)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, GlStateManager.TEXTURES[0].binding)

        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, GlStateManager.TEXTURES[1].binding)

        glActiveTexture(GL_TEXTURE2)
        glBindTexture(GL_TEXTURE_2D, GlStateManager.TEXTURES[2].binding)

        glActiveTexture(GL_TEXTURE3)
        glBindTexture(GL_TEXTURE_2D, GlStateManager.TEXTURES[3].binding)

        glActiveTexture(org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE))
    }

    fun pushScissor(x: Int, y: Int, x1: Int, y1: Int) {
        scissors.enableScissor(x, y, x1, y1)
    }

    fun popScissor() {
        scissors.disableScissor()
    }

    fun applyScissor(x: Int, y: Int, x1: Int, y1: Int, func: () -> Unit) {
        pushScissor(x, y, x1, y1)
        func()
        popScissor()
    }

    private fun drawAlien() {
        val layer = currentLayer
        arrayTexture.useTexture {
            GL_TRIANGLE_STRIP.draw(PMVBObjects.VertexMode.Pos2fColorTexArray) {
                vertex(100f, 0f, ColorRGBA.WHITE, 1f, 0f, layer)
                vertex(0f, 0f, ColorRGBA.WHITE, 0f, 0f, layer)
                vertex(100f, 100f, ColorRGBA.WHITE, 1f, 1f, layer)
                vertex(0f, 100f, ColorRGBA.WHITE, 0f, 1f, layer)
            }
        }
    }

    fun render2D(context: GuiGraphics, ticksDelta: Float) {
        preRender()

        GLHelper.blend = true
        GLHelper.cull = false
        GLHelper.depth = false

        matrixLayer.scope {
            modelView.identity()
            projection.set(Matrix4f().setOrtho(0.0f, scaledWidthF, scaledHeightF, 0.0f, -1000.0f, 1000.0f))
            matrixLayer.checkID++

            if (ClientSettings.alien) drawAlien()

//            UnicodeFontManager.CURRENT_FONT.drawString(
//                "${model.verticesCount * 1000} vertices, ${model.primitivesCount * 1000} primitives", ColorRGBA.WHITE
//            )

            Render2DEvent(context, ticksDelta).post()
            CoreRender2DEvent(ticksDelta).post()

            while (true) {
                val poll = render2DTasks.poll()
                if (poll != null) poll.invoke(ticksDelta)
                else break
            }
        }

        GLHelper.depth = true

        postRender()
    }

    fun render3D(matrixStack: PoseStack, ticksDelta: Float) {
        preRender()

        GLHelper.blend = true
        GLHelper.cull = true
        GLHelper.depth = true

        matrixLayer.scope {
            val camera = mc.gameRenderer.mainCamera

            modelView.set(Matrix4f(RenderSystem.getModelViewMatrix()))
            projection.set(mc.gameRenderer.getProjectionMatrix(ticksDelta))

            mulPosition(matrixStack.last().pose())
//            rotate(RotationAxis.POSITIVE_X.rotationDegrees(camera.pitch))
//            rotate(RotationAxis.POSITIVE_Y.rotationDegrees(camera.yaw + 180))

            val pos = camera.position()
            translatef((-pos.x).toFloat(), (-pos.y).toFloat(), (-pos.z).toFloat())

//            matrixLayer.scope {
//                translatef(0f, -50f, 0f)
//                model.drawModel(this@scope)
//            }

            Render3DEvent(matrixStack, ticksDelta).post()
            CoreRender3DEvent(ticksDelta).post()

            while (true) {
                val poll = render3DTasks.poll()
                if (poll != null) poll.invoke(ticksDelta)
                else break
            }
        }

        postRender()
    }

    fun initDebugCallbacks() {
        if (!RenderSystem.isOnRenderThread()) return
        OpenGLWrapper.glDebugMessageControl(
            GL_DONT_CARE,
            GL_DONT_CARE,
            GL_DONT_CARE,
            null as IntArray?,
            true
        )
        OpenGLWrapper.glDebugMessageControl(
            GL_DONT_CARE,
            GL_DONT_CARE,
            GL_DEBUG_SEVERITY_NOTIFICATION,
            null as IntArray?,
            false
        )
        OpenGLWrapper.glDebugMessageControl(
            GL_DONT_CARE,
            GL_DONT_CARE,
            GL_DEBUG_SEVERITY_LOW,
            null as IntArray?,
            false
        )

        glfwSetErrorCallback { error, description ->
            if (ClientSettings.glDebugStacktrace) GLMessageException().printStackTrace()
            val glErrorString = when (error) {
                GL_INVALID_ENUM -> "GL_INVALID_ENUM"
                GL_INVALID_VALUE -> "GL_INVALID_VALUE"
                GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
                GL_STACK_OVERFLOW -> "GL_STACK_OVERFLOW"
                GL_STACK_UNDERFLOW -> "GL_STACK_UNDERFLOW"
                GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
                GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
                GL_CONTEXT_LOST -> "GL_CONTEXT_LOST"
                GL_TABLE_TOO_LARGE -> "GL_TABLE_TOO_LARGE1"
                else -> "GL_UNKNOWN_ERROR"
            }

            if (ClientSettings.glDebugVerbose) TrollHackMod.LOGGER.error(
                "GLError $glErrorString: ${
                    MemoryUtil.memUTF8(
                        description
                    )
                }"
            )
            else TrollHackMod.LOGGER.debug("GLError $error: ${MemoryUtil.memUTF8(description)}")
        }

        OpenGLWrapper.glDebugMessageCallback({ source, type, id, severity, length, message, _ ->
            val wrappedBuffer = wrapDirectByteBuffer(message, length)
            val byteArray = ByteArray(length)
            wrappedBuffer.get(byteArray)
            val sourceStr = when (source) {
                GL_DEBUG_SOURCE_API -> "API"
                GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "WINDOW_SYSTEM"
                GL_DEBUG_SOURCE_SHADER_COMPILER -> "SHADER_COMPILER"
                GL_DEBUG_SOURCE_THIRD_PARTY -> "THIRD_PARTY"
                GL_DEBUG_SOURCE_APPLICATION -> "APPLICATION"
                GL_DEBUG_SOURCE_OTHER -> "OTHER"
                else -> "UNKNOWN"
            }
            val typeStr = when (type) {
                GL_DEBUG_TYPE_ERROR -> "ERROR"
                GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "DEPRECATED_BEHAVIOR"
                GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "UNDEFINED_BEHAVIOR"
                GL_DEBUG_TYPE_PORTABILITY -> "PORTABILITY"
                GL_DEBUG_TYPE_PERFORMANCE -> "PERFORMANCE"
                GL_DEBUG_TYPE_MARKER -> "MARKER"
                GL_DEBUG_TYPE_PUSH_GROUP -> "PUSH_GROUP"
                GL_DEBUG_TYPE_POP_GROUP -> "POP_GROUP"
                GL_DEBUG_TYPE_OTHER -> "OTHER"
                else -> "UNKNOWN"
            }
            val severityStr = when (severity) {
                GL_DEBUG_SEVERITY_HIGH -> "HIGH"
                GL_DEBUG_SEVERITY_MEDIUM -> "MEDIUM"
                GL_DEBUG_SEVERITY_LOW -> "LOW"
                GL_DEBUG_SEVERITY_NOTIFICATION -> "NOTIFICATION"
                else -> "UNKNOWN"
            }

            var stacktrace = ClientSettings.glDebugStacktrace
            if (type == GL_DEBUG_TYPE_PUSH_GROUP) stacktrace = false
            if (type == GL_DEBUG_TYPE_POP_GROUP) stacktrace = false
            if (severity == GL_DEBUG_SEVERITY_NOTIFICATION) stacktrace = false
            if (severity == GL_DEBUG_SEVERITY_LOW) stacktrace = false
            if (severity == GL_DEBUG_SEVERITY_MEDIUM) stacktrace = false

            val log = "[OpenGL/$sourceStr][message-$id] " +
                    "type = $typeStr, severity = $severityStr, message = ${byteArray.decodeToString()}"
            if (stacktrace) GLMessageException().printStackTrace()
            if (ClientSettings.glDebugVerbose) TrollHackMod.LOGGER.warn(log)
            else TrollHackMod.LOGGER.debug(log)
        }, 0)
    }

    class GLMessageException : Exception()

    fun render2D(job: (Float) -> Unit) = render2DTasks.put(job)
    fun render3D(job: (Float) -> Unit) = render3DTasks.put(job)
}
