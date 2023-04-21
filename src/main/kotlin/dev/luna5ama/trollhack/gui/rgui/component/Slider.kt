package dev.luna5ama.trollhack.gui.rgui.component

import dev.luna5ama.trollhack.gui.rgui.InteractiveComponent
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.module.modules.client.Tooltips
import dev.luna5ama.trollhack.util.graphics.AnimationFlag
import dev.luna5ama.trollhack.util.graphics.Easing
import dev.luna5ama.trollhack.util.graphics.RenderUtils2D
import dev.luna5ama.trollhack.util.graphics.font.TextComponent
import dev.luna5ama.trollhack.util.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.util.graphics.shaders.WindowBlurShader
import dev.luna5ama.trollhack.util.math.vector.Vec2d
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import org.lwjgl.opengl.GL11.*

open class Slider(
    name: CharSequence,
    private val description: CharSequence = "",
    private val visibility: (() -> Boolean)?
) : InteractiveComponent(name, 0.0f, 0.0f, 40.0f, 10.0f, SettingGroup.NONE) {

    override var posY: Float
        get() = if (!visible) super.posY + 100.0f else super.posY
        set(value) {
            super.posY = value
        }

    override var height: Float
        get() = super.height
        set(value) {
            super.height = value
        }

    protected var inputField = ""

    protected open val progress: Float
        get() = 0.0f

    protected val renderProgress = AnimationFlag(Easing.OUT_QUART, 300.0f)

    override val maxHeight
        get() = MainFontRenderer.getHeight() + 3.0f
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
    }

    open fun onStopListening(success: Boolean) {
        listening = false
    }

    private fun setupDescription() {
        displayDescription.clear()
        if (description.isNotBlank()) {
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
    }

    override fun onTick() {
        super.onTick()
        height = maxHeight
        if (visibility != null) visible = visibility.invoke()
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
            Easing.OUT_CUBIC.inc(Easing.toDelta(lastStateUpdateTime, 300.0f))
        )
        RenderUtils2D.drawRectFilled(0.0f, 0.0f, renderWidth, renderHeight, overlayColor)

        // Slider frame
        if (GuiSetting.outline.a > 0) {
            RenderUtils2D.drawRectOutline(0.0f, 0.0f, renderWidth, renderHeight, 1.25f, GuiSetting.outline)
        }

        // Slider name
        val displayText = inputField.takeIf { listening } ?: name
        val prev = if (prevState == MouseState.NONE) 0.0f else 1.0f
        val curr = if (mouseState == MouseState.NONE) 0.0f else 1.0f
        val scale = Easing.OUT_BACK.incOrDec(Easing.toDelta(lastStateUpdateTime, 300.0f), prev, curr)
        MainFontRenderer.drawString(
            displayText,
            2.0f + 2.0f * scale,
            1.0f - 0.025f * scale * MainFontRenderer.getHeight(),
            color = GuiSetting.text,
            scale = 1.0f + 0.05f * scale
        )
    }

    override fun onPostRender(absolutePos: Vec2f) {
        if (Tooltips.isDisabled || description.isEmpty()) return

        var deltaTime = Easing.toDelta(lastStateUpdateTime)

        if (mouseState == MouseState.HOVER && deltaTime > 500L || prevState == MouseState.HOVER && shown) {

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

            val relativeCorner =
                Vec2f(mc.displayWidth.toFloat(), mc.displayHeight.toFloat()).div(GuiSetting.scaleFactorFloat)
                    .minus(absolutePos)

            val posX = descriptionPosX.coerceIn(-absolutePos.x, (relativeCorner.x - textWidth - 10.0f))
            val posY = (renderHeight + 4.0f).coerceIn(-absolutePos.y, (relativeCorner.y - textHeight - 10.0f))

            glDisable(GL_SCISSOR_TEST)
            glPushMatrix()
            glTranslatef(posX, posY, 696.0f)

            if (GuiSetting.windowBlur) {
                WindowBlurShader.render(textWidth + 4.0f, textHeight + 4.0f)
            }
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
            glPopMatrix()
        }
    }

    private fun getStateColor(state: MouseState) = when (state) {
        MouseState.NONE -> GuiSetting.idle
        MouseState.HOVER -> GuiSetting.hover
        else -> GuiSetting.click
    }
}