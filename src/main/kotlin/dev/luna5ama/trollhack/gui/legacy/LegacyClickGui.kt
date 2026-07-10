package dev.luna5ama.trollhack.gui.legacy

import dev.luna5ama.trollhack.config.settings.AbstractRangedSetting
import dev.luna5ama.trollhack.config.settings.AbstractSetting
import dev.luna5ama.trollhack.config.settings.BindSetting
import dev.luna5ama.trollhack.config.settings.BooleanSetting
import dev.luna5ama.trollhack.config.settings.ColorSetting
import dev.luna5ama.trollhack.config.settings.EnumSetting
import dev.luna5ama.trollhack.config.settings.StringSetting
import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.Render2DEvent
import dev.luna5ama.trollhack.graphics.animations.AnimationFlag
import dev.luna5ama.trollhack.graphics.animations.Easing
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.color.ColorUtils
import dev.luna5ama.trollhack.graphics.font.FontRenderer
import dev.luna5ama.trollhack.gui.NullClickGui
import dev.luna5ama.trollhack.manager.managers.ModuleManager
import dev.luna5ama.trollhack.manager.managers.UnicodeFontManager
import dev.luna5ama.trollhack.modules.AbstractModule
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.impl.client.ClickGui
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.input.KeyBind
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object LegacyClickGui : AlwaysListening {
    private const val WINDOW_WIDTH = 86f
    private const val WINDOW_HEIGHT = 300f
    private const val TITLE_HEIGHT = 16f
    private const val ROW_HEIGHT = 13f
    private const val MARGIN_X = 3f
    private const val MARGIN_Y = 3f

    private val mc: Minecraft get() = Minecraft.getInstance()
    private val windows = mutableListOf<LegacyWindow>()
    private var activeWindow: LegacyWindow? = null
    private var mouseX = 0f
    private var mouseY = 0f
    private var searchString = ""
    private var listeningComponent: LegacyComponent? = null
    private val displayAnim = AnimationFlag(Easing.OUT_CUBIC, 260f)
    private var activeSettingWindow: LegacyWindow? = null
    private val textQueue = ArrayList<TextDraw>()
    private var suppressTextInputUntil = 0L

    init {
        reloadPanel()
        nonNullHandler<Render2DEvent> {
            renderTextOverlay()
        }
    }

    fun open() {
        reloadPanelIfEmpty()
        displayAnim.forceUpdate(0f, 0f)
        suppressTextInputUntil = System.currentTimeMillis() + 180L
        windows.forEach { it.onDisplayed() }
        mc.setScreen(NullClickGui)
    }

    fun close() {
        windows.forEach { it.onClosed() }
        activeWindow = null
        listeningComponent = null
        searchString = ""
        if (mc.screen === NullClickGui) mc.setScreen(null)
        ClickGui.disable()
    }

    fun reloadPanel() {
        windows.clear()
        var x = 4f
        var y = 6f
        Category.entries.filter { it != Category.HUD }.forEach { category ->
            val window = ModuleListWindow(category, x, y)
            windows.add(window)
            x += WINDOW_WIDTH
            if (x + WINDOW_WIDTH > max(NullClickGui.width.toFloat(), 480f)) {
                x = 4f
                y += 92f
            }
        }
    }

    fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        this.mouseX = mouseX.toFloat()
        this.mouseY = mouseY.toFloat()
        textQueue.clear()
        reloadPanelIfEmpty()

        val display = displayAnim.getAndUpdate(1f)
        fill(context, 0f, 0f, NullClickGui.width.toFloat(), NullClickGui.height.toFloat(), argb((92 * display).toInt(), 0, 0, 0))

        val offsetY = -24f * (1f - display)
        windows.forEach {
            it.tick()
            if (it.visible) it.render(context, offsetY)
        }

        if (searchString.isNotBlank()) {
            val text = searchString
            val width = fontWidth(text) * 2
            val x = NullClickGui.width / 2f - width / 2f
            val y = NullClickGui.height / 2f - 10f
            fill(context, x - 5f, y - 4f, width + 10f, 20f, argb(180, 12, 14, 18))
            text(context, text, x, y, argb(255, 235, 245, 255), 2f)
        }
    }

    fun renderTextOverlay() {
        if (mc.screen !== NullClickGui || textQueue.isEmpty()) return
        textQueue.forEach { draw ->
            val font = fontFor(draw.text) ?: return@forEach
            font.drawStringWithShadow(draw.text, draw.x, draw.y, awt(draw.color), draw.scale)
        }
    }

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        this.mouseX = mouseX.toFloat()
        this.mouseY = mouseY.toFloat()
        activeWindow?.dragTo(this.mouseX, this.mouseY)
    }

    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        this.mouseX = mouseX.toFloat()
        this.mouseY = mouseY.toFloat()
        val window = windows.asReversed().firstOrNull { it.visible && it.contains(this.mouseX, this.mouseY) }
        activeWindow = window
        if (window != null) {
            windows.remove(window)
            windows.add(window)
            window.mouseClicked(this.mouseX, this.mouseY, button)
            return true
        }
        closeActiveSettingWindow()
        listeningComponent?.stopListening(false)
        listeningComponent = null
        return true
    }

    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        this.mouseX = mouseX.toFloat()
        this.mouseY = mouseY.toFloat()
        activeWindow?.mouseReleased(this.mouseX, this.mouseY, button)
        activeWindow?.stopDrag()
        return true
    }

    fun mouseScrolled(mouseX: Double, mouseY: Double, verticalAmount: Double): Boolean {
        this.mouseX = mouseX.toFloat()
        this.mouseY = mouseY.toFloat()
        windows.asReversed().firstOrNull { it.visible && it.contains(this.mouseX, this.mouseY) }
            ?.scroll(verticalAmount.toFloat())
        return true
    }

    fun keyPressed(key: Int, scanCode: Int): Boolean {
        listeningComponent?.let {
            it.keyPressed(key, scanCode)
            return true
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            close()
            return true
        }

        if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
            searchString = ""
            updateSearch()
            return true
        }

        activeWindow?.keyPressed(key, scanCode)
        return true
    }

    fun charTyped(char: Char): Boolean {
        if (System.currentTimeMillis() < suppressTextInputUntil) return true
        listeningComponent?.let {
            it.charTyped(char)
            return true
        }
        if (!char.isISOControl() && (char.isLetterOrDigit() || char == ' ')) {
            searchString += char
            updateSearch()
        }
        return true
    }

    private fun updateSearch() {
        val needle = searchString.replace(" ", "")
        windows.filterIsInstance<ModuleListWindow>().forEach { window ->
            window.filter(needle)
        }
    }

    private fun displayWindow(window: LegacyWindow) {
        windows.remove(window)
        windows.add(window)
        window.visible = true
        window.onDisplayed()
        activeWindow = window
    }

    private fun displaySettingWindow(window: LegacyWindow) {
        activeSettingWindow?.let { closeWindow(it) }
        activeSettingWindow = window
        displayWindow(window)
    }

    private fun closeWindow(window: LegacyWindow) {
        if (activeWindow === window) activeWindow = null
        if (activeSettingWindow === window) activeSettingWindow = null
        windows.remove(window)
        window.onClosed()
    }

    private fun closeActiveSettingWindow() {
        activeSettingWindow?.let { closeWindow(it) }
    }

    private fun listen(component: LegacyComponent?) {
        val previous = listeningComponent
        listeningComponent = component
        if (previous !== component) previous?.stopListening(false)
    }

    private fun clearListening(component: LegacyComponent) {
        if (listeningComponent === component) listeningComponent = null
    }

    private fun reloadPanelIfEmpty() {
        if (windows.none { it is ModuleListWindow }) reloadPanel()
    }

    private abstract class LegacyWindow(
        val title: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        var visible = true
        protected var x = x
        protected var y = y
        protected var width = width
        protected var height = height
        protected val children = mutableListOf<LegacyComponent>()
        protected var scroll = 0f
        private var scrollSpeed = 0f
        private var dragging = false
        private var resizing = false
        private var minimized = false
        private var dragOffsetX = 0f
        private var dragOffsetY = 0f
        private var lastClickTime = 0L
        private val minimizeAnim = AnimationFlag(Easing.OUT_QUART, 240f)
        private val xAnim = AnimationFlag(Easing.OUT_CUBIC, 180f)
        private val yAnim = AnimationFlag(Easing.OUT_CUBIC, 180f)
        private val widthAnim = AnimationFlag(Easing.OUT_CUBIC, 180f)
        private val heightAnim = AnimationFlag(Easing.OUT_CUBIC, 180f)

        open fun onDisplayed() {
            xAnim.forceUpdate(x, x)
            yAnim.forceUpdate(y, y)
            widthAnim.forceUpdate(width, width)
            heightAnim.forceUpdate(height, height)
            children.forEach { it.onDisplayed() }
        }

        open fun onClosed() {
            children.forEach { it.stopListening(false) }
        }

        fun tick() {
            children.forEach { it.tick() }
            if (!dragging && !resizing) {
                x = x.coerceIn(0f, max(0f, NullClickGui.width - width))
                y = y.coerceIn(0f, max(0f, NullClickGui.height - TITLE_HEIGHT))
            }
            if (abs(scrollSpeed) > 0.01f) {
                scroll += scrollSpeed
                scrollSpeed *= 0.78f
            }
            clampScroll()
        }

        fun render(context: GuiGraphics, extraY: Float) {
            val rx = xAnim.getAndUpdate(x)
            val ry = yAnim.getAndUpdate(y + extraY)
            val rw = widthAnim.getAndUpdate(width)
            val rh = heightAnim.getAndUpdate(height)
            val open = minimizeAnim.getAndUpdate(if (minimized) 0f else 1f)
            val bodyHeight = max(TITLE_HEIGHT, TITLE_HEIGHT + (rh - TITLE_HEIGHT) * open)

            fill(context, rx, ry, rw, bodyHeight, argb(176, 17, 18, 22))
            stroke(context, rx, ry, rw, bodyHeight, argb(255, 80, 190, 255))
            fill(context, rx, ry, rw, TITLE_HEIGHT, argb(235, 40, 120, 165))
            text(context, title, rx + 3f, ry + 3.5f, argb(255, 245, 250, 255))
            text(context, if (minimized) "+" else "-", rx + rw - 10f, ry + 3.5f, argb(255, 245, 250, 255))

            if (open <= 0.02f) return

            val contentTop = ry + TITLE_HEIGHT
            val contentBottom = ry + bodyHeight
            context.enableScissor((rx + MARGIN_X).roundToInt(), contentTop.roundToInt(), (rx + rw - MARGIN_X).roundToInt(), contentBottom.roundToInt())
            renderChildren(context, rx, contentTop - scroll, rw)
            context.disableScissor()

            val maxScroll = maxScroll()
            if (maxScroll > 0f) {
                val barH = max(18f, (rh - TITLE_HEIGHT) * ((rh - TITLE_HEIGHT) / (rh - TITLE_HEIGHT + maxScroll)))
                val barY = contentTop + (rh - TITLE_HEIGHT - barH) * (scroll / maxScroll).coerceIn(0f, 1f)
                fill(context, rx + rw - 2f, barY, 1f, barH, argb(180, 120, 210, 255))
            }
        }

        protected open fun renderChildren(context: GuiGraphics, rx: Float, baseY: Float, rw: Float) {
            var y = baseY + MARGIN_Y
            children.filter { it.effectiveVisible }.forEach {
                it.setBounds(rx + MARGIN_X, y, rw - MARGIN_X * 2f, ROW_HEIGHT)
                it.render(context)
                y += ROW_HEIGHT + MARGIN_Y
            }
        }

        fun contains(mx: Float, my: Float): Boolean {
            return mx in x..(x + width) && my in y..(y + height)
        }

        fun dragTo(mx: Float, my: Float) {
            when {
                dragging -> {
                    x = mx - dragOffsetX
                    y = my - dragOffsetY
                }
                resizing -> {
                    width = max(WINDOW_WIDTH, mx - x)
                    height = max(80f, my - y)
                }
                else -> mouseMoved(mx, my)
            }
        }

        open fun mouseMoved(mx: Float, my: Float) {
            children.firstOrNull { it.contains(mx, my) }?.mouseMoved(mx, my)
        }

        fun stopDrag() {
            dragging = false
            resizing = false
        }

        open fun mouseClicked(mx: Float, my: Float, button: Int) {
            if (my <= y + TITLE_HEIGHT) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    minimized = !minimized
                } else {
                    val now = System.currentTimeMillis()
                    if (now - lastClickTime < 450L) fitHeight()
                    lastClickTime = now
                    dragging = true
                    dragOffsetX = mx - x
                    dragOffsetY = my - y
                }
                return
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && mx > x + width - 8f && my > y + height - 8f) {
                resizing = true
                return
            }
            val child = childAt(mx, my)
            if (child != null) {
                child.mouseClicked(mx, my, button)
            } else {
                closeActiveSettingWindow()
            }
        }

        open fun mouseReleased(mx: Float, my: Float, button: Int) {
            childAt(mx, my)?.mouseReleased(mx, my, button)
        }

        fun scroll(amount: Float) {
            scrollSpeed -= amount * ClickGui.mouseScrollSpeed
        }

        open fun keyPressed(key: Int, scanCode: Int) {
            children.forEach { it.keyPressed(key, scanCode) }
        }

        protected fun childAt(mx: Float, my: Float): LegacyComponent? {
            return children.asSequence().filter { it.effectiveVisible }.firstOrNull { it.contains(mx, my) }
        }

        private fun fitHeight() {
            height = min(NullClickGui.height - y - 2f, TITLE_HEIGHT + MARGIN_Y + children.filter { it.effectiveVisible }.sumOf { (it.height + MARGIN_Y).toDouble() }.toFloat())
        }

        private fun maxScroll(): Float {
            val content = MARGIN_Y + children.filter { it.effectiveVisible }.sumOf { (it.height + MARGIN_Y).toDouble() }.toFloat()
            return max(0f, content - (height - TITLE_HEIGHT))
        }

        private fun clampScroll() {
            val maxScroll = maxScroll()
            scroll = scroll.coerceIn(0f, maxScroll)
        }
    }

    private class ModuleListWindow(
        private val category: Category,
        x: Float,
        y: Float
    ) : LegacyWindow(category.displayName.toString(), x, y, WINDOW_WIDTH, WINDOW_HEIGHT) {
        private val moduleButtons = ModuleManager.getModulesByCategory(category)
            .map { ModuleButton(this, it) }

        init {
            children.addAll(moduleButtons)
        }

        fun filter(needle: String) {
            moduleButtons.forEach {
                it.visible = needle.isEmpty() || it.module.alias.any { alias -> alias.toString().replace(" ", "").contains(needle, true) }
                    || it.module.nameAsString.replace(" ", "").contains(needle, true)
            }
        }

        fun showSettings(module: AbstractModule) {
            val settingWindow = SettingWindow(module, mouseX, mouseY)
            displaySettingWindow(settingWindow)
        }
    }

    private class SettingWindow(
        private val module: AbstractModule,
        x: Float,
        y: Float
    ) : LegacyWindow(module.nameAsString, x, y, 150f, 80f) {
        init {
            module.filteredSettings.forEach { setting ->
                setting.toComponent(this)?.let(children::add)
            }
            height = min(NullClickGui.height - y - 2f, TITLE_HEIGHT + MARGIN_Y + children.filter { it.effectiveVisible }.sumOf { (it.height + MARGIN_Y).toDouble() }.toFloat())
        }

        override fun onDisplayed() {
            super.onDisplayed()
            val maxX = max(0f, NullClickGui.width - width)
            val maxY = max(0f, NullClickGui.height - height)
            super.x = super.x.coerceIn(0f, maxX)
            super.y = super.y.coerceIn(0f, maxY)
        }

        override fun mouseClicked(mx: Float, my: Float, button: Int) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && my <= super.y + TITLE_HEIGHT) {
                closeWindow(this)
                return
            }
            super.mouseClicked(mx, my, button)
        }
    }

    private abstract class LegacyComponent(
        protected val owner: LegacyWindow,
        val label: String,
        val description: String = ""
    ) {
        var visible = true
        val effectiveVisible get() = visible && isVisible()
        var height = ROW_HEIGHT
        protected var x = 0f
        protected var y = 0f
        protected var width = 0f
        private var hovered = false
        private var pressed = false
        private val progressAnim = AnimationFlag(Easing.OUT_QUART, 260f)
        private val hoverAnim = AnimationFlag(Easing.OUT_CUBIC, 160f)

        open val progress: Float get() = 0f

        fun setBounds(x: Float, y: Float, width: Float, height: Float) {
            this.x = x
            this.y = y
            this.width = width
            this.height = height
        }

        open fun onDisplayed() {
            progressAnim.forceUpdate(progress, progress)
            hoverAnim.forceUpdate(0f, 0f)
        }

        fun tick() {
        }

        open fun isVisible() = true

        fun render(context: GuiGraphics) {
            hovered = contains(mouseX, mouseY)
            val hover = hoverAnim.getAndUpdate(if (hovered) 1f else 0f)
            val fillProgress = progressAnim.getAndUpdate(progress.coerceIn(0f, 1f))
            if (fillProgress > 0f) fill(context, x, y, width * fillProgress, height, argb(180, 60, 160, 215))
            fill(context, x, y, width, height, argb((35 + hover * 45).toInt(), 255, 255, 255))
            drawContent(context, hover)
        }

        protected open fun drawContent(context: GuiGraphics, hover: Float) {
            text(context, displayText(), x + 2f + hover * 2f, y + 2f, argb(255, 230, 235, 242))
            valueText()?.let {
                text(context, it, x + width - fontWidth(it) - 2f, y + 2f, argb(255, 165, 220, 255))
            }
        }

        protected open fun displayText() = label
        protected open fun valueText(): String? = null

        open fun mouseMoved(mx: Float, my: Float) {}

        open fun mouseClicked(mx: Float, my: Float, button: Int) {
            pressed = true
        }

        open fun mouseReleased(mx: Float, my: Float, button: Int) {
            if (pressed && contains(mx, my)) activate(button)
            pressed = false
        }

        protected open fun activate(button: Int) {}
        open fun keyPressed(key: Int, scanCode: Int) {}
        open fun charTyped(char: Char) {}
        open fun stopListening(success: Boolean) {}

        fun contains(mx: Float, my: Float) = mx in x..(x + width) && my in y..(y + height)
    }

    private class ModuleButton(
        private val window: ModuleListWindow,
        val module: AbstractModule
    ) : LegacyComponent(window, module.nameAsString, module.description.toString()) {
        override val progress: Float get() = if (module.isEnabled) 1f else 0f

        override fun valueText(): String? = if (module.filteredSettings.any { it.isVisible }) "..." else null

        override fun activate(button: Int) {
            when (button) {
                GLFW.GLFW_MOUSE_BUTTON_LEFT -> {
                    closeActiveSettingWindow()
                    module.toggle()
                }
                GLFW.GLFW_MOUSE_BUTTON_RIGHT -> window.showSettings(module)
            }
        }
    }

    private class BooleanComponent(owner: LegacyWindow, private val setting: BooleanSetting) :
        LegacyComponent(owner, setting.localizedName.toString(), setting.description) {
        override val progress: Float get() = if (setting.value) 1f else 0f
        override fun isVisible() = setting.isVisible
        override fun valueText() = if (setting.value) "On" else "Off"
        override fun activate(button: Int) {
            setting.value = !setting.value
        }
    }

    private class EnumComponent(owner: LegacyWindow, private val setting: EnumSetting<*>) :
        LegacyComponent(owner, setting.localizedName.toString(), setting.description) {
        override fun isVisible() = setting.isVisible
        override fun valueText() = (setting.value as? Displayable)?.displayName?.toString() ?: setting.value.toString()
        override fun activate(button: Int) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) setting.prev() else setting.next()
        }
    }

    private class BindComponent(owner: LegacyWindow, private val setting: BindSetting) :
        LegacyComponent(owner, setting.localizedName.toString(), setting.description) {
        private var listening = false
        override fun isVisible() = setting.isVisible
        override fun valueText() = if (listening) "..." else setting.keyName
        override fun activate(button: Int) {
            listening = true
            listen(this)
        }
        override fun keyPressed(key: Int, scanCode: Int) {
            setting.value = if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) KeyBind.NONE
            else KeyBind(KeyBind.Category.KEYBOARD, key, scanCode)
            stopListening(true)
        }
        override fun stopListening(success: Boolean) {
            listening = false
            clearListening(this)
        }
    }

    private class StringComponent(owner: LegacyWindow, private val setting: StringSetting) :
        LegacyComponent(owner, setting.localizedName.toString(), setting.description) {
        private var listening = false
        override fun isVisible() = setting.isVisible
        override fun valueText() = if (listening) setting.value + "_" else setting.value
        override fun activate(button: Int) {
            listening = true
            listen(this)
        }
        override fun keyPressed(key: Int, scanCode: Int) {
            when (key) {
                GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_ESCAPE -> stopListening(true)
                GLFW.GLFW_KEY_BACKSPACE -> if (setting.value.isNotEmpty()) setting.value = setting.value.dropLast(1)
                GLFW.GLFW_KEY_DELETE -> setting.value = ""
            }
        }
        override fun charTyped(char: Char) {
            if (!char.isISOControl()) setting.value += char
        }
        override fun stopListening(success: Boolean) {
            listening = false
            clearListening(this)
        }
    }

    private class RangedComponent(owner: LegacyWindow, private val setting: AbstractRangedSetting<*, *>) :
        LegacyComponent(owner, setting.localizedName.toString(), setting.description) {
        private var sliding = false
        override val progress: Float get() = ratio(setting)
        override fun isVisible() = setting.isVisible
        override fun valueText() = formatValue(setting.value)
        override fun mouseClicked(mx: Float, my: Float, button: Int) {
            super.mouseClicked(mx, my, button)
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                sliding = true
                update(mx)
            }
        }
        override fun mouseMoved(mx: Float, my: Float) {
            if (sliding) update(mx)
        }
        override fun mouseReleased(mx: Float, my: Float, button: Int) {
            super.mouseReleased(mx, my, button)
            sliding = false
        }
        private fun update(mx: Float) {
            setRanged(setting, ((mx - x) / width).coerceIn(0f, 1f))
        }
    }

    private class ColorComponent(owner: LegacyWindow, private val setting: ColorSetting) :
        LegacyComponent(owner, setting.localizedName.toString(), setting.description) {
        override fun isVisible() = setting.isVisible
        override fun valueText() = "#%02X%02X%02X".format(setting.value.r, setting.value.g, setting.value.b)
        override fun drawContent(context: GuiGraphics, hover: Float) {
            super.drawContent(context, hover)
            fill(context, x + width - 35f, y + 2f, 10f, height - 4f, argb(setting.value))
            stroke(context, x + width - 35f, y + 2f, 10f, height - 4f, argb(255, 230, 230, 230))
        }
        override fun activate(button: Int) {
            displaySettingWindow(ColorWindow(owner, setting, mouseX, mouseY))
        }
    }

    private class ColorWindow(owner: LegacyWindow, private val setting: ColorSetting, x: Float, y: Float) :
        LegacyWindow("Color Picker", x, y, 288f, 152f) {
        private val parent = owner
        private val originalColor = setting.value
        private var red = originalColor.r
        private var green = originalColor.g
        private var blue = originalColor.b
        private var alpha = originalColor.a
        private var hue = 0f
        private var saturation = 1f
        private var brightness = 1f
        private var dragging: DragTarget? = null

        init {
            updateHSBFromRGB()
        }

        override fun onDisplayed() {
            super.onDisplayed()
            val maxX = max(0f, NullClickGui.width - width)
            val maxY = max(0f, NullClickGui.height - height)
            super.x = super.x.coerceIn(0f, maxX)
            super.y = super.y.coerceIn(0f, maxY)
        }

        override fun renderChildren(context: GuiGraphics, rx: Float, baseY: Float, rw: Float) {
            val fieldX = rx + 4f
            val fieldY = baseY + 4f
            val fieldSize = 128f
            val hueX = fieldX + fieldSize + 6f
            val hueW = 8f
            val controlX = hueX + hueW + 10f
            val controlW = 128f

            drawColorField(context, fieldX, fieldY, fieldSize)
            drawHueSlider(context, hueX, fieldY, hueW, fieldSize)

            var y = fieldY
            drawSlider(context, "Red", red, controlX, y, controlW, Channel.RED)
            y += 15f
            drawSlider(context, "Green", green, controlX, y, controlW, Channel.GREEN)
            y += 15f
            drawSlider(context, "Blue", blue, controlX, y, controlW, Channel.BLUE)
            y += 15f
            drawSlider(context, "Alpha", alpha, controlX, y, controlW, Channel.ALPHA)

            val previewY = fieldY + 64f
            drawPreview(context, controlX, previewY, controlW)

            val buttonY = fieldY + 106f
            drawButton(context, "Okay", controlX, buttonY, 40f, 14f)
            drawButton(context, "Cancel", controlX + 44f, buttonY, 40f, 14f)
            drawButton(context, "Apply", controlX + 88f, buttonY, 40f, 14f)
        }

        override fun mouseClicked(mx: Float, my: Float, button: Int) {
            if (my <= super.y + TITLE_HEIGHT) {
                super.mouseClicked(mx, my, button)
                return
            }
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return

            val layout = layout()
            dragging = when {
                inRect(mx, my, layout.fieldX, layout.fieldY, layout.fieldSize, layout.fieldSize) -> DragTarget.FIELD
                inRect(mx, my, layout.hueX, layout.fieldY, layout.hueW, layout.fieldSize) -> DragTarget.HUE
                inRect(mx, my, layout.controlX, layout.redY, layout.controlW, 13f) -> DragTarget.RED
                inRect(mx, my, layout.controlX, layout.greenY, layout.controlW, 13f) -> DragTarget.GREEN
                inRect(mx, my, layout.controlX, layout.blueY, layout.controlW, 13f) -> DragTarget.BLUE
                inRect(mx, my, layout.controlX, layout.alphaY, layout.controlW, 13f) -> DragTarget.ALPHA
                inRect(mx, my, layout.controlX, layout.buttonY, 40f, 14f) -> {
                    applyColor()
                    closeWindow(this)
                    activeWindow = parent
                    null
                }
                inRect(mx, my, layout.controlX + 44f, layout.buttonY, 40f, 14f) -> {
                    closeWindow(this)
                    activeWindow = parent
                    null
                }
                inRect(mx, my, layout.controlX + 88f, layout.buttonY, 40f, 14f) -> {
                    applyColor()
                    null
                }
                else -> null
            }
            dragging?.let { updateDrag(it, mx, my, layout) }
        }

        override fun mouseMoved(mx: Float, my: Float) {
            val target = dragging ?: return
            updateDrag(target, mx, my, layout())
        }

        override fun mouseReleased(mx: Float, my: Float, button: Int) {
            dragging = null
            super.mouseReleased(mx, my, button)
        }

        override fun onClosed() {
            dragging = null
            super.onClosed()
        }

        private fun drawColorField(context: GuiGraphics, x: Float, y: Float, size: Float) {
            val steps = size.roundToInt().coerceAtLeast(1)
            for (i in 0 until steps) {
                val sat = i / (steps - 1f).coerceAtLeast(1f)
                val color = ColorUtils.hsbToRGB(hue, sat, 1f, 1f)
                fill(context, x + i, y, 1f, size, argb(255, color.r, color.g, color.b))
            }
            for (i in 0 until steps) {
                val darkness = (i / (steps - 1f).coerceAtLeast(1f) * 255f).roundToInt()
                fill(context, x, y + i, size, 1f, argb(darkness, 0, 0, 0))
            }
            stroke(context, x, y, size, size, argb(210, 235, 245, 255))

            val pointerX = x + saturation * size
            val pointerY = y + (1f - brightness) * size
            stroke(context, pointerX - 3f, pointerY - 3f, 6f, 6f, argb(255, 20, 20, 20))
            stroke(context, pointerX - 2f, pointerY - 2f, 4f, 4f, argb(255, 245, 245, 245))
        }

        private fun drawHueSlider(context: GuiGraphics, x: Float, y: Float, w: Float, h: Float) {
            val steps = h.roundToInt().coerceAtLeast(1)
            for (i in 0 until steps) {
                val color = ColorUtils.hsbToRGB(i / (steps - 1f).coerceAtLeast(1f), 1f, 1f, 1f)
                fill(context, x, y + i, w, 1f, argb(255, color.r, color.g, color.b))
            }
            stroke(context, x, y, w, h, argb(210, 235, 245, 255))
            val pointerY = y + hue * h
            fill(context, x - 4f, pointerY - 1f, 3f, 2f, argb(255, 245, 250, 255))
            fill(context, x + w + 1f, pointerY - 1f, 3f, 2f, argb(255, 245, 250, 255))
        }

        private fun drawSlider(context: GuiGraphics, label: String, value: Int, x: Float, y: Float, w: Float, channel: Channel) {
            val ratio = value / 255f
            fill(context, x, y, w, 13f, argb(55, 255, 255, 255))
            fill(context, x, y, w * ratio, 13f, sliderColor(channel))
            stroke(context, x, y, w, 13f, argb(110, 235, 245, 255))
            text(context, label, x + 3f, y + 2f, argb(255, 230, 235, 242))
            val valueText = value.toString()
            text(context, valueText, x + w - fontWidth(valueText) - 3f, y + 2f, argb(255, 235, 245, 255))
        }

        private fun drawPreview(context: GuiGraphics, x: Float, y: Float, w: Float) {
            text(context, "Previous", x, y, argb(255, 210, 225, 235))
            text(context, "Current", x + 66f, y, argb(255, 210, 225, 235))
            drawChecker(context, x, y + 12f, 58f, 26f)
            drawChecker(context, x + 66f, y + 12f, 58f, 26f)
            fill(context, x, y + 12f, 58f, 26f, argb(originalColor))
            fill(context, x + 66f, y + 12f, 58f, 26f, argb(currentColor()))
            stroke(context, x, y + 12f, 58f, 26f, argb(150, 235, 245, 255))
            stroke(context, x + 66f, y + 12f, 58f, 26f, argb(150, 235, 245, 255))
        }

        private fun drawButton(context: GuiGraphics, label: String, x: Float, y: Float, w: Float, h: Float) {
            val hovered = inRect(mouseX, mouseY, x, y, w, h)
            fill(context, x, y, w, h, if (hovered) argb(170, 55, 155, 210) else argb(100, 35, 95, 140))
            stroke(context, x, y, w, h, argb(170, 120, 210, 255))
            text(context, label, x + (w - fontWidth(label)) / 2f, y + 3f, argb(255, 245, 250, 255))
        }

        private fun drawChecker(context: GuiGraphics, x: Float, y: Float, w: Float, h: Float) {
            val size = 4f
            var iy = 0
            while (iy * size < h) {
                var ix = 0
                while (ix * size < w) {
                    val color = if ((ix + iy) % 2 == 0) argb(255, 70, 74, 82) else argb(255, 42, 46, 54)
                    fill(context, x + ix * size, y + iy * size, min(size, w - ix * size), min(size, h - iy * size), color)
                    ix++
                }
                iy++
            }
        }

        private fun sliderColor(channel: Channel): Int {
            return when (channel) {
                Channel.RED -> argb(180, 255, 70, 80)
                Channel.GREEN -> argb(180, 85, 220, 120)
                Channel.BLUE -> argb(180, 80, 150, 255)
                Channel.ALPHA -> argb(max(80, alpha), red, green, blue)
            }
        }

        private fun updateDrag(target: DragTarget, mx: Float, my: Float, layout: PickerLayout) {
            when (target) {
                DragTarget.FIELD -> {
                    saturation = ((mx - layout.fieldX) / layout.fieldSize).coerceIn(0f, 1f)
                    brightness = (1f - (my - layout.fieldY) / layout.fieldSize).coerceIn(0f, 1f)
                    updateRGBFromHSB()
                }
                DragTarget.HUE -> {
                    hue = ((my - layout.fieldY) / layout.fieldSize).coerceIn(0f, 1f)
                    updateRGBFromHSB()
                }
                DragTarget.RED -> {
                    red = sliderValue(mx, layout)
                    updateHSBFromRGB()
                }
                DragTarget.GREEN -> {
                    green = sliderValue(mx, layout)
                    updateHSBFromRGB()
                }
                DragTarget.BLUE -> {
                    blue = sliderValue(mx, layout)
                    updateHSBFromRGB()
                }
                DragTarget.ALPHA -> alpha = sliderValue(mx, layout)
            }
        }

        private fun sliderValue(mx: Float, layout: PickerLayout): Int {
            return (((mx - layout.controlX) / layout.controlW).coerceIn(0f, 1f) * 255f).roundToInt()
        }

        private fun applyColor() {
            setting.value = currentColor()
        }

        private fun currentColor() = ColorRGBA(red, green, blue, alpha)

        private fun updateRGBFromHSB() {
            val color = ColorUtils.hsbToRGB(hue, saturation, brightness, alpha / 255f)
            red = color.r
            green = color.g
            blue = color.b
        }

        private fun updateHSBFromRGB() {
            val hsb = ColorUtils.rgbToHSB(red, green, blue, alpha)
            hue = hsb.h
            saturation = hsb.s
            brightness = hsb.b
        }

        private fun layout(): PickerLayout {
            val fieldX = super.x + 4f
            val fieldY = super.y + TITLE_HEIGHT + 4f
            val fieldSize = 128f
            val hueX = fieldX + fieldSize + 6f
            val hueW = 8f
            val controlX = hueX + hueW + 10f
            val controlW = 128f
            return PickerLayout(
                fieldX,
                fieldY,
                fieldSize,
                hueX,
                hueW,
                controlX,
                controlW,
                fieldY,
                fieldY + 15f,
                fieldY + 30f,
                fieldY + 45f,
                fieldY + 106f
            )
        }

        private enum class DragTarget {
            FIELD, HUE, RED, GREEN, BLUE, ALPHA
        }

        private enum class Channel {
            RED, GREEN, BLUE, ALPHA
        }

        private data class PickerLayout(
            val fieldX: Float,
            val fieldY: Float,
            val fieldSize: Float,
            val hueX: Float,
            val hueW: Float,
            val controlX: Float,
            val controlW: Float,
            val redY: Float,
            val greenY: Float,
            val blueY: Float,
            val alphaY: Float,
            val buttonY: Float
        )

        private fun inRect(mx: Float, my: Float, x: Float, y: Float, w: Float, h: Float): Boolean {
            return mx in x..(x + w) && my in y..(y + h)
        }
    }

    private fun AbstractSetting<*, *>.toComponent(owner: LegacyWindow): LegacyComponent? {
        return when (this) {
            is BooleanSetting -> BooleanComponent(owner, this)
            is AbstractRangedSetting<*, *> -> RangedComponent(owner, this)
            is EnumSetting<*> -> EnumComponent(owner, this)
            is BindSetting -> BindComponent(owner, this)
            is StringSetting -> StringComponent(owner, this)
            is ColorSetting -> ColorComponent(owner, this)
            else -> null
        }
    }

    private fun ratio(setting: AbstractRangedSetting<*, *>): Float {
        return when (val value = setting.value) {
            is Int -> ((value - setting.range.start as Int).toFloat() / ((setting.range.endInclusive as Int) - setting.range.start as Int)).coerceIn(0f, 1f)
            is Long -> ((value - setting.range.start as Long).toFloat() / ((setting.range.endInclusive as Long) - setting.range.start as Long)).coerceIn(0f, 1f)
            is Float -> ((value - setting.range.start as Float) / ((setting.range.endInclusive as Float) - setting.range.start as Float)).coerceIn(0f, 1f)
            is Double -> ((value - setting.range.start as Double) / ((setting.range.endInclusive as Double) - setting.range.start as Double)).toFloat().coerceIn(0f, 1f)
            else -> 0f
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun setRanged(setting: AbstractRangedSetting<*, *>, ratio: Float) {
        when (setting.value) {
            is Int -> (setting as AbstractRangedSetting<Int, *>).value =
                lerp((setting.range.start as Int).toDouble(), (setting.range.endInclusive as Int).toDouble(), ratio).roundToInt()
            is Long -> (setting as AbstractRangedSetting<Long, *>).value =
                lerp((setting.range.start as Long).toDouble(), (setting.range.endInclusive as Long).toDouble(), ratio).roundToInt().toLong()
            is Float -> (setting as AbstractRangedSetting<Float, *>).value =
                lerp((setting.range.start as Float).toDouble(), (setting.range.endInclusive as Float).toDouble(), ratio).toFloat()
            is Double -> (setting as AbstractRangedSetting<Double, *>).value =
                lerp(setting.range.start as Double, setting.range.endInclusive as Double, ratio)
        }
    }

    private fun formatValue(value: Any?) = when (value) {
        is Float -> "%.2f".format(value)
        is Double -> "%.2f".format(value)
        else -> value.toString()
    }

    private fun lerp(start: Double, end: Double, ratio: Float) = start + (end - start) * ratio

    private fun fill(context: GuiGraphics, x: Float, y: Float, w: Float, h: Float, color: Int) {
        context.fill(x.roundToInt(), y.roundToInt(), (x + w).roundToInt(), (y + h).roundToInt(), color)
    }

    private fun stroke(context: GuiGraphics, x: Float, y: Float, w: Float, h: Float, color: Int) {
        fill(context, x, y, w, 1f, color)
        fill(context, x, y + h - 1f, w, 1f, color)
        fill(context, x, y, 1f, h, color)
        fill(context, x + w - 1f, y, 1f, h, color)
    }

    private fun text(context: GuiGraphics, text: String, x: Float, y: Float, color: Int, scale: Float = 1f) {
        textQueue.add(TextDraw(text, x, y, color, scale))
        if (scale == 1f) {
            context.drawString(mc.font, text, x.roundToInt(), y.roundToInt(), color)
        } else {
            context.pose().pushMatrix()
            context.pose().translate(x, y)
            context.pose().scale(scale, scale)
            context.drawString(mc.font, text, 0, 0, color)
            context.pose().popMatrix()
        }
    }

    private fun fontWidth(text: String): Float = fontFor(text)?.getWidth(text) ?: mc.font.width(text).toFloat()
    private fun fontFor(text: String): FontRenderer? = try {
        if (text.all { it.code in 32..255 }) UnicodeFontManager.LEGACY_9 else UnicodeFontManager.MSYAHEI_9
    } catch (_: UninitializedPropertyAccessException) {
        null
    }

    private fun awt(color: Int) = Color(
        color shr 16 and 255,
        color shr 8 and 255,
        color and 255,
        color ushr 24 and 255
    )

    private data class TextDraw(
        val text: String,
        val x: Float,
        val y: Float,
        val color: Int,
        val scale: Float
    )
    private fun argb(color: ColorRGBA) = argb(color.a, color.r, color.g, color.b)
    private fun argb(a: Int, r: Int, g: Int, b: Int): Int {
        return ((a.coerceIn(0, 255) and 255) shl 24) or
            ((r.coerceIn(0, 255) and 255) shl 16) or
            ((g.coerceIn(0, 255) and 255) shl 8) or
            (b.coerceIn(0, 255) and 255)
    }
}
