package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.util.interfaces.DisplayEnum
import cum.xiaro.trollhack.event.events.GuiEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.Wrapper
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.graphics.RenderUtils2D
import cum.xiaro.trollhack.util.graphics.font.renderer.FontRenderer
import cum.xiaro.trollhack.util.math.Box
import cum.xiaro.trollhack.util.math.vector.Vec2f
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.SoundEvents
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.*
import java.awt.Font
import kotlin.math.max
import kotlin.math.min

internal object MainMenu : Module(
    name = "MainMenu",
    description = "Better Main Menu",
    category = Category.RENDER,
    visible = false,
    enabledByDefault = true
) {
    private val title by setting("Title", Title.MINECRAFT)

    @Suppress("unused")
    enum class Title(override val displayName: CharSequence) : DisplayEnum {
        MINECRAFT("Minecraft"),
        TROLL_HACK("Troll Hack")
    }

    init {
        listener<GuiEvent.Displayed>(true) {
            if (it.screen is GuiMainMenu) {
                it.screen = TrollGuiMainMenu()
            }
        }
    }

    class TrollGuiMainMenu : GuiScreen() {
        private val buttons = ArrayList<Button>()

        private val singlePlayerButton = newButton("Single Player") {
            mc.displayGuiScreen(GuiWorldSelection(this))
        }
        private val multiPlayerButton = newButton("Multi Player") {
            mc.displayGuiScreen(GuiMultiplayer(this))
        }
        private val optionsButton = newButton("Options") {
            mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
        }
        private val exitButton = newButton("Exit") {
            mc.shutdown()
        }

        override fun initGui() {
            MainMenuShader.reset()

            buttons.forEach {
                it.updatePos(mc.displayWidth.toFloat(), mc.displayHeight.toFloat())
            }
        }

        private fun newButton(text: String, action: () -> Unit): Button {
            val button = Button(buttons.size, text, action)
            buttons.add(button)
            return button
        }

        override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
            if (MainMenuShader.isEnabled) {
                MainMenuShader.render()
            }

            GlStateUtils.rescaleActual()
            GlStateUtils.blend(true)
            GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)

            val posX = min(max(mc.displayWidth / 2.0f - TitleFontRender.getWidth(title.displayName), 100.0f), 200.0f)
            val posY = 100.0f * (posX / 200.0f)

            TitleFontRender.drawString(title.displayName, posX, posY)

            buttons.forEach {
                it.onRender()
            }
        }

        override fun handleMouseInput() {
            val mouseX = Mouse.getEventX() - 1.0f
            val mouseY = Wrapper.minecraft.displayHeight - Mouse.getEventY() - 1.0f
            val button = Mouse.getEventButton()
            val state = Mouse.getEventButtonState()

            buttons.forEach {
                if (it.isOnButton(mouseX, mouseY)) {
                    it.onHover()
                    if (button == 0 || button == 1) {
                        if (state) it.onClick()
                        else it.onRelease()
                    }
                } else {
                    it.onLeave()
                }
            }
        }

        override fun keyTyped(typedChar: Char, keyCode: Int) {
            when (typedChar) {
                's', 'S' -> singlePlayerButton.action.invoke()
                'm', 'M' -> multiPlayerButton.action.invoke()
                'o', 'O' -> optionsButton.action.invoke()
                'e', 'E' -> exitButton.action.invoke()
            }
        }

        private class Button(private val index: Int, text: String, val action: () -> Unit) {
            private var posX = 0.0f
            private var posY = 0.0f

            private val text1 = text.first().toString()
            private val text2 = text.substring(1)

            private val quad = Box(0.0f, 0.0f, 0.0f, 0.0f)

            private var hovered = false
            private var clicked = false

            fun isOnButton(x: Float, y: Float): Boolean {
                return quad.contains(x, y)
            }

            fun updatePos(width: Float, height: Float) {
                posX = width / 2.0f + itemPadding / 2.0f + (index - 2) * (itemPadding + buttonWidth)
                posY = height - bottomPadding

                quad.x1 = posX
                quad.y1 = posY
                quad.x2 = posX + buttonWidth
                quad.y2 = posY + buttonHeight
            }

            fun onRender() {
                val lineColor = when {
                    clicked -> ColorRGB(172, 97, 32)
                    hovered -> ColorRGB(215, 121, 39)
                    else -> ColorRGB(183, 183, 183)
                }

                RenderUtils2D.drawRectFilled(Vec2f(posX + 1.0f, posY + 1.0f), Vec2f(posX + buttonWidth + 1.0f, posY + 3.0f + 1.0f), ColorRGB(64, 64, 64, 200))
                RenderUtils2D.drawRectFilled(Vec2f(posX, posY), Vec2f(posX + buttonWidth, posY + 3.0f), lineColor)

                ButtonFontRenderer.drawString(text1, posX, posY + 5.0f, ColorRGB(230, 158, 42))
                ButtonFontRenderer.drawString(text2, posX + ButtonFontRenderer.getWidth(text1), posY + 5.0f)
            }

            fun onHover() {
                hovered = true
            }

            fun onClick() {
                clicked = true
            }

            fun onRelease() {
                val prev = clicked
                clicked = false
                mc.soundHandler.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F))
                if (prev) action.invoke()
            }

            fun onLeave() {
                hovered = false
                clicked = false
            }

            private companion object {
                const val bottomPadding = 150.0f
                const val itemPadding = 10.0f

                const val buttonWidth = 200.0f
                const val buttonHeight = 30.0f
            }
        }
    }

    private object TitleFontRender : FontRenderer(Font.createFont(Font.TRUETYPE_FONT, this::class.java.getResourceAsStream("/assets/trollhack/fonts/Orbitron-Regular.ttf")), 80.0f, 5120) {
        override val shadowDist: Float
            get() = 4.0f
    }

    private object ButtonFontRenderer : FontRenderer(Font.createFont(Font.TRUETYPE_FONT, this::class.java.getResourceAsStream("/assets/trollhack/fonts/GOTHIC.TTF")), 18.0f, 512)
}
