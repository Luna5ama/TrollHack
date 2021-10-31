package cum.xiaro.trollhack.gui.rgui.component

import cum.xiaro.trollhack.util.graphics.Easing
import cum.xiaro.trollhack.gui.rgui.InteractiveComponent
import cum.xiaro.trollhack.module.modules.client.GuiSetting
import cum.xiaro.trollhack.module.modules.client.Tooltips
import cum.xiaro.trollhack.util.delegate.FrameValue
import cum.xiaro.trollhack.util.graphics.RenderUtils2D
import cum.xiaro.trollhack.util.graphics.font.TextComponent
import cum.xiaro.trollhack.util.graphics.font.renderer.MainFontRenderer
import cum.xiaro.trollhack.util.graphics.shaders.WindowBlurShader
import cum.xiaro.trollhack.util.math.vector.Vec2d
import cum.xiaro.trollhack.util.math.vector.Vec2f
import cum.xiaro.trollhack.util.state.TimedFlag
import org.lwjgl.opengl.GL11.*

open class Slider(
    name: CharSequence,
    value: Float,
    private val description: CharSequence = "",
    private val visibility: ((() -> Boolean))?
) : InteractiveComponent(name, 0.0f, 0.0f, 40.0f, 10.0f, SettingGroup.NONE) {

    protected var inputField = ""

    protected var value = value
        set(value) {
            if (value != field) {
                prevValue.value = renderProgress
                field = value.coerceIn(0.0f, 1.0f)
            }
        }

    protected val prevValue = TimedFlag(this.value)
    protected open val renderProgress by FrameValue {
        Easing.OUT_QUAD.incOrDec(Easing.toDelta(prevValue.lastUpdateTime, 100.0f), prevValue.value, this.value)
    }

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
        super.onDisplayed()
        prevValue.value = 0.0f
        value = 0.0f
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
        if (renderProgress > 0.0f) {
            RenderUtils2D.drawRectFilled(Vec2f.ZERO, Vec2f(renderWidth * renderProgress, renderHeight), GuiSetting.primary)
        }

        // Slider hover overlay
        val overlayColor = getStateColor(prevState).mix(getStateColor(mouseState), Easing.OUT_CUBIC.inc(Easing.toDelta(lastStateUpdateTime, 200)))
        RenderUtils2D.drawRectFilled(Vec2f.ZERO, Vec2f(renderWidth, renderHeight), overlayColor)

        // Slider frame
        RenderUtils2D.drawRectOutline(Vec2f.ZERO, Vec2f(renderWidth, renderHeight), 1.25f, GuiSetting.outline)

        // Slider name

        // TODO: do something with this https://discord.com/channels/573954110454366214/789630848194183209/795732239211429909
        //GlStateUtils.pushScissor()
        /*if (protectedWidth > 0.0) {
            GlStateUtils.scissor(
                    ((absolutePos.x + renderWidth - protectedWidth) * ClickGUI.getScaleFactor()).roundToInt(),
                    (mc.displayHeight - (absolutePos.y + renderHeight) * ClickGUI.getScaleFactor()).roundToInt(),
                    (protectedWidth * ClickGUI.getScaleFactor()).roundToInt(),
                    (renderHeight * ClickGUI.getScaleFactor()).roundToInt()
            )
        }*/

        val lol = inputField.takeIf { listening } ?: name
        MainFontRenderer.drawString(lol, 1.5f, 1.0f, color = GuiSetting.text)
        //GlStateUtils.popScissor()
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

            val textWidth = displayDescription.getWidth().toDouble()
            val textHeight = displayDescription.getHeight(2).toDouble()

            val relativeCorner = Vec2f(mc.displayWidth.toFloat(), mc.displayHeight.toFloat()).div(GuiSetting.scaleFactorFloat).minus(absolutePos)

            val posX = descriptionPosX.coerceIn(-absolutePos.x, (relativeCorner.x - textWidth - 10.0f).toFloat())
            val posY = (renderHeight + 4.0f).coerceIn(-absolutePos.y, (relativeCorner.y - textHeight - 10.0f).toFloat())

            glDisable(GL_SCISSOR_TEST)
            glPushMatrix()
            glTranslatef(posX, posY, 696.0f)

            if (GuiSetting.windowBlur) {
                WindowBlurShader.render(textWidth.toFloat() + 4.0f, textHeight.toFloat() + 4.0f)
            }
            RenderUtils2D.drawRectFilled(posEnd = Vec2f(textWidth, textHeight).plus(4.0f), color = GuiSetting.backGround.run { alpha((a * alpha).toInt()) })
            if (GuiSetting.windowOutline) {
                RenderUtils2D.drawRectOutline(posEnd = Vec2f(textWidth, textHeight).plus(4.0f), lineWidth = 1.0f, color = GuiSetting.primary.alpha((255 * alpha).toInt()))
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