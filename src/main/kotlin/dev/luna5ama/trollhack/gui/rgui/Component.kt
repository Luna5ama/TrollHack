package dev.luna5ama.trollhack.gui.rgui

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.setting.GuiConfig
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.setting.configs.AbstractConfig
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.delegate.FrameValue
import dev.luna5ama.trollhack.util.graphics.AnimationFlag
import dev.luna5ama.trollhack.util.graphics.Easing
import dev.luna5ama.trollhack.util.graphics.HAlign
import dev.luna5ama.trollhack.util.graphics.VAlign
import dev.luna5ama.trollhack.util.interfaces.Nameable
import dev.luna5ama.trollhack.util.math.vector.Vec2f
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
            if (this is WindowComponent && TrollHackMod.ready) a2rX(
                r2aX(it).coerceIn(
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
            if (this is WindowComponent && TrollHackMod.ready) a2rY(
                r2aY(it).coerceIn(
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

    open var posX: Float
        get() {
            return r2aX(relativePosX)
        }
        set(value) {
            if (!TrollHackMod.ready) return
            relativePosX = a2rX(value)
        }

    open var posY: Float
        get() {
            return r2aY(relativePosY)
        }
        set(value) {
            if (!TrollHackMod.ready) return
            relativePosY = a2rY(value)
        }

    open var width: Float
        get() = widthSetting
        set(value) {
            widthSetting = value
        }

    open var height: Float
        get() = heightSetting
        set(value) {
            heightSetting = value
        }

    init {
        dockingHSetting.valueListeners.add { prev, current ->
            relativePosX = a2rX(r2aX(relativePosX, prev), current)
            renderPosXFlag.forceUpdate(relativePosX, relativePosX)
        }
        dockingVSetting.valueListeners.add { prev, current ->
            relativePosY = a2rY(r2aY(relativePosY, prev), current)
            renderPosYFlag.forceUpdate(relativePosY, relativePosY)
        }
    }

    // Extra info
    protected val mc = Wrapper.minecraft
    open val minWidth = 1.0f
    open val minHeight = 1.0f
    open val maxWidth = -1.0f
    open val maxHeight = -1.0f
    open val closeable: Boolean get() = true

    // Rendering info
    val renderPosXFlag = AnimationFlag { time, prev, current ->
        r2aX(Easing.OUT_CUBIC.incOrDec(Easing.toDelta(time, 200.0f), prev, current))
    }
    val renderPosYFlag = AnimationFlag { time, prev, current ->
        r2aY(Easing.OUT_CUBIC.incOrDec(Easing.toDelta(time, 200.0f), prev, current))
    }
    val renderWidthFlag = AnimationFlag(Easing.OUT_CUBIC, 200.0f)
    val renderHeightFlag = AnimationFlag(Easing.OUT_CUBIC, 200.0f)

    open val renderPosX by FrameValue(renderPosXFlag::get)
    open val renderPosY by FrameValue(renderPosYFlag::get)
    open val renderWidth by FrameValue(renderWidthFlag::get)
    open val renderHeight by FrameValue(renderHeightFlag::get)

    private fun r2aX(x: Float, docking: HAlign) = x + scaledDisplayWidth * docking.multiplier - dockWidth(docking)
    private fun r2aY(y: Float, docking: VAlign) = y + scaledDisplayHeight * docking.multiplier - dockHeight(docking)
    private fun a2rX(x: Float, docking: HAlign) = x - scaledDisplayWidth * docking.multiplier + dockWidth(docking)
    private fun a2rY(y: Float, docking: VAlign) = y - scaledDisplayHeight * docking.multiplier + dockHeight(docking)

    private fun r2aX(x: Float) = r2aX(x, dockingH)
    private fun r2aY(y: Float) = r2aY(y, dockingV)
    private fun a2rX(x: Float) = a2rX(x, dockingH)
    private fun a2rY(y: Float) = a2rY(y, dockingV)

    private fun dockWidth(docking: HAlign) = width * docking.multiplier
    private fun dockHeight(docking: VAlign) = height * docking.multiplier

    protected val scaledDisplayWidth get() = mc.displayWidth / GuiSetting.scaleFactorFloat
    protected val scaledDisplayHeight get() = mc.displayHeight / GuiSetting.scaleFactorFloat

    // Update methods
    open fun onDisplayed() {
        renderPosXFlag.forceUpdate(relativePosX, relativePosX)
        renderPosYFlag.forceUpdate(relativePosY, relativePosY)
        renderWidthFlag.forceUpdate(width, width)
        renderHeightFlag.forceUpdate(height, height)
    }

    open fun onClosed() {}

    open fun onGuiInit() {}

    open fun onTick() {
        renderPosXFlag.update(relativePosX)
        renderPosYFlag.update(relativePosY)
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