package dev.luna5ama.trollhack.module.modules.client

import dev.luna5ama.trollhack.event.events.GuiEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.gui.rgui.InteractiveComponent
import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.modules.render.AntiAlias
import dev.luna5ama.trollhack.setting.GenericConfig
import dev.luna5ama.trollhack.translation.TranslateType
import dev.luna5ama.trollhack.translation.TranslationKey
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.extension.mapEach
import dev.luna5ama.trollhack.util.extension.normalizeCase
import dev.luna5ama.trollhack.util.graphics.Easing
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
    private val backgroundShader by setting("Background Shader", ShaderEnum.GALAXY, { mode == Mode.SET })
    private val fpsLimit by setting("Fps Limit", 60, 10..240, 10)

    @Suppress("unused")
    enum class Title(override val displayName: CharSequence, val titleName: String) : DisplayEnum {
        TROLL_HACK("Troll Hack", "TROLL HACK"),
        MINECRAFT("Minecraft", "MINCERAFT"),
    }

    private enum class Mode {
        RANDOM, SET
    }

    @Suppress("UNUSED")
    private enum class ShaderEnum {
        GALAXY,
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
            .joinToString("", "/assets/trollhack/shaders/menu/", ".frag.glsl")
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
            backgroundShader
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

            val scale = ((mc.displayWidth * 2 + mc.displayHeight) / 4000.0f).coerceIn(0.5f, 1.0f)

            val posX = mc.displayWidth / 12.0f
            val posY = posX / 2.0f

            TitleFontRender.drawString(title.titleName, posX, posY, scale = scale)

            buttons.forEach {
                it.onRender()
            }
        }

        override fun handleMouseInput() {
            val mouseX = Mouse.getEventX() - 1.0f
            val mouseY = mc.displayHeight - Mouse.getEventY() - 1.0f
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
            private val singlePlayer = TranslateType.SPECIFIC key ("singlePlayer" to "Singleplayer")
            private val multiPlayer = TranslateType.SPECIFIC key ("multiPlayer" to "Multiplayer")
            private val options = TranslateType.SPECIFIC key ("options" to "Options")
            private val exit = TranslateType.SPECIFIC key ("exit" to "Exit")
        }

        private class Button(private val index: Int, private val text: TranslationKey, val action: () -> Unit) {
            private var posX = 0.0f
            private var posY = 0.0f

            private val text1 get() = text.toString().first().toString()
            private val text2 get() = text.toString().substring(1)

            private val quad = Box(0.0f, 0.0f, 0.0f, 0.0f)

            private var lastUpdateTime = System.currentTimeMillis()
            private var prevState = InteractiveComponent.MouseState.NONE
            private var mouseState = InteractiveComponent.MouseState.NONE
                set(value) {
                    if (field != value) {
                        prevState = field
                        field = value
                        lastUpdateTime = System.currentTimeMillis()
                    }
                }

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

            private fun getStateColor(state: InteractiveComponent.MouseState): ColorRGB {
                return when (state) {
                    InteractiveComponent.MouseState.HOVER -> ColorRGB(215, 121, 39)
                    InteractiveComponent.MouseState.CLICK -> ColorRGB(172, 97, 32)
                    else -> ColorRGB(183, 183, 183)
                }
            }

            fun onRender() {
                val lineColor = getStateColor(prevState).mix(
                    getStateColor(mouseState),
                    Easing.OUT_CUBIC.inc(Easing.toDelta(lastUpdateTime, 300.0f))
                )

                val scale = min((mc.displayWidth * 2 + mc.displayHeight) / 4000.0f + 1.25f, 3.0f)

                RenderUtils2D.drawRectFilled(
                    posX + 1.0f,
                    posY + 1.0f,
                    posX + buttonWidth + 2.0f,
                    posY + scale * 1.5f + 2.0f,
                    ColorRGB(64, 64, 64, 160)
                )
                RenderUtils2D.drawRectFilled(posX, posY, posX + buttonWidth, posY + scale * 1.5f, lineColor)

                MainFontRenderer.drawString(text1, posX, posY + 5.0f, ColorRGB(230, 158, 42), scale = scale)
                MainFontRenderer.drawString(
                    text2,
                    posX + MainFontRenderer.getWidth(text1, scale = scale),
                    posY + 5.0f,
                    scale = scale
                )
            }

            fun onHover() {
                if (mouseState == InteractiveComponent.MouseState.NONE) {
                    mouseState = InteractiveComponent.MouseState.HOVER
                }
            }

            fun onClick() {
                mouseState = InteractiveComponent.MouseState.CLICK
            }

            fun onRelease() {
                val prev = mouseState
                mouseState = InteractiveComponent.MouseState.NONE
                if (prev == InteractiveComponent.MouseState.CLICK) {
                    mc.soundHandler.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F))
                    action.invoke()
                }
            }

            fun onLeave() {
                mouseState = InteractiveComponent.MouseState.NONE
            }

            private companion object {
                val bottomPadding get() = min((mc.displayWidth + mc.displayHeight * 2) / 25.0f, 150.0f)
                const val itemPadding = 10.0f

                val buttonWidth get() = min(mc.displayWidth / 6.0f, 300.0f)
                const val buttonHeight = 30.0f
            }
        }
    }

    private object TitleFontRender : FontRenderer(
        Font.createFont(
            Font.TRUETYPE_FONT,
            this::class.java.getResourceAsStream("/assets/trollhack/fonts/Jura-Light.ttf")
        ), 128.0f, 4096
    ) {
        override val charGap: Float
            get() = 20.0f
        override val shadowDist: Float
            get() = 4.0f
    }
}