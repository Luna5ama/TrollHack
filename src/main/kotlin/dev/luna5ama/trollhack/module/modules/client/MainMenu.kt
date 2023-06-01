package dev.luna5ama.trollhack.module.modules.client

import dev.luna5ama.trollhack.event.events.GuiEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.modules.render.AntiAlias
import dev.luna5ama.trollhack.setting.GenericConfig
import dev.luna5ama.trollhack.translation.TranslateType
import dev.luna5ama.trollhack.translation.TranslationKey
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.extension.mapEach
import dev.luna5ama.trollhack.util.extension.normalizeCase
import dev.luna5ama.trollhack.util.graphics.GlStateUtils
import dev.luna5ama.trollhack.util.graphics.RenderUtils2D
import dev.luna5ama.trollhack.util.graphics.color.ColorRGB
import dev.luna5ama.trollhack.util.graphics.font.renderer.FontRenderer
import dev.luna5ama.trollhack.util.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.util.graphics.shaders.GLSLSandbox
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.math.Box
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.SoundEvents
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.*
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.awt.Font
import java.util.*
import kotlin.math.max
import kotlin.math.min

internal object MainMenu : AbstractModule(
    name = "Main Menu",
    description = "Better Main Menu",
    category = Category.CLIENT,
    visible = false,
    alwaysEnabled = true,
    config = GenericConfig
) {
    private val title by setting("Title", Title.TROLL_HACK)
    private val mode by setting("Mode", Mode.SET)
    private val background by setting("Background", ShaderEnum.PLANET, { mode == Mode.SET })
    private val fpsLimit by setting("Fps Limit", 60, 10..240, 10)

    @Suppress("unused")
    enum class Title(override val displayName: CharSequence) : DisplayEnum {
        TROLL_HACK("Troll Hack"),
        MINECRAFT("Minecraft")
    }

    private enum class Mode {
        RANDOM, SET
    }

    @Suppress("UNUSED")
    private enum class ShaderEnum {
        PLANET,
        BLACK_HOLE,
        BLUE_GRID,
        BLUE_LANDSCAPE,
        CIRCUITS,
        CUBE_CAVE,
        GREEN_NEBULA,
        GRID_CAVE,
        MATRIX,
        MINECRAFT,
        PURPLE_GRID,
        RECT_WAVES,
        RED_LANDSCAPE,
        SPACE,
        TUBE;

        val path = name
            .mapEach('_') { it.normalizeCase() }
            .joinToString("", "/assets/trollhack/shaders/menu/", ".fsh")
    }

    private val shaderCache = EnumMap<ShaderEnum, GLSLSandbox>(ShaderEnum::class.java)
    private var initTime = System.currentTimeMillis()
    private var currentShader = getShader()

    init {
        listener<GuiEvent.Displayed> {
            if (it.screen is GuiMainMenu) {
                it.screen = TrollGuiMainMenu()
            }
        }
        (TrollGuiMainMenu.Companion).toString()
    }

    @JvmStatic
    fun handleGetLimitFramerate(cir: CallbackInfoReturnable<Int>) {
        if (mc.world == null && mc.currentScreen != null) {
            cir.returnValue = fpsLimit
        }
    }

    private fun renderBackground() {
        val width = mc.displayWidth * AntiAlias.sampleLevel
        val height = mc.displayHeight * AntiAlias.sampleLevel
        val mouseX = Mouse.getX() - 1.0f
        val mouseY = height - Mouse.getY() - 1.0f

        currentShader.render(width, height, mouseX * AntiAlias.sampleLevel, mouseY * AntiAlias.sampleLevel, initTime)
    }

    private fun resetBackground() {
        currentShader = getShader()
    }

    private fun getShader(): GLSLSandbox {
        val shader = if (mode == Mode.RANDOM) {
            ShaderEnum.values().random()
        } else {
            background
        }

        return shaderCache.getOrPut(shader) {
            GLSLSandbox(shader.path)
        }
    }

    class TrollGuiMainMenu : GuiScreen() {
        private val buttons = ArrayList<Button>()

        private val singlePlayerButton = newButton(singlePlayer) {
            mc.displayGuiScreen(GuiWorldSelection(this))
        }
        private val multiPlayerButton = newButton(multiPlayer) {
            mc.displayGuiScreen(GuiMultiplayer(this))
        }
        private val optionsButton = newButton(options) {
            mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
        }
        private val exitButton = newButton(exit) {
            mc.shutdown()
        }

        override fun initGui() {
            resetBackground()

            buttons.forEach {
                it.updatePos(mc.displayWidth.toFloat(), mc.displayHeight.toFloat())
            }
        }

        private fun newButton(text: TranslationKey, action: () -> Unit): Button {
            val button = Button(buttons.size, text, action)
            buttons.add(button)
            return button
        }

        override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
            renderBackground()

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

        companion object {
            private val singlePlayer = TranslateType.SPECIFIC key ("singlePlayer" to "Single Player")
            private val multiPlayer = TranslateType.SPECIFIC key ("multiPlayer" to "Multi Player")
            private val options = TranslateType.SPECIFIC key ("options" to "Options")
            private val exit = TranslateType.SPECIFIC key ("exit" to "Exit")
        }

        private class Button(private val index: Int, private val text: TranslationKey, val action: () -> Unit) {
            private var posX = 0.0f
            private var posY = 0.0f

            private val text1 get() = text.toString().first().toString()
            private val text2 get() = text.toString().substring(1)

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

                RenderUtils2D.drawRectFilled(
                    posX + 1.0f,
                    posY + 1.0f,
                    posX + buttonWidth + 1.0f,
                    posY + 3.0f + 1.0f,
                    ColorRGB(64, 64, 64, 200)
                )
                RenderUtils2D.drawRectFilled(posX, posY, posX + buttonWidth, posY + 3.0f, lineColor)

                if (Language.settingLanguage.startsWith("en")) {
                    val scale = 0.5f
                    ButtonFontRenderer.drawString(text1, posX, posY + 5.0f, ColorRGB(230, 158, 42), scale = scale)
                    ButtonFontRenderer.drawString(
                        text2,
                        posX + ButtonFontRenderer.getWidth(text1, scale = scale),
                        posY + 5.0f,
                        scale = scale
                    )
                } else {
                    val scale = 2.0f
                    MainFontRenderer.drawString(text1, posX, posY + 5.0f, ColorRGB(230, 158, 42), scale = scale)
                    MainFontRenderer.drawString(
                        text2,
                        posX + MainFontRenderer.getWidth(text1, scale = scale),
                        posY + 5.0f,
                        scale = scale
                    )
                }
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

    private object TitleFontRender : FontRenderer(
        Font.createFont(
            Font.TRUETYPE_FONT,
            this::class.java.getResourceAsStream("/assets/trollhack/fonts/Orbitron-Regular.ttf")
        ), 80.0f, 2048
    ) {
        override val shadowDist: Float
            get() = 4.0f
    }

    private object ButtonFontRenderer : FontRenderer(
        Font.createFont(
            Font.TRUETYPE_FONT,
            this::class.java.getResourceAsStream("/assets/trollhack/fonts/GOTHIC.TTF")
        ), 36.0f, 1024
    )
}