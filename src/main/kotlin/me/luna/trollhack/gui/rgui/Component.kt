package me.luna.trollhack.gui.rgui

import me.luna.trollhack.TrollHackMod
import me.luna.trollhack.module.modules.client.GuiSetting
import me.luna.trollhack.setting.GuiConfig
import me.luna.trollhack.setting.GuiConfig.setting
import me.luna.trollhack.setting.configs.AbstractConfig
import me.luna.trollhack.util.Wrapper
import me.luna.trollhack.util.delegate.FrameValue
import me.luna.trollhack.util.graphics.HAlign
import me.luna.trollhack.util.graphics.RenderUtils3D
import me.luna.trollhack.util.graphics.VAlign
import me.luna.trollhack.util.interfaces.Nameable
import me.luna.trollhack.util.math.MathUtils
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

    var width by setting("Width", widthIn, 0.0f..69420.911f, 0.1f, { false }, { _, it -> it.coerceIn(minWidth, max(scaledDisplayWidth, minWidth)) })
    var height by setting("Height", heightIn, 0.0f..69420.911f, 0.1f, { false }, { _, it -> it.coerceIn(minHeight, max(scaledDisplayHeight, minHeight)) })

    protected var relativePosX by setting("Pos X", posXIn, -69420.911f..69420.911f, 0.1f, { false },
        { _, it -> if (this is WindowComponent && TrollHackMod.ready) absToRelativeX(relativeToAbsX(it).coerceIn(0.0f, max(scaledDisplayWidth - width, 0.0f))) else it })
    protected var relativePosY by setting("Pos Y", posYIn, -69420.911f..69420.911f, 0.1f, { false },
        { _, it -> if (this is WindowComponent && TrollHackMod.ready) absToRelativeY(relativeToAbsY(it).coerceIn(0.0f, max(scaledDisplayHeight - height, 0.0f))) else it })

    var dockingH by dockingHSetting
    var dockingV by dockingVSetting

    var posX: Float
        get() {
            return relativeToAbsX(relativePosX)
        }
        set(value) {
            if (!TrollHackMod.ready) return
            relativePosX = absToRelativeX(value)
        }

    var posY: Float
        get() {
            return relativeToAbsY(relativePosY)
        }
        set(value) {
            if (!TrollHackMod.ready) return
            relativePosY = absToRelativeY(value)
        }

    init {
        dockingHSetting.listeners.add { posX = prevPosX }
        dockingVSetting.listeners.add { posY = prevPosY }
    }

    // Extra info
    protected val mc = Wrapper.minecraft
    open val minWidth = 1.0f
    open val minHeight = 1.0f
    open val maxWidth = -1.0f
    open val maxHeight = -1.0f
    open val closeable: Boolean get() = true

    // Rendering info
    var prevPosX = 0.0f; protected set
    var prevPosY = 0.0f; protected set
    val renderPosX by FrameValue { MathUtils.lerp(prevPosX + prevDockWidth, posX + dockWidth, RenderUtils3D.partialTicks) - dockWidth }
    val renderPosY by FrameValue { MathUtils.lerp(prevPosY + prevDockHeight, posY + dockHeight, RenderUtils3D.partialTicks) - dockHeight }

    var prevWidth = 0.0f; protected set
    var prevHeight = 0.0f; protected set
    val renderWidth by FrameValue { MathUtils.lerp(prevWidth, width, RenderUtils3D.partialTicks) }
    open val renderHeight by FrameValue { MathUtils.lerp(prevHeight, height, RenderUtils3D.partialTicks) }

    private fun relativeToAbsX(xIn: Float) = xIn + scaledDisplayWidth * dockingH.multiplier - dockWidth
    private fun relativeToAbsY(yIn: Float) = yIn + scaledDisplayHeight * dockingV.multiplier - dockHeight
    private fun absToRelativeX(xIn: Float) = xIn - scaledDisplayWidth * dockingH.multiplier + dockWidth
    private fun absToRelativeY(yIn: Float) = yIn - scaledDisplayHeight * dockingV.multiplier + dockHeight

    protected val scaledDisplayWidth get() = mc.displayWidth / GuiSetting.scaleFactorFloat
    protected val scaledDisplayHeight get() = mc.displayHeight / GuiSetting.scaleFactorFloat
    private val dockWidth get() = width * dockingH.multiplier
    private val dockHeight get() = height * dockingV.multiplier
    private val prevDockWidth get() = prevWidth * dockingH.multiplier
    private val prevDockHeight get() = prevHeight * dockingV.multiplier

    // Update methods
    open fun onDisplayed() {
        updatePrevPos()
        updatePrevSize()
    }

    open fun onClosed() {}

    open fun onGuiInit() {}

    open fun onTick() {
        updatePrevPos()
        updatePrevSize()
    }

    private fun updatePrevPos() {
        prevPosX = posX
        prevPosY = posY
    }

    private fun updatePrevSize() {
        prevWidth = width
        prevHeight = height
    }

    open fun onRender(absolutePos: Vec2f) {}

    open fun onPostRender(absolutePos: Vec2f) {}

    enum class SettingGroup(val groupName: String) {
        NONE(""),
        CLICK_GUI("click_gui"),
        HUD_GUI("hud_gui")
    }

}