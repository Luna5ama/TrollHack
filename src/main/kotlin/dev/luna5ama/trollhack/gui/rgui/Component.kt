package dev.luna5ama.trollhack.gui.rgui

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.graphics.AnimationFlag
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.setting.GuiConfig
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.setting.configs.AbstractConfig
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.delegate.FrameFloat
import dev.luna5ama.trollhack.util.extension.rootName
import dev.luna5ama.trollhack.util.interfaces.Nameable
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import kotlin.math.max

open class Component(
    open val screen: IGuiScreen,
    final override val name: CharSequence,
    val uiSettingGroup: UiSettingGroup,
    val config: AbstractConfig<out Nameable> = GuiConfig
) : Nameable {
    override val internalName = rootName.replace(" ", "")

    val settingGroup get() = config.getGroupOrPut(uiSettingGroup.groupName).getGroupOrPut(internalName)

    // Basic info
    protected val visibleSetting = setting("Visible", true, { false }, { _, it -> it || !closeable })
    var visible by visibleSetting

    protected val dockingHSetting = setting("Docking H", dev.luna5ama.trollhack.graphics.HAlign.LEFT)
    protected val dockingVSetting = setting("Docking V", dev.luna5ama.trollhack.graphics.VAlign.TOP)

    protected var widthSetting by setting(
        "Width",
        1.0f,
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
        1.0f,
        0.0f..69420.911f,
        0.1f,
        { false },
        { _, it -> it.coerceIn(minHeight, max(scaledDisplayHeight, minHeight)) }).apply {
        valueListeners.add { _, it ->
            renderHeightFlag.update(it)
        }
    }

    protected var relativePosX by setting("Pos X", 0.0f, -69420.911f..69420.911f, 0.1f, { false },
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
    protected var relativePosY by setting("Pos Y", 0.0f, -69420.911f..69420.911f, 0.1f, { false },
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

    var forcePosX: Float
        get() = posX
        set(value) {
            posX = value
            renderPosXFlag.forceUpdate(relativePosX, relativePosX)
        }

    open var posY: Float
        get() {
            return r2aY(relativePosY)
        }
        set(value) {
            if (!TrollHackMod.ready) return
            relativePosY = a2rY(value)
        }

    var forcePosY: Float
        get() = posY
        set(value) {
            posY = value
            renderPosYFlag.forceUpdate(relativePosY, relativePosY)
        }

    open var width: Float
        get() {
            var value = widthSetting
            val maxWidth = maxWidth
            if (maxWidth != -1.0f && value > maxWidth) value = maxWidth
            if (value < minWidth) value = minWidth
            widthSetting = value
            return value
        }
        set(value) {
            widthSetting = value
        }

    var forceWidth: Float
        get() = width
        set(value) {
            width = value
            renderWidthFlag.forceUpdate(widthSetting, widthSetting)
        }

    open var height: Float
        get() {
            var value = heightSetting
            val maxHeight = maxHeight
            if (maxHeight != -1.0f && value > maxHeight) value = maxHeight
            if (value < minHeight) value = minHeight
            heightSetting = value
            return value
        }
        set(value) {
            heightSetting = value
        }

    var forceHeight: Float
        get() = height
        set(value) {
            height = value
            renderHeightFlag.forceUpdate(heightSetting, heightSetting)
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
    open val closeable get() = true

    // Rendering info
    private val renderPosXFlag = AnimationFlag { time, prev, current ->
        r2aX(Easing.OUT_CUBIC.incOrDec(Easing.toDelta(time, 200.0f), prev, current))
    }
    private val renderPosYFlag = AnimationFlag { time, prev, current ->
        r2aY(Easing.OUT_CUBIC.incOrDec(Easing.toDelta(time, 200.0f), prev, current))
    }
    private val renderWidthFlag = AnimationFlag(Easing.OUT_CUBIC, 200.0f)
    private val renderHeightFlag = AnimationFlag(Easing.OUT_CUBIC, 200.0f)

    open val renderPosX by FrameFloat(renderPosXFlag::get)
    open val renderPosY by FrameFloat(renderPosYFlag::get)
    open val renderWidth by FrameFloat(renderWidthFlag::get)
    open val renderHeight by FrameFloat(renderHeightFlag::get)

    private fun r2aX(x: Float, docking: dev.luna5ama.trollhack.graphics.HAlign) = x + scaledDisplayWidth * docking.multiplier - dockWidth(docking)
    private fun r2aY(y: Float, docking: dev.luna5ama.trollhack.graphics.VAlign) = y + scaledDisplayHeight * docking.multiplier - dockHeight(docking)
    private fun a2rX(x: Float, docking: dev.luna5ama.trollhack.graphics.HAlign) = x - scaledDisplayWidth * docking.multiplier + dockWidth(docking)
    private fun a2rY(y: Float, docking: dev.luna5ama.trollhack.graphics.VAlign) = y - scaledDisplayHeight * docking.multiplier + dockHeight(docking)

    private fun r2aX(x: Float) = r2aX(x, dockingH)
    private fun r2aY(y: Float) = r2aY(y, dockingV)
    private fun a2rX(x: Float) = a2rX(x, dockingH)
    private fun a2rY(y: Float) = a2rY(y, dockingV)

    private fun dockWidth(docking: dev.luna5ama.trollhack.graphics.HAlign) = width * docking.multiplier
    private fun dockHeight(docking: dev.luna5ama.trollhack.graphics.VAlign) = height * docking.multiplier

    protected val scaledDisplayWidth get() = mc.displayWidth / GuiSetting.scaleFactor
    protected val scaledDisplayHeight get() = mc.displayHeight / GuiSetting.scaleFactor

    // Update methods
    open fun onGuiDisplayed() {
        onDisplayed()
    }

    open fun onGuiClosed() {
        onClosed()
    }

    open fun onDisplayed() {
        renderPosXFlag.forceUpdate(relativePosX, relativePosX)
        renderPosYFlag.forceUpdate(relativePosY, relativePosY)
        renderWidthFlag.forceUpdate(width, width)
        renderHeightFlag.forceUpdate(height, height)
    }

    open fun onClosed() {}

    open fun onTick() {
        renderPosXFlag.update(relativePosX)
        renderPosYFlag.update(relativePosY)
        renderWidthFlag.update(width)
        renderHeightFlag.update(height)
    }

    open fun onRender(absolutePos: Vec2f) {}

    open fun onPostRender(absolutePos: Vec2f) {}

    enum class UiSettingGroup(val groupName: String) {
        NONE(""),
        CLICK_GUI("click_gui"),
        HUD_GUI("hud_gui")
    }
}