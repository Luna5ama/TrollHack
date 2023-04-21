package dev.luna5ama.trollhack.gui.rgui.windows

import dev.luna5ama.trollhack.gui.AbstractTrollGui
import dev.luna5ama.trollhack.gui.rgui.Component
import dev.luna5ama.trollhack.gui.rgui.InteractiveComponent
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.module.modules.render.AntiAlias
import dev.luna5ama.trollhack.util.TickTimer
import dev.luna5ama.trollhack.util.extension.fastCeil
import dev.luna5ama.trollhack.util.extension.fastFloor
import dev.luna5ama.trollhack.util.extension.sumOfFloat
import dev.luna5ama.trollhack.util.graphics.GlStateUtils
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.*
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

open class ListWindow(
    name: CharSequence,
    posX: Float,
    posY: Float,
    width: Float,
    height: Float,
    saveToConfig: SettingGroup,
    vararg childrenIn: Component
) : TitledWindow(name, posX, posY, width, height, saveToConfig) {
    val children = ArrayList<Component>()

    override val minWidth = 80.0f
    override val minHeight = 200.0f
    override val maxWidth = 200.0f
    override val maxHeight get() = mc.displayHeight.toFloat()
    override val resizable: Boolean get() = hoveredChild == null

    private val lineSpace = 3.0f
    var hoveredChild: Component? = null
        private set(value) {
            if (value == field) return
            (field as? InteractiveComponent)?.onLeave(AbstractTrollGui.getRealMousePos())
            (value as? InteractiveComponent)?.onHover(AbstractTrollGui.getRealMousePos())
            field = value
        }

    private val scrollTimer = TickTimer()
    private var lastScrollSpeedUpdate = System.currentTimeMillis()
    protected var scrollSpeed = 0.0f
    protected var scrollProgress = 0.0f

    private var doubleClickTime = -1L

    init {
        children.addAll(childrenIn)
        updateChild()
    }

    private fun updateChild() {
        var y = (if (draggableHeight != height) draggableHeight else 0.0f) + lineSpace
        for (child in children) {
            if (!child.visible) continue
            child.posX = lineSpace * 1.618f
            child.posY = y
            child.width = width - lineSpace * 3.236f
            y += child.height + lineSpace
        }
    }

    override fun onDisplayed() {
        super.onDisplayed()
        for (child in children) child.onDisplayed()
        updateChild()
        onTick()
        lastScrollSpeedUpdate = System.currentTimeMillis()
        for (child in children) child.onDisplayed()
    }

    override fun onClosed() {
        super.onClosed()
        for (child in children) child.onClosed()
    }

    override fun onGuiInit() {
        super.onGuiInit()
        for (child in children) child.onGuiInit()
        updateChild()
    }

    override fun onResize() {
        super.onResize()
        updateChild()
    }

    override fun onTick() {
        super.onTick()

        updateChild()
        for (child in children) child.onTick()

        updateHovered(AbstractTrollGui.getRealMousePos().minus(posX, posY))
    }

    override fun onRender(absolutePos: Vec2f) {
        super.onRender(absolutePos)
        updateScrollProgress()

        if (renderMinimizeProgress != 0.0f) {
            renderChildren {
                it.onRender(absolutePos.plus(it.renderPosX, it.renderPosY - scrollProgress))
            }
        }
    }

    private fun updateScrollProgress() {
        if (children.isEmpty()) return

        val x = (System.currentTimeMillis() - lastScrollSpeedUpdate) / 100.0
        val lnHalf = ln(0.25)
        val newSpeed = scrollSpeed * (0.25.pow(x))
        scrollProgress += ((newSpeed / lnHalf) - (scrollSpeed / lnHalf)).toFloat()
        scrollSpeed = newSpeed.toFloat()
        lastScrollSpeedUpdate = System.currentTimeMillis()

        if (scrollTimer.tick(100L)) {
            val lastVisible = children.lastOrNull { it.visible }
            val maxScrollProgress = lastVisible?.let { max(it.posY + it.height + lineSpace - height, 0.01f) }
                ?: draggableHeight
            if (scrollProgress < 0.0) {
                scrollSpeed = scrollProgress * -0.4f
            } else if (scrollProgress > maxScrollProgress) {
                scrollSpeed = (scrollProgress - maxScrollProgress) * -0.4f
            }
        }
    }


    override fun onPostRender(absolutePos: Vec2f) {
        super.onPostRender(absolutePos)

        if (renderMinimizeProgress != 0.0f) {
            renderChildren {
                it.onPostRender(absolutePos.plus(it.renderPosX, it.renderPosY - scrollProgress))
            }
        }
    }

    private inline fun renderChildren(renderBlock: (Component) -> Unit) {
        val sampleLevel = AntiAlias.sampleLevel

        GlStateUtils.scissor(
            (((renderPosX + lineSpace * 1.618) * GuiSetting.scaleFactor - 0.5f) * sampleLevel).fastFloor(),
            (mc.displayHeight * sampleLevel - ((renderPosY * sampleLevel + renderHeight * sampleLevel) * GuiSetting.scaleFactor - 0.5f)).fastFloor(),
            (((renderWidth - lineSpace * 3.236) * GuiSetting.scaleFactor + 1.0f) * sampleLevel).fastCeil(),
            (((renderHeight - draggableHeight) * GuiSetting.scaleFactor) * sampleLevel).fastCeil()
        )
        glEnable(GL_SCISSOR_TEST)
        glTranslatef(0.0f, -scrollProgress, 0.0f)

        mc.profiler.startSection("childrens")
        for (child in children) {
            if (!child.visible) continue
            if (child.renderPosY + child.renderHeight - scrollProgress < draggableHeight) continue
            if (child.renderPosY - scrollProgress > renderHeight) continue
            glPushMatrix()
            glTranslatef(child.renderPosX, child.renderPosY, 0.0f)
            renderBlock(child)
            glPopMatrix()
        }
        mc.profiler.endSection()

        glDisable(GL_SCISSOR_TEST)
    }

    override fun onMouseInput(mousePos: Vec2f) {
        super.onMouseInput(mousePos)
        val relativeMousePos = mousePos.minus(posX, posY)
        if (Mouse.getEventDWheel() != 0) {
            scrollTimer.reset()
            scrollSpeed -= Mouse.getEventDWheel() * 0.2f
            updateHovered(relativeMousePos)
        }
        if (mouseState != MouseState.DRAG) {
            updateHovered(relativeMousePos)
        }
        if (!minimized) (hoveredChild as? InteractiveComponent)?.let {
            it.onMouseInput(getRelativeMousePos(mousePos, it))
        }
    }

    private fun updateHovered(relativeMousePos: Vec2f) {
        hoveredChild =
            if (relativeMousePos.y < draggableHeight || relativeMousePos.x < lineSpace || relativeMousePos.x > renderWidth - lineSpace) null
            else children.firstOrNull { it.visible && relativeMousePos.y + scrollProgress in it.posY..it.posY + it.height }
    }

    override fun onLeave(mousePos: Vec2f) {
        super.onLeave(mousePos)
        hoveredChild = null
    }

    override fun onClick(mousePos: Vec2f, buttonId: Int) {
        super.onClick(mousePos, buttonId)

        handleDoubleClick(mousePos, buttonId)

        if (!minimized) (hoveredChild as? InteractiveComponent)?.let {
            it.onClick(getRelativeMousePos(mousePos, it), buttonId)
        }
    }

    override fun onRelease(mousePos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, buttonId)
        if (!minimized) (hoveredChild as? InteractiveComponent)?.let {
            it.onRelease(getRelativeMousePos(mousePos, it), buttonId)
        }
    }

    override fun onDrag(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        super.onDrag(mousePos, clickPos, buttonId)
        if (!minimized) (hoveredChild as? InteractiveComponent)?.let {
            it.onDrag(getRelativeMousePos(mousePos, it), clickPos, buttonId)
        }
    }

    override fun onKeyInput(keyCode: Int, keyState: Boolean) {
        super.onKeyInput(keyCode, keyState)
        if (!minimized) (hoveredChild as? InteractiveComponent)?.onKeyInput(keyCode, keyState)
    }

    private fun handleDoubleClick(mousePos: Vec2f, buttonId: Int) {
        if (!visible || buttonId != 0 || mousePos.y - posY >= draggableHeight) {
            doubleClickTime = -1L
            return
        }

        val currentTime = System.currentTimeMillis()

        doubleClickTime = if (currentTime - doubleClickTime > 500L) {
            currentTime
        } else {
            updateHeightToFit(false)

            -1L
        }
    }

    protected fun updateHeightToFit(forceHeight: Boolean) {
        val sum = children.asSequence().filter(Component::visible).sumOfFloat { it.height + lineSpace }
        val targetHeight = sum + draggableHeight + lineSpace
        if (!forceHeight && targetHeight < height) {
            return
        }
        val maxHeight = scaledDisplayHeight - 2.0f

        height = min(targetHeight, scaledDisplayHeight - 2.0f)
        posY = min(posY, maxHeight - targetHeight)
    }

    private fun getRelativeMousePos(mousePos: Vec2f, component: InteractiveComponent): Vec2f {
        return mousePos.minus(posX, posY - scrollProgress).minus(component.posX, component.posY)
    }
}