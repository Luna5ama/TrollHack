package dev.luna5ama.trollhack.gui.hud.impl

import dev.luna5ama.trollhack.RenderSystem
import dev.luna5ama.trollhack.gui.HudModule
import dev.luna5ama.trollhack.manager.managers.GuiManager
import dev.luna5ama.trollhack.manager.managers.ModuleManager
import dev.luna5ama.trollhack.manager.managers.UnicodeFontManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.modules.impl.client.Colors
import dev.luna5ama.trollhack.config.settings.BooleanSetting
import dev.luna5ama.trollhack.utils.BiPredicate
import dev.luna5ama.trollhack.utils.ChatUtils
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.MinecraftWrapper
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.utils.Nameable
import dev.luna5ama.trollhack.utils.collections.asSequenceFast
import dev.luna5ama.trollhack.graphics.GLHelper
import dev.luna5ama.trollhack.graphics.OpenGLWrapper
import dev.luna5ama.trollhack.graphics.animations.AnimationFlag
import dev.luna5ama.trollhack.graphics.animations.Easing
import dev.luna5ama.trollhack.graphics.buffer.Render2DUtils
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.shader.BlurRenderer
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.experimental.ExperimentalTypeInference
import kotlin.math.absoluteValue
import kotlin.math.max

object HudArrayList : HudModule("Array List") {
    override var width: Float = 0f
        get() = elementList.filter { !it.hidden }.maxOfOrNull { it.getBackgroundWidth() } ?: 10f
    override var height: Float = 0f
        get() = max(elementList.filter { !it.hidden }.size * (UnicodeFontManager.CURRENT_FONT.height + 2), 10f)

    private val prefix by setting("Prefix", " [")
    private val suffix by setting("Suffix", "]")
    private val animationLength by setting(
        "Animation Length",
        500,
        0..1000,
        100,
        onModified = listOf(BiPredicate { _, it -> initializeElements(ModuleManager.modules.filterIsInstance<Module>(), it); true })
    )
    private val abcd by setting("ASSSSSS", true)
    private val lineWidth by setting("Width", 1, 0..5, 1)
    private val renderColor by setting("Color", ColorEnum.RAINBOW)
    private val backgroundAlpha by setting("BackGroundAlpha", 128, 0..255)
    private val appearOnTop by setting("Appear On Top", false)
    private val infoBeforeName by setting("Info First", false)
    private val horizon by setting("Horizon", HorizonAlignment.LEFT)
    private val vertical by setting("Vertical", VerticalAlignment.UP)
    private val sortByLength by setting("Sort By Length", false)
    private val boundOnly by setting("Bound Only", false)
    private val hiddenList = mutableMapOf<Category, BooleanSetting>()

    private val elementList = CopyOnWriteArrayList<Element>()

    init {
        Category.entries
            .associateWith { setting("Hide ${it.displayName}", false) }
            .forEach { (c, s) -> hiddenList[c] = s }
    }

    override fun onRender2D(x: Float, y: Float) {
        elementList.forEach { element ->
            element.hidden = (element.module.category in hiddenList.filter { it.value.value }.keys
                    || element.module.isDisabled
                    || (boundOnly && element.module.bind.keyCode == GLFW.GLFW_KEY_UNKNOWN))

        }
        elementList.sortBy {
            if (sortByLength) UnicodeFontManager.CURRENT_FONT.getWidth(it.getRenderText()) + it.module.moduleId / 100.0
            else it.module.localizedName.uppercase()[0].code.toDouble()
        }
//        elementList.sortBy { it.hidden }
        if (vertical == VerticalAlignment.UP && sortByLength) elementList.reverse()
        RenderSystem.framebuffer.bind()

        glEnable(GL_STENCIL_TEST)
        glDisable(GL_DEPTH_TEST)

        glClearStencil(0)
        glClear(GL_STENCIL_BUFFER_BIT)
        glStencilFunc(GL_ALWAYS, 0, 0xff)
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE)
        glStencilMask(0xff)
        Render2DUtils.drawRect(0, 0, RenderSystem.scaledWidth.toInt(), RenderSystem.scaledHeight.toInt(), ColorRGBA(0, 0, 0, 0))

        glStencilFunc(GL_ALWAYS, 1, 0xff)
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE)
        val elementsToDraw = elementList.asSequenceFast()
//            .filter { !it.hidden }
//            .toMutableList()
//            .also { it.addAll(elementList.filter { e -> e.hidden }) }
            .onEachIndexed { index, element ->
                element.renderStencil(index)
            }

        glStencilFunc(GL_EQUAL, 1, 0xff)
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)
        glStencilMask(0x00)
        BlurRenderer.render(_x, _y, _x + width, _y + height)

        glDisable(GL_STENCIL_TEST)

        elementsToDraw.forEachIndexed { index, element ->
            element.render(index)
        }
    }

    @OptIn(ExperimentalTypeInference::class)
    fun initializeElements(
        @BuilderInference modules: List<Module>,
        length: Int = animationLength
    ) {
        elementList.clear()
        modules.map { Element(it, length) }.forEach { elementList.add(it) }
        elementList.distinctBy { it.module.moduleId }
    }

    private class Element(val module: Module, animationLength: Int) : Nameable {
        override val name: CharSequence
            get() = module.name

        private val animX = AnimationFlag(Easing.OUT_CUBIC, animationLength * 2f)
        private val animY = AnimationFlag(Easing.OUT_CUBIC, animationLength * 2f)

        var hidden = false

        private fun getDescription(): String {
            return if (module.getDisplayInfo() == null) ""
            else "$prefix${module.getDisplayInfo()}$suffix"
        }

        fun getRenderText(): String =
            if (infoBeforeName) "${ChatUtils.WHITE}${getDescription()} ${ChatUtils.RESET}${module.name}" else "${module.name}${ChatUtils.WHITE}${getDescription()}${ChatUtils.RESET}"

        private fun getFontX(): Float {
            return animX.getAndUpdate(
                if (hidden) {
                    if (horizon == HorizonAlignment.LEFT) 0 - UnicodeFontManager.CURRENT_FONT.getWidth(getRenderText()) - lineWidth - 1 else MinecraftWrapper.mc.window.guiScaledWidth + width
                } else {
                    if (horizon == HorizonAlignment.LEFT) _x + lineWidth + 1f else _x + width - UnicodeFontManager.CURRENT_FONT.getWidth(
                        getRenderText()
                    ) - lineWidth - 2f
                }.toFloat()
            )
        }

        private fun getFontY(index: Int): Float {
            return animY.getAndUpdate(
                if (hidden && appearOnTop) 0f
                else
                    _y + (index) * (UnicodeFontManager.CURRENT_FONT.height + 2f) + 1f
            )
        }

        private fun getBackgroundX(): Float {
            return if (horizon == HorizonAlignment.LEFT) getFontX() - 1 - lineWidth else getFontX() - 1
        }

        private fun getBackgroundY(index: Int): Float {
            return getFontY(index) - 1
        }

        fun getBackgroundWidth(): Float {
            return UnicodeFontManager.CURRENT_FONT.getWidth(getRenderText()) + 2 + lineWidth
        }

        private fun getBackgroundHeight(): Float {
            return UnicodeFontManager.CURRENT_FONT.height + 2
        }

        fun renderStencil(index: Int) {
            if (Colors.blur) {
                val bgX = getBackgroundX()
                val bgY = getBackgroundY(index)
                Render2DUtils.drawRect(bgX, bgY, bgX + getBackgroundWidth(), bgY + getBackgroundHeight(), ColorRGBA(0, 0, 0, 0))
            }
        }

        fun render(index: Int) {
            if (!isHidden()) {
                val fontColor = renderColor.colorSupplier(index)
                val bgX = getBackgroundX()
                val bgY = getBackgroundY(index)
//                if (Colors.blur) {
//                    BlurRenderer.render(bgX, bgY, bgX + getBackgroundWidth(), bgY + getBackgroundHeight())
//                }
                Render2DUtils.drawRect(bgX, bgY, bgX + getBackgroundWidth(), bgY + getBackgroundHeight(), ColorRGBA(0, 0, 0, backgroundAlpha))
                UnicodeFontManager.CURRENT_FONT.drawStringWithShadow(getRenderText(), getFontX(), getFontY(index), fontColor)
                if (lineWidth != 0) {
                    if (horizon == HorizonAlignment.LEFT) {
                        Render2DUtils.drawRect(bgX, bgY, bgX + lineWidth, bgY + getBackgroundHeight(), fontColor)
                    } else {
                        Render2DUtils.drawRect(bgX + getBackgroundWidth() - lineWidth, bgY, bgX + getBackgroundWidth(), bgY + getBackgroundHeight(), fontColor)
                    }
                }
            }
        }

        private fun isHidden(): Boolean {
            return hidden && (getFontX() - if (hidden) {
                if (horizon == HorizonAlignment.LEFT) 0 - UnicodeFontManager.CURRENT_FONT.getWidth(getRenderText()) - lineWidth - 1 else MinecraftWrapper.mc.window.guiScaledWidth + width
            } else {
                if (horizon == HorizonAlignment.LEFT) _x + lineWidth + 1f else _x + width - UnicodeFontManager.CURRENT_FONT.getWidth(
                    getRenderText()
                ) - lineWidth - 2f
            }.toFloat()).absoluteValue <= 0.01
        }
    }

    private enum class ColorEnum(val colorSupplier: (Int) -> ColorRGBA) : Displayable {
        RAINBOW({ index -> ColorRGBA(GuiManager.getRainbow(index * 100)) }),
        WHITE({ ColorRGBA.WHITE })
    }

    private enum class HorizonAlignment : Displayable {
        LEFT, RIGHT
    }

    private enum class VerticalAlignment : Displayable {
        UP, DOWN
    }
}