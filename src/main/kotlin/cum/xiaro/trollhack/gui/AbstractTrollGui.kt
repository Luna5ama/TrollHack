package cum.xiaro.trollhack.gui

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.util.graphics.Easing
import cum.xiaro.trollhack.event.IListenerOwner
import cum.xiaro.trollhack.event.ListenerOwner
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.render.Render2DEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.event.safeParallelListener
import cum.xiaro.trollhack.gui.rgui.WindowComponent
import cum.xiaro.trollhack.gui.rgui.windows.ColorPicker
import cum.xiaro.trollhack.gui.rgui.windows.SettingWindow
import cum.xiaro.trollhack.module.modules.client.GuiSetting
import cum.xiaro.trollhack.util.Wrapper
import cum.xiaro.trollhack.util.accessor.listShaders
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.graphics.RenderUtils2D
import cum.xiaro.trollhack.util.graphics.Resolution
import cum.xiaro.trollhack.util.graphics.ShaderHelper
import cum.xiaro.trollhack.util.graphics.font.renderer.MainFontRenderer
import cum.xiaro.trollhack.util.graphics.shaders.ParticleShader
import cum.xiaro.trollhack.util.math.vector.Vec2f
import cum.xiaro.trollhack.util.state.TimedFlag
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL32.GL_DEPTH_CLAMP
import kotlin.math.min

abstract class AbstractTrollGui<S : SettingWindow<*>, E : Any> : GuiScreen(), IListenerOwner by ListenerOwner() {

    open val alwaysTicking = false

    // Window
    val windowList = LinkedHashSet<WindowComponent>()
    private var lastClickedWindow: WindowComponent? = null
    private var hoveredWindow: WindowComponent? = null
        set(value) {
            if (value == field) return
            field?.onLeave(getRealMousePos())
            value?.onHover(getRealMousePos())
            field = value
        }
    private val settingMap = HashMap<E, S>()
    protected var settingWindow: S? = null

    // Mouse
    private var lastEventButton = -1
    private var lastClickPos = Vec2f(0.0f, 0.0f)

    // Searching
    protected var typedString = ""
    protected var lastTypedTime = 0L
    protected var prevStringWidth = 0.0f
    protected var stringWidth = 0.0f
        set(value) {
            prevStringWidth = renderStringPosX
            field = value
        }
    private val renderStringPosX
        get() = Easing.OUT_CUBIC.incOrDec(Easing.toDelta(lastTypedTime, 250.0f), prevStringWidth, stringWidth)
    val searching
        get() = typedString.isNotEmpty()

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
        windowList.add(ColorPicker)

        safeParallelListener<TickEvent.Pre> {
            blurShader.shader?.let {
                val multiplier = GuiSetting.blur * fadeMultiplier
                for (shader in it.listShaders) {
                    shader.shaderManager.getShaderUniform("multiplier")?.set(multiplier)
                }
            }

            if (displayed.value || alwaysTicking) {
                coroutineScope {
                    for (window in windowList) {
                        launch {
                            window.onTick()
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

    fun displaySettingWindow(element: E) {
        val mousePos = getRealMousePos()

        settingMap.getOrPut(element) {
            newSettingWindow(element, mousePos)
        }.apply {
            lastClickedWindow = this
            settingWindow = this
            windowList.add(this)

            val screenWidth = mc.displayWidth / GuiSetting.scaleFactorFloat
            val screenHeight = mc.displayHeight / GuiSetting.scaleFactorFloat

            posX = if (mousePos.x + this.width <= screenWidth) {
                mousePos.x
            } else {
                mousePos.x - this.width
            }

            posY = min(mousePos.y, screenHeight - this.height)

            onGuiInit()
            onDisplayed()
        }
    }

    abstract fun newSettingWindow(element: E, mousePos: Vec2f): S

    // Gui init
    open fun onDisplayed() {
        displayed.value = true

        for (window in windowList) window.onDisplayed()
    }

    override fun initGui() {
        super.initGui()

        val scaledResolution = ScaledResolution(mc)
        width = scaledResolution.scaledWidth + 16
        height = scaledResolution.scaledHeight + 16

        for (window in windowList) window.onGuiInit()
    }

    override fun onGuiClosed() {
        lastClickedWindow = null
        hoveredWindow = null

        typedString = ""
        lastTypedTime = 0L

        displayed.value = false

        for (window in windowList) window.onClosed()
        updateSettingWindow()
    }
    // End of gui init


    // Mouse input
    override fun handleMouseInput() {
        val scaleFactor = GuiSetting.scaleFactorFloat
        val mousePos = Vec2f(
            Mouse.getEventX() / scaleFactor - 1.0f,
            (Wrapper.minecraft.displayHeight - 1 - Mouse.getEventY()) / scaleFactor
        )
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
            // Move
            else -> {
                hoveredWindow = windowList.lastOrNull { it.isInWindow(mousePos) }
            }
        }

        hoveredWindow?.onMouseInput(mousePos)
        super.handleMouseInput()
        updateSettingWindow()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        with(hoveredWindow) {
            this?.onClick(lastClickPos, mouseButton)
            lastClickedWindow = this
        }
        updateWindowOrder()
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        val scaleFactor = GuiSetting.scaleFactorFloat
        val mousePos = Vec2f(
            Mouse.getEventX() / scaleFactor - 1.0f,
            (Wrapper.minecraft.displayHeight - 1 - Mouse.getEventY()) / scaleFactor
        )
        hoveredWindow?.onRelease(mousePos, state)
        updateWindowOrder()
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        val scaleFactor = GuiSetting.scaleFactorFloat
        val mousePos = Vec2f(
            Mouse.getEventX() / scaleFactor - 1.0f,
            (Wrapper.minecraft.displayHeight - 1 - Mouse.getEventY()) / scaleFactor
        )
        hoveredWindow?.onDrag(mousePos, lastClickPos, clickedMouseButton)
    }

    private fun updateSettingWindow() {
        settingWindow?.let {
            if (lastClickedWindow != it && lastClickedWindow != ColorPicker) {
                it.onClosed()
                windowList.remove(it)
                settingWindow = null
            }
        }
    }

    private fun updateWindowOrder() {
        val cacheList = windowList.sortedBy { it.lastActiveTime }
        windowList.clear()
        windowList.addAll(cacheList)
    }
    // End of mouse input

    // Keyboard input
    override fun handleKeyboardInput() {
        super.handleKeyboardInput()
        val keyCode = Keyboard.getEventKey()
        val keyState = Keyboard.getEventKeyState()

        hoveredWindow?.onKeyInput(keyCode, keyState)
        if (settingWindow != hoveredWindow) settingWindow?.onKeyInput(keyCode, keyState)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (settingWindow?.listeningChild != null) return
        when {
            keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE -> {
                typedString = ""
                lastTypedTime = 0L
                stringWidth = 0.0f
                prevStringWidth = 0.0f
            }
            typedChar.isLetter() || typedChar == ' ' -> {
                typedString += typedChar
                stringWidth = MainFontRenderer.getWidth(typedString, 2.0f)
                lastTypedTime = System.currentTimeMillis()
            }
        }
    }
    // End of keyboard input

    // Rendering
    final override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        mc.profiler.startSection("trollGui")

        mc.profiler.startSection("pre")
        GlStateUtils.alpha(false)
        GlStateUtils.depth(false)
        glEnable(GL_DEPTH_CLAMP)
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE)

        val scale = GuiSetting.scaleFactorFloat
        val scaledResolution = ScaledResolution(mc)
        val multiplier = fadeMultiplier

        mc.profiler.endStartSection("backGround")
        GlStateUtils.rescaleActual()
        drawBackground(partialTicks)

        mc.profiler.endStartSection("windows")
        GlStateUtils.rescaleTroll()
        glTranslatef(0.0f, -(mc.displayHeight / scale * (1.0f - multiplier)), 0.0f)
        drawWindows()

        mc.profiler.endStartSection("post")
        GlStateUtils.rescaleMc()
        glTranslatef(0.0f, -(scaledResolution.scaledHeight * (1.0f - multiplier)), 0.0f)
        drawTypedString()

        glDisable(GL_DEPTH_CLAMP)
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        GlStateUtils.alpha(true)
        mc.profiler.endSection()

        mc.profiler.endSection()
        GlStateUtils.useProgramForce(0)
    }

    private fun drawBackground(partialTicks: Float) {
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE)
        GlStateManager.colorMask(false, false, false, true)
        RenderUtils2D.drawRectFilled(posEnd = Vec2f(Resolution.widthF, Resolution.heightF), color = ColorRGB(0, 0, 0, 255))
        GlStateManager.colorMask(true, true, true, true)

        // Blur effect
        if (GuiSetting.blur > 0.0f) {
            glPushMatrix()
            GlStateUtils.useProgramForce(0)
            blurShader.shader?.render(partialTicks)
            mc.framebuffer.bindFramebuffer(true)
            blurShader.getFrameBuffer("final")?.framebufferRenderExt(mc.displayWidth, mc.displayHeight, false)
            GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE)
            glPopMatrix()
        }

        // Darkened background
        if (GuiSetting.darkness > 0.0f) {
            val color = ColorRGB(0, 0, 0, (GuiSetting.darkness * 255.0f * fadeMultiplier).toInt())
            RenderUtils2D.drawRectFilled(posEnd = Vec2f(Resolution.widthF, Resolution.heightF), color = color)
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

    private inline fun drawEachWindow(renderBlock: (WindowComponent) -> Unit) {
        for (window in windowList) {
            if (!window.visible) continue
            glPushMatrix()
            glTranslatef(window.renderPosX, window.renderPosY, 0.0f)
            renderBlock(window)
            glPopMatrix()
        }
    }

    private fun drawTypedString() {
        if (typedString.isNotBlank() && System.currentTimeMillis() - lastTypedTime <= 5000L) {
            val scaledResolution = ScaledResolution(mc)
            val posX = scaledResolution.scaledWidth / 2.0f - renderStringPosX / 2.0f
            val posY = scaledResolution.scaledHeight / 2.0f - MainFontRenderer.getHeight(2.0f) / 2.0f
            var color = GuiSetting.text
            color = color.alpha(Easing.IN_CUBIC.dec(Easing.toDelta(lastTypedTime, 5000.0f), 0.0f, 255.0f).toInt())
            MainFontRenderer.drawString(typedString, posX, posY, color, 2.0f)
        }
    }
    // End of rendering

    override fun doesGuiPauseGame(): Boolean {
        return false
    }

    companion object {
        fun getRealMousePos(): Vec2f {
            val scaleFactor = GuiSetting.scaleFactorFloat
            return Vec2f(
                Mouse.getX() / scaleFactor - 1.0f,
                (Wrapper.minecraft.displayHeight - 1 - Mouse.getY()) / scaleFactor
            )
        }
    }
}