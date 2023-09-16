package dev.luna5ama.trollhack.gui.rgui.component

import dev.luna5ama.trollhack.graphics.AnimationFlag
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.graphics.RenderUtils2D
import dev.luna5ama.trollhack.graphics.Resolution
import dev.luna5ama.trollhack.graphics.font.TextComponent
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.gui.rgui.InteractiveComponent
import dev.luna5ama.trollhack.gui.rgui.MouseState
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.module.modules.client.Tooltips
import dev.luna5ama.trollhack.util.delegate.FrameFloat
import dev.luna5ama.trollhack.util.math.vector.Vec2d
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11.*

open class Slider(
    screen: IGuiScreen,
    name: CharSequence,
    private val description: CharSequence = "",
    private val visibility: (() -> Boolean)?
) : InteractiveComponent(screen, name, UiSettingGroup.NONE) {

    override var posY: Float
        get() = if (!visible) super.posY + 100.0f else super.posY
        set(value) {
            super.posY = value
        }

    protected var inputField = ""

    protected open val progress get() = 0.0f

    protected val renderProgress = AnimationFlag { time, prev, current ->
        Easing.OUT_QUART.incOrDec(Easing.toDelta(time, 300.0f), prev.coerceIn(0.0f, 1.0f), current.coerceIn(0.0f, 1.0f))
    }

    private val minWidth0 = FrameFloat {
        MainFontRenderer.getWidth(name) + 20.0f + protectedWidth
    }
    override val minWidth by minWidth0

    private val maxHeight0 = FrameFloat {
        MainFontRenderer.getHeight() + 3.0f
    }
    override val maxHeight by maxHeight0

    protected var protectedWidth = 0.0f

    private val displayDescription = TextComponent(" ")
    private var descriptionPosX = 0.0f
    private var shown = false

    var listening = false; protected set

    override fun onClosed() {
        super.onClosed()
        onStopListening(false)
    }

    override fun onDisplayed() {
        height = maxHeight
        if (visibility != null) visible = visibility.invoke()
        super.onDisplayed()
        renderProgress.forceUpdate(0.0f, 0.0f)
        setupDescription()

        maxHeight0.updateLazy()
        minWidth0.updateLazy()
    }

    open fun onStopListening(success: Boolean) {
        listening = false
    }

    private fun setupDescription() {
        displayDescription.clear()
        if (description.isBlank()) return

        val stringBuilder = StringBuilder()
        val spaceWidth = MainFontRenderer.getWidth(" ")
        var lineWidth = -spaceWidth

        for (string in description.split(' ')) {
            val wordWidth = MainFontRenderer.getWidth(string) + spaceWidth
            val newWidth = lineWidth + wordWidth

            lineWidth = if (newWidth > 169.0f) {
                displayDescription.addLine(stringBuilder.toString())
                stringBuilder.clear()
                -spaceWidth + wordWidth
            } else {
                newWidth
            }

            stringBuilder.append(string)
            stringBuilder.append(' ')
        }

        if (stringBuilder.isNotEmpty()) displayDescription.addLine(stringBuilder.toString())
    }

    override fun onTick() {
        super.onTick()
        height = maxHeight
        if (visibility != null) visible = visibility.invoke()
        if (!visible) {
            renderProgress.forceUpdate(0.0f, 0.0f)
        }
    }

    override fun onRender(absolutePos: Vec2f) {
        // Slider bar
        val progress = renderProgress.getAndUpdate(progress)
        if (progress > 0.0f) {
            RenderUtils2D.drawRectFilled(0.0f, 0.0f, renderWidth * progress, renderHeight, GuiSetting.primary)
        }

        // Slider hover overlay
        val overlayColor = getStateColor(prevState).mix(
            getStateColor(mouseState),
            Easing.OUT_EXPO.inc(Easing.toDelta(lastStateUpdateTime, 300.0f))
        )
        RenderUtils2D.drawRectFilled(0.0f, 0.0f, renderWidth, renderHeight, overlayColor)

        // Slider name
        val displayText = inputField.takeIf { listening } ?: name
        val prev = if (prevState == MouseState.NONE) 0.0f else 1.0f
        val curr = if (mouseState == MouseState.NONE) 0.0f else 1.0f
        val scale = Easing.OUT_BACK.incOrDec(Easing.toDelta(lastStateUpdateTime, 300.0f), prev, curr)

        val prevClicked = if (prevState == MouseState.CLICK || prevState == MouseState.DRAG) 1.0f else 0.0f
        val currClicked = if (mouseState == MouseState.CLICK || mouseState == MouseState.DRAG) 1.0f else 0.0f
        val clickedScale =
            Easing.OUT_CUBIC.incOrDec(Easing.toDelta(lastStateUpdateTime, 300.0f), prevClicked, currClicked)

        MainFontRenderer.drawString(
            displayText,
            2.0f + 2.0f * scale,
            1.0f - 0.025f * scale * MainFontRenderer.getHeight() + 0.05f * clickedScale * MainFontRenderer.getHeight(),
            color = GuiSetting.text,
            scale = 1.0f + 0.05f * scale - 0.1f * clickedScale
        )
    }

    override fun onPostRender(absolutePos: Vec2f) {
        if (Tooltips.isDisabled || description.isEmpty()) return

        var deltaTime = Easing.toDelta(lastStateUpdateTime)

        if (!(mouseState == MouseState.HOVER && deltaTime > 500L || prevState == MouseState.HOVER && shown)) return

        if (mouseState == MouseState.HOVER) {
            if (descriptionPosX == 0.0f) descriptionPosX = lastMousePos.x
            deltaTime -= 500L
            shown = true
        } else if (deltaTime > 250.0f) {
            descriptionPosX = 0.0f
            shown = false
            return
        }

        val alpha = if (mouseState == MouseState.HOVER) Easing.OUT_CUBIC.inc(deltaTime / 250.0f)
        else Easing.OUT_CUBIC.dec(deltaTime / 250.0f)

        val textWidth = displayDescription.getWidth()
        val textHeight = displayDescription.getHeight(2)

        val relativeCorner = Vec2f(Resolution.trollWidthF, Resolution.trollHeightF) - absolutePos

        val posX = descriptionPosX.coerceIn(-absolutePos.x, (relativeCorner.x - textWidth - 10.0f))
        val posY = (renderHeight + 4.0f).coerceIn(-absolutePos.y, (relativeCorner.y - textHeight - 10.0f))

        glDisable(GL_SCISSOR_TEST)
        GlStateManager.pushMatrix()
         GlStateManager.translate(posX, posY, 696.0f)

        RenderUtils2D.drawRectFilled(
            0.0f,
            0.0f,
            textWidth + 4.0f,
            textHeight + 4.0f,
            color = GuiSetting.backGround.run { alpha((a * alpha).toInt()) })
        if (GuiSetting.windowOutline) {
            RenderUtils2D.drawRectOutline(
                0.0f,
                0.0f,
                textWidth + 4.0f,
                textHeight + 4.0f,
                lineWidth = 1.0f,
                color = GuiSetting.primary.alpha((255 * alpha).toInt())
            )
        }

        displayDescription.draw(Vec2d(2.0, 2.0), 2, alpha)

        glEnable(GL_SCISSOR_TEST)
        GlStateManager.popMatrix()
    }

    private fun getStateColor(state: MouseState) = when (state) {
        MouseState.NONE -> GuiSetting.idle
        MouseState.HOVER -> GuiSetting.hover
        else -> GuiSetting.click
    }
}