package dev.luna5ama.trollhack.gui

import dev.fastmc.common.collection.FastObjectArrayList
import dev.luna5ama.trollhack.event.*
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.render.Render2DEvent
import dev.luna5ama.trollhack.graphics.*
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.graphics.shaders.ParticleShader
import dev.luna5ama.trollhack.gui.IGuiScreen.Companion.forEachWindow
import dev.luna5ama.trollhack.gui.rgui.MouseState
import dev.luna5ama.trollhack.gui.rgui.WindowComponent
import dev.luna5ama.trollhack.gui.rgui.windows.ListWindow
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.accessor.listShaders
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import dev.luna5ama.trollhack.util.state.TimedFlag
import dev.luna5ama.trollhack.util.threads.runSynchronized
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.glUseProgram
import org.lwjgl.opengl.GL32.GL_DEPTH_CLAMP

abstract class AbstractTrollGui : GuiScreen(), IListenerOwner by ListenerOwner(), IGuiScreen {
    override val isVisible: Boolean get() = mc.currentScreen === this || displayed.value

    override var mouseState: MouseState = MouseState.NONE
    override val mousePos: Vec2f get() = Companion.mousePos

    open val alwaysTicking = false

    // Window
    override val windows = ObjectLinkedOpenHashSet<WindowComponent>()
    override val windowsCachedList = FastObjectArrayList<WindowComponent>()

    override var lastClicked: WindowComponent? = null
    override var hovered: WindowComponent? = null
        get() {
            if (mouseState != MouseState.NONE) {
                return field
            }

            val value = windows.lastOrNull { it.isInWindow(mousePos) }
            if (value != field) {
                field?.onLeave(mousePos)
                value?.onHover(mousePos)
                field = value
            }

            return value
        }

    // Mouse
    private var lastEventButton = -1
    private var lastClickPos = Vec2f.ZERO
        set(value) {
            field = value
            lastClickTime = System.currentTimeMillis()
        }
    private var lastClickTime = 0L

    // Searching
    open var searchString = ""
        set(value) {
            renderStringPosX.update(MainFontRenderer.getWidth(value, 2.0f))
            field = value
        }
    private val renderStringPosX = AnimationFlag(Easing.OUT_CUBIC, 250.0f)
    val searching
        get() = searchString.isNotEmpty()

    // Shader
    private val blurShader = ShaderHelper(ResourceLocation("shaders/post/kawase_blur_6.json"))

    // Animations
    private var displayed = TimedFlag(false)
    private val fadeMultiplier
        get() = if (displayed.value) {
            if (GuiSetting.fadeInTime > 0.0f) {
                Easing.OUT_CUBIC.inc(Easing.toDelta(displayed.lastUpdateTime, GuiSetting.fadeInTime * 1000.0f))
            } else {
                1.0f
            }
        } else {
            if (GuiSetting.fadeOutTime > 0.0f) {
                Easing.OUT_CUBIC.dec(Easing.toDelta(displayed.lastUpdateTime, GuiSetting.fadeOutTime * 1000.0f))
            } else {
                0.0f
            }
        }

    init {
        mc = Wrapper.minecraft

        safeParallelListener<TickEvent.Pre> {
            blurShader.shader?.let {
                val multiplier = GuiSetting.backGroundBlur * fadeMultiplier
                for (shader in it.listShaders) {
                    shader.shaderManager.getShaderUniform("multiplier")?.set(multiplier)
                }
            }

            if (displayed.value || alwaysTicking) {
                coroutineScope {
                    forEachWindow {
                        launch {
                            it.onTick()
                        }
                    }
                }
            }
        }

        safeListener<Render2DEvent.Mc>(-69420) {
            if (!displayed.value && fadeMultiplier > 0.0f) {
                drawScreen(0, 0, mc.renderPartialTicks)
            }
        }
    }

    // Gui init
    open fun onDisplayed() {
        searchString = ""
        displayed.value = true

        forEachWindow {
            it.onGuiDisplayed()
        }
    }

    override fun initGui() {
        super.initGui()

        val scaledResolution = ScaledResolution(mc)
        width = scaledResolution.scaledWidth + 16
        height = scaledResolution.scaledHeight + 16
    }

    override fun onGuiClosed() {
        lastClicked = null
        hovered = null

        searchString = ""
        renderStringPosX.forceUpdate(0.0f)

        displayed.value = false

        forEachWindow {
            it.onGuiClosed()
        }
    }
    // End of gui init


    // Mouse input
    override fun handleMouseInput() {
        Companion.mousePos = calcMousePos(Mouse.getEventX(), Mouse.getEventY())
        val eventButton = Mouse.getEventButton()

        when {
            // Click
            Mouse.getEventButtonState() -> {
                lastClickPos = mousePos
                lastEventButton = eventButton
            }
            // Release
            eventButton != -1 -> {
                lastEventButton = -1
            }
            // Drag
            lastEventButton != -1 -> {

            }
        }

        hovered?.onMouseInput(mousePos)
        super.handleMouseInput()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        mouseState = MouseState.CLICK

        hovered?.onClick(lastClickPos, mouseButton)
        lastClicked = hovered

        lastClicked?.let {
            windows.runSynchronized { addAndMoveToLast(it) }
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        hovered?.onRelease(mousePos, lastClickPos, state)

        mouseState = MouseState.NONE

        lastClicked?.let {
            windows.runSynchronized { addAndMoveToLast(it) }
        }
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if ((mousePos - lastClickPos).length() < 4.0f || System.currentTimeMillis() - lastClickTime < 50L) return

        mouseState = MouseState.DRAG

        hovered?.onDrag(mousePos, lastClickPos, clickedMouseButton)

        lastClicked?.let {
            windows.runSynchronized { addAndMoveToLast(it) }
        }
    }
    // End of mouse input

    // Keyboard input
    override fun handleKeyboardInput() {
        super.handleKeyboardInput()
        val keyCode = Keyboard.getEventKey()
        val keyState = Keyboard.getEventKeyState()

        lastClicked?.onKeyInput(keyCode, keyState)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        val lastClicked = lastClicked
        if (lastClicked is ListWindow && lastClicked.keybordListening != null) return

        when {
            keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE -> {
                searchString = ""
            }
            typedChar.isLetter() || typedChar == ' ' -> {
                searchString += typedChar
            }
        }
    }
    // End of keyboard input

    // Rendering
    final override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        handleInput()

        mc.profiler.startSection("trollGui")

        mc.profiler.startSection("pre")
        GlStateUtils.alpha(false)
        GlStateUtils.depth(false)
        glEnable(GL_DEPTH_CLAMP)
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE)

        val scaledResolution = ScaledResolution(mc)
        val multiplier = fadeMultiplier

        mc.profiler.endStartSection("backGround")
        GlStateUtils.rescaleActual()
        drawBackground(partialTicks)

        mc.profiler.endStartSection("windows")
        GlStateUtils.rescaleTroll()
        GlStateManager.translate(0.0f, -(Resolution.trollHeightF * (1.0f - multiplier)), 0.0f)
        drawWindows()
        drawTypedString()

        mc.profiler.endStartSection("post")
        GlStateUtils.rescaleMc()
        GlStateManager.translate(0.0f, -(scaledResolution.scaledHeight * (1.0f - multiplier)), 0.0f)

        glDisable(GL_DEPTH_CLAMP)
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        GlStateUtils.alpha(true)
        mc.profiler.endSection()

        mc.profiler.endSection()
        glUseProgram(0)
    }

    private fun drawBackground(partialTicks: Float) {
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE)
        GlStateManager.colorMask(false, false, false, true)
        RenderUtils2D.drawRectFilled(Resolution.widthF, Resolution.heightF, ColorRGB(0, 0, 0, 255))
        GlStateManager.colorMask(true, true, true, true)

        // Blur effect
        if (GuiSetting.backGroundBlur > 0.0f) {
            GlStateManager.pushMatrix()
            glUseProgram(0)
            blurShader.shader?.render(partialTicks)
            mc.framebuffer.bindFramebuffer(true)
            blurShader.getFrameBuffer("final")?.framebufferRenderExt(mc.displayWidth, mc.displayHeight, false)
            GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE)
            GlStateManager.popMatrix()
        }

        // Darkened background
        if (GuiSetting.darkness > 0.0f) {
            val color = ColorRGB(0, 0, 0, (GuiSetting.darkness * 255.0f * fadeMultiplier).toInt())
            RenderUtils2D.drawRectFilled(Resolution.widthF, Resolution.heightF, color)
        }

        if (GuiSetting.particle) {
            GlStateUtils.blend(true)
            ParticleShader.render()
        }
    }

    private fun drawWindows() {
        mc.profiler.startSection("pre")
        drawEachWindow {
            it.onRender(Vec2f(it.renderPosX, it.renderPosY))
        }

        mc.profiler.endStartSection("post")
        drawEachWindow {
            it.onPostRender(Vec2f(it.renderPosX, it.renderPosY))
        }

        mc.profiler.endSection()
    }

    private inline fun drawEachWindow(crossinline renderBlock: (WindowComponent) -> Unit) {
        forEachWindow {
            if (!it.visible) return@forEachWindow
            GlStateManager.pushMatrix()
            GlStateManager.translate(it.renderPosX, it.renderPosY, 0.0f)
            renderBlock(it)
            GlStateManager.popMatrix()
        }
    }

    private fun drawTypedString() {
        if (searchString.isNotBlank() && System.currentTimeMillis() - renderStringPosX.time <= 5000L) {
            val posX = Resolution.trollWidthF / 2.0f - renderStringPosX.get() / 2.0f
            val posY = Resolution.trollHeightF / 2.0f - MainFontRenderer.getHeight(2.0f) / 2.0f
            var color = GuiSetting.text
            color =
                color.alpha(Easing.IN_CUBIC.dec(Easing.toDelta(renderStringPosX.time, 5000.0f), 0.0f, 255.0f).toInt())
            MainFontRenderer.drawString(searchString, posX, posY, color, 2.0f)
        }
    }
    // End of rendering

    override fun doesGuiPauseGame(): Boolean {
        return false
    }

    private companion object : AlwaysListening {
        var mousePos = calcMousePos(Mouse.getX(), Mouse.getY())

        init {
            listener<RunGameLoopEvent.Tick> {
                mousePos = calcMousePos(Mouse.getX(), Mouse.getY())
            }
        }

        fun calcMousePos(x: Int, y: Int): Vec2f {
            val scaleFactor = GuiSetting.scaleFactor
            return Vec2f(
                x / scaleFactor - 1.0f,
                (Wrapper.minecraft.displayHeight - 1 - y) / scaleFactor
            )
        }
    }
}