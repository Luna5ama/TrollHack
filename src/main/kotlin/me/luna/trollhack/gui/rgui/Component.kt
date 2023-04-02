package me.luna.trollhack.gui.rgui

import me.luna.trollhack.TrollHackMod
import me.luna.trollhack.module.modules.client.GuiSetting
import me.luna.trollhack.setting.GuiConfig
import me.luna.trollhack.setting.GuiConfig.setting
import me.luna.trollhack.setting.configs.AbstractConfig
import me.luna.trollhack.util.Wrapper
import me.luna.trollhack.util.delegate.FrameValue
import me.luna.trollhack.util.graphics.*
import me.luna.trollhack.util.interfaces.Nameable
import me.luna.trollhack.util.math.vector.Vec2f
import kotlin.math.max

open class Component(
    final override val name: CharSequence,
    posXIn: Float,
    posYIn: Float,
    widthIn: Float,
    heightIn: Float,
    val settingGroup: SettingGroup,
    val config: AbstractConfig<out Nameable> = GuiConfig
) : Nameable {

    // Basic info
    protected val visibleSetting = setting("Visible", true, { false }, { _, it -> it || !closeable })
    var visible by visibleSetting

    protected val dockingHSetting = setting("Docking H", HAlign.LEFT)
    protected val dockingVSetting = setting("Docking V", VAlign.TOP)

    protected var widthSetting by setting(
        "Width",
        widthIn,
        0.0f..69420.911f,
        0.1f,
        { false },
        { _, it -> it.coerceIn(minWidth, max(scaledDisplayWidth, minWidth)) }).apply {
        valueListeners.add { _, it ->
            renderWidthFlag.update(it)
        }
    }
    protected var heightSetting by setting(
        "Height",
        heightIn,
        0.0f..69420.911f,
        0.1f,
        { false },
        { _, it -> it.coerceIn(minHeight, max(scaledDisplayHeight, minHeight)) }).apply {
        valueListeners.add { _, it ->
            renderHeightFlag.update(it)
        }
    }

    protected var relativePosX by setting("Pos X", posXIn, -69420.911f..69420.911f, 0.1f, { false },
        { _, it ->
            if (this is WindowComponent && TrollHackMod.ready) absToRelativeX(
                relativeToAbsX(it).coerceIn(
                    0.0f,
                    max(scaledDisplayWidth - widthSetting, 0.0f)
                )
            ) else it
        }).apply {
        valueListeners.add { _, it ->
            renderPosXFlag.update(it)
        }
    }
    protected var relativePosY by setting("Pos Y", posYIn, -69420.911f..69420.911f, 0.1f, { false },
        { _, it ->
            if (this is WindowComponent && TrollHackMod.ready) absToRelativeY(
                relativeToAbsY(it).coerceIn(
                    0.0f,
                    max(scaledDisplayHeight - heightSetting, 0.0f)
                )
            ) else it
        }).apply {
        valueListeners.add { _, it ->
            renderPosYFlag.update(it)
        }
    }

    var dockingH by dockingHSetting
    var dockingV by dockingVSetting

    open  var posX: Float
        get() {
            return relativeToAbsX(relativePosX)
        }
        set(value) {
            if (!TrollHackMod.ready) return
            relativePosX = absToRelativeX(value)
        }

    open  var posY: Float
        get() {
            return relativeToAbsY(relativePosY)
        }
        set(value) {
            if (!TrollHackMod.ready) return
            relativePosY = absToRelativeY(value)
        }

    open var width: Float
        get() = widthSetting
        set(value) {
            widthSetting = value
        }

    open  var height: Float
        get() = heightSetting
        set(value) {
            heightSetting = value
        }

    init {
        dockingHSetting.listeners.add { posX = renderPosXFlag.prev }
        dockingVSetting.listeners.add { posY = renderPosYFlag.prev }
    }

    // Extra info
    protected val mc = Wrapper.minecraft
    open val minWidth = 1.0f
    open val minHeight = 1.0f
    open val maxWidth = -1.0f
    open val maxHeight = -1.0f
    open val closeable: Boolean get() = true

    // Rendering info
    val renderPosXFlag = AnimationFlag(Easing.OUT_CUBIC, 200.0f)
    val renderPosYFlag = AnimationFlag(Easing.OUT_CUBIC, 200.0f)
    val renderWidthFlag = AnimationFlag(Easing.OUT_CUBIC, 200.0f)
    val renderHeightFlag = AnimationFlag(Easing.OUT_CUBIC, 200.0f)

    open val renderPosX by FrameValue(renderPosXFlag::get)
    open val renderPosY by FrameValue(renderPosYFlag::get)
    open val renderWidth by FrameValue(renderWidthFlag::get)
    open val renderHeight by FrameValue(renderHeightFlag::get)

    private fun relativeToAbsX(xIn: Float) = xIn + scaledDisplayWidth * dockingH.multiplier - dockWidth
    private fun relativeToAbsY(yIn: Float) = yIn + scaledDisplayHeight * dockingV.multiplier - dockHeight
    private fun absToRelativeX(xIn: Float) = xIn - scaledDisplayWidth * dockingH.multiplier + dockWidth
    private fun absToRelativeY(yIn: Float) = yIn - scaledDisplayHeight * dockingV.multiplier + dockHeight

    protected val scaledDisplayWidth get() = mc.displayWidth / GuiSetting.scaleFactorFloat
    protected val scaledDisplayHeight get() = mc.displayHeight / GuiSetting.scaleFactorFloat
    private val dockWidth get() = width * dockingH.multiplier
    private val dockHeight get() = height * dockingV.multiplier

    // Update methods
    open fun onDisplayed() {
        renderPosXFlag.forceUpdate(posX, posX)
        renderPosYFlag.forceUpdate(posY, posY)
        renderWidthFlag.forceUpdate(width, width)
        renderHeightFlag.forceUpdate(height, height)
    }

    open fun onClosed() {}

    open fun onGuiInit() {}

    open fun onTick() {
        renderPosXFlag.update(posX)
        renderPosYFlag.update(posY)
        renderWidthFlag.update(width)
        renderHeightFlag.update(height)
    }

    open fun onRender(absolutePos: Vec2f) {}

    open fun onPostRender(absolutePos: Vec2f) {}

    enum class SettingGroup(val groupName: String) {
        NONE(""),
        CLICK_GUI("click_gui"),
        HUD_GUI("hud_gui")
    }

}