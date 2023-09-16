package dev.luna5ama.trollhack.gui.rgui

import dev.luna5ama.trollhack.graphics.AnimationFlag
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.gui.rgui.windows.DockingOverlay
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.setting.configs.AbstractConfig
import dev.luna5ama.trollhack.util.delegate.FrameFloat
import dev.luna5ama.trollhack.util.interfaces.Nameable
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import dev.luna5ama.trollhack.util.threads.runSynchronized
import kotlin.math.max
import kotlin.math.min

open class WindowComponent(
    screen: IGuiScreen,
    name: CharSequence,
    uiSettingGroup: UiSettingGroup,
    config: AbstractConfig<out Nameable>
) : InteractiveComponent(screen, name, uiSettingGroup, config) {

    // Basic info
    private val minimizedSetting = setting("Minimized", false,
        { false }, { _, input -> System.currentTimeMillis() - renderMinimizeProgressFlag.time > 300L && input }
    )
    var minimized by minimizedSetting

    // Interactive info
    open var keybordListening: InteractiveComponent? = null
    open val draggableHeight get() = height
    var lastActiveTime: Long = System.currentTimeMillis(); protected set
    var preDragMousePos = Vec2f.ZERO; private set
    var preDragPos = Vec2f.ZERO; private set
    var preDragSize = Vec2f.ZERO; private set

    // Render info
    private val renderMinimizeProgressFlag = AnimationFlag(Easing.OUT_QUART, 300.0f)
    val renderMinimizeProgress by FrameFloat(renderMinimizeProgressFlag::get)

    override val renderHeight: Float
        get() = (super.renderHeight - draggableHeight) * renderMinimizeProgress + draggableHeight

    open val resizable get() = true
    open val minimizable get() = false

    init {
        minimizedSetting.valueListeners.add { _, it ->
            renderMinimizeProgressFlag.update(if (it) 0.0f else 1.0f)
        }
    }

    private val dockingOverlay by lazy { DockingOverlay(screen, this) }

    open fun onResize() {}
    open fun onReposition() {}

    override fun onTick() {
        super.onTick()
        if (mouseState != MouseState.DRAG) {
            updatePreDrag(null)
        }
    }

    override fun onDisplayed() {
        lastActiveTime = System.currentTimeMillis() + 1000L

        super.onDisplayed()
        if (!minimized) {
            minimized = true
            minimized = false
        }
        updatePreDrag(null)
    }

    override fun onMouseInput(mousePos: Vec2f) {
        super.onMouseInput(mousePos)
        if (mouseState != MouseState.DRAG) updatePreDrag(mousePos.minus(posX, posY))
    }

    override fun onClick(mousePos: Vec2f, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        lastActiveTime = System.currentTimeMillis()
    }

    override fun onRelease(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        lastActiveTime = System.currentTimeMillis()

        if (minimizable
            && prevState != MouseState.DRAG
            && buttonId == 1
            && mousePos.y - posY < draggableHeight
        ) minimized = !minimized

        if (mouseState != MouseState.DRAG) {
            updatePreDrag(mousePos.minus(posX, posY))
        }

        if (screen.windows.runSynchronized { contains(dockingOverlay) }) {
            dockingOverlay.onRelease(mousePos, clickPos, buttonId)
        }
    }

    private fun updatePreDrag(mousePos: Vec2f?) {
        mousePos?.let { preDragMousePos = it }
        preDragPos = Vec2f(posX, posY)
        preDragSize = Vec2f(width, height)
    }

    override fun onDrag(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        super.onDrag(mousePos, clickPos, buttonId)

        when (buttonId) {
            0 -> handleLeftClickDrag(clickPos, mousePos)
            1 -> handleRightClickDrag(clickPos, mousePos)
        }
    }

    private fun handleLeftClickDrag(
        clickPos: Vec2f,
        mousePos: Vec2f
    ) {
        val relativeClickPos = clickPos - preDragPos
        val centerSplitterH = min(10.0, preDragSize.x / 3.0)
        val centerSplitterV = min(10.0, preDragSize.y / 3.0)

        val horizontalSide = when (relativeClickPos.x) {
            in -2.0..centerSplitterH -> dev.luna5ama.trollhack.graphics.HAlign.LEFT
            in centerSplitterH..preDragSize.x - centerSplitterH -> dev.luna5ama.trollhack.graphics.HAlign.CENTER
            in preDragSize.x - centerSplitterH..preDragSize.x + 2.0 -> dev.luna5ama.trollhack.graphics.HAlign.RIGHT
            else -> null
        }

        val centerSplitterVCenter =
            if (draggableHeight != height && horizontalSide == dev.luna5ama.trollhack.graphics.HAlign.CENTER) {
                2.5
            } else {
                min(15.0, preDragSize.x / 3.0)
            }

        val verticalSide = when (relativeClickPos.y) {
            in -2.0..centerSplitterVCenter -> dev.luna5ama.trollhack.graphics.VAlign.TOP
            in centerSplitterVCenter..preDragSize.y - centerSplitterV -> dev.luna5ama.trollhack.graphics.VAlign.CENTER
            in preDragSize.y - centerSplitterV..preDragSize.y + 2.0 -> dev.luna5ama.trollhack.graphics.VAlign.BOTTOM
            else -> null
        }

        if (horizontalSide == null || verticalSide == null) return
        val draggedDist = mousePos.minus(clickPos)

        if (resizable && !minimized && (horizontalSide != dev.luna5ama.trollhack.graphics.HAlign.CENTER || verticalSide != dev.luna5ama.trollhack.graphics.VAlign.CENTER)) {
            handleResizeX(horizontalSide, draggedDist)
            handleResizeY(verticalSide, draggedDist)

            onResize()
        } else if (draggableHeight == height || relativeClickPos.y <= draggableHeight) {
            val x = (preDragPos.x + draggedDist.x).coerceIn(0.0f, mc.displayWidth - width - 1.0f)
            val y = (preDragPos.y + draggedDist.y).coerceIn(0.0f, mc.displayHeight - height - 1.0f)
            posX = x
            posY = y

            onReposition()
        } else {
            // TODO
        }
    }

    private fun handleRightClickDrag(
        clickPos: Vec2f,
        mousePos: Vec2f
    ) {
        val relativeClickPos = clickPos.minus(preDragPos)
        if (relativeClickPos.y > draggableHeight) return

        screen.displayWindow(dockingOverlay)
    }

    private fun handleResizeX(horizontalSide: dev.luna5ama.trollhack.graphics.HAlign, draggedDist: Vec2f) {
        when (horizontalSide) {
            dev.luna5ama.trollhack.graphics.HAlign.LEFT -> {
                val draggedX = max(draggedDist.x, 1.0f - preDragPos.x)
                var newWidth = max(preDragSize.x - draggedX, minWidth)

                if (maxWidth != -1.0f) newWidth = min(newWidth, maxWidth)
                newWidth = min(newWidth, scaledDisplayWidth - 2.0f)

                val prevWidth = width
                width = newWidth
                posX += prevWidth - newWidth
            }
            dev.luna5ama.trollhack.graphics.HAlign.RIGHT -> {
                val draggedX = min(draggedDist.x, preDragPos.x + preDragSize.x - 1.0f)
                var newWidth = max(preDragSize.x + draggedX, minWidth)

                if (maxWidth != -1.0f) newWidth = min(newWidth, maxWidth)
                newWidth = min(newWidth, scaledDisplayWidth - posX - 2.0f)

                width = newWidth
            }
            else -> {
                // Nothing lol
            }
        }
    }

    private fun handleResizeY(verticalSide: dev.luna5ama.trollhack.graphics.VAlign, draggedDist: Vec2f) {
        when (verticalSide) {
            dev.luna5ama.trollhack.graphics.VAlign.TOP -> {
                val draggedY = max(draggedDist.y, 1.0f - preDragPos.y)
                var newHeight = max(preDragSize.y - draggedY, minHeight)

                if (maxHeight != -1.0f) newHeight = min(newHeight, maxHeight)
                newHeight = min(newHeight, scaledDisplayHeight - 2.0f)

                val prevHeight = height
                height = newHeight
                posY += prevHeight - newHeight
            }
            dev.luna5ama.trollhack.graphics.VAlign.BOTTOM -> {
                val draggedY = min(draggedDist.y, preDragPos.y + preDragSize.y - 1.0f)
                var newHeight = max(preDragSize.y + draggedY, minHeight)

                if (maxHeight != -1.0f) newHeight = min(newHeight, maxHeight)
                newHeight = min(newHeight, scaledDisplayHeight - posY - 2.0f)

                height = newHeight
            }
            else -> {
                // Nothing lol
            }
        }
    }

    open fun isInWindow(mousePos: Vec2f): Boolean {
        val xMin = preDragPos.x - 2.0f
        val xMax = preDragPos.x + preDragSize.x + 2.0f
        val yMin = preDragPos.y - 2.0f
        val yMax = preDragPos.y + max(preDragSize.y * renderMinimizeProgress, draggableHeight) + 2.0f

        return visible && mousePos.x in xMin..xMax && mousePos.y in yMin..yMax
    }

    init {
        with({ updatePreDrag(null) }) {
            dockingHSetting.listeners.add(this)
            dockingVSetting.listeners.add(this)
        }
    }
}