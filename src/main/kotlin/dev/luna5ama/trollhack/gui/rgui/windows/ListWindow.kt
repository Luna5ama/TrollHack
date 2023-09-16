package dev.luna5ama.trollhack.gui.rgui.windows

import dev.fastmc.common.TickTimer
import dev.fastmc.common.ceilToInt
import dev.fastmc.common.floorToInt
import dev.luna5ama.trollhack.graphics.GlStateUtils
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.gui.rgui.Component
import dev.luna5ama.trollhack.gui.rgui.InteractiveComponent
import dev.luna5ama.trollhack.gui.rgui.MouseState
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.module.modules.render.AntiAlias
import dev.luna5ama.trollhack.util.delegate.FrameFloat
import dev.luna5ama.trollhack.util.extension.sumOfFloat
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.*
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

open class ListWindow(
    screen: IGuiScreen,
    name: CharSequence,
    saveToConfig: UiSettingGroup,
    vararg childrenIn: Component
) : TitledWindow(screen, name, saveToConfig) {
    val children = ArrayList<Component>()

    override val minWidth get() = 80.0f
    override val maxWidth get() = max(minWidth, 200.0f)

    override val minHeight = 100.0f
    override val maxHeight get() = mc.displayHeight.toFloat()

    override val resizable: Boolean get() = hoveredChild == null

    private val xMargin get() = GuiSetting.xMargin
    private val yMargin get() = GuiSetting.yMargin

    var hoveredChild: Component? = null
        private set(value) {
            if (value == field) return
            (field as? InteractiveComponent)?.onLeave(screen.mousePos)
            (value as? InteractiveComponent)?.onHover(screen.mousePos)
            field = value
        }

    private val scrollTimer = TickTimer()
    private var lastScrollSpeedUpdate = System.currentTimeMillis()
    protected var scrollSpeed = 0.0f
    protected var scrollProgress = 0.0f

    private var doubleClickTime = -1L

    private val optimalWidth0 = FrameFloat {
        val result = children
            .asSequence()
            .maxOfOrNull { it.minWidth + xMargin * 2.0f } ?: 80.0f
        maxOf(result, MainFontRenderer.getWidth(name) + 20.0f, 80.0f)
    }
    protected val optimalWidth by optimalWidth0

    private val optimalHeight0 = FrameFloat {
        val sum = children.asSequence().filter(Component::visible).sumOfFloat { it.height + yMargin }
        sum + draggableHeight + max(GuiSetting.xMargin, GuiSetting.yMargin)
    }
    protected val optimalHeight by optimalHeight0

    init {
        children.addAll(childrenIn)
        updateChildPosSize()
    }

    override fun onDisplayed() {
        super.onDisplayed()
        lastScrollSpeedUpdate = System.currentTimeMillis()

        onTick()
        for (child in children) child.onDisplayed()
        updateChildPosSize()

        optimalWidth0.updateLazy()
        optimalHeight0.updateLazy()
    }

    override fun onClosed() {
        super.onClosed()
        for (child in children) child.onClosed()
    }

    override fun onResize() {
        super.onResize()
        updateChildPosSize()
    }

    override fun onTick() {
        super.onTick()

        for (child in children) {
            child.onTick()
        }
        updateChildPosSize()

        if (mouseState != MouseState.DRAG) {
            updateHovered(screen.mousePos.minus(posX, posY))
        }
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

    private fun updateChildPosSize() {
        optimalWidth0.updateLazy()
        optimalHeight0.updateLazy()
        var y = (if (draggableHeight != height) draggableHeight else 0.0f) + yMargin
        for (child in children) {
            child.posX = xMargin
            child.posY = y
            child.width = width - xMargin * 2.0f
            if (!child.visible) continue
            y += child.height + yMargin
        }
        optimalWidth0.updateLazy()
        optimalHeight0.updateLazy()
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
            val maxScrollProgress = lastVisible?.let { max(it.posY + it.height + yMargin - height, 0.01f) }
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
            (((renderPosX + xMargin) * GuiSetting.scaleFactor - 0.5f) * sampleLevel).floorToInt(),
            (mc.displayHeight * sampleLevel - ((renderPosY * sampleLevel + renderHeight * sampleLevel) * GuiSetting.scaleFactor - 0.5f)).floorToInt(),
            (((renderWidth - xMargin * 2.0f) * GuiSetting.scaleFactor + 1.0f) * sampleLevel).ceilToInt(),
            (((renderHeight - draggableHeight) * GuiSetting.scaleFactor) * sampleLevel).ceilToInt()
        )
        glEnable(GL_SCISSOR_TEST)
         GlStateManager.translate(0.0f, -scrollProgress, 0.0f)

        mc.profiler.startSection("childrens")
        for (child in children) {
            if (!child.visible) continue
            if (child.renderPosY + child.renderHeight - scrollProgress < draggableHeight) continue
            if (child.renderPosY - scrollProgress > renderHeight) continue
            GlStateManager.pushMatrix()
             GlStateManager.translate(child.renderPosX, child.renderPosY, 0.0f)
            renderBlock(child)
            GlStateManager.popMatrix()
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
        }
        if (mouseState != MouseState.DRAG) {
            updateHovered(relativeMousePos)
        }
        if (!minimized) (hoveredChild as? InteractiveComponent)?.let {
            it.onMouseInput(getRelativeMousePos(mousePos, it))
        }
    }

    private fun updateHovered(relativeMousePos: Vec2f) {
        if (minimized || mouseState == MouseState.NONE) {
            hoveredChild = null
            return
        }

        hoveredChild =
            if (relativeMousePos.y < draggableHeight || relativeMousePos.x < xMargin || relativeMousePos.x > renderWidth - xMargin) null
            else children.firstOrNull { it.visible && relativeMousePos.y + scrollProgress in it.posY..it.posY + it.height }
    }

    override fun onLeave(mousePos: Vec2f) {
        super.onLeave(mousePos)
        hoveredChild = null
    }

    override fun onClick(mousePos: Vec2f, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        val relativeMousePos = mousePos.minus(posX, posY)

        updateHovered(relativeMousePos)
        handleDoubleClick(mousePos, buttonId)

        if (!minimized) (hoveredChild as? InteractiveComponent)?.let {
            it.onClick(getRelativeMousePos(mousePos, it), buttonId)
        }
    }

    override fun onRelease(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        if (!minimized) (hoveredChild as? InteractiveComponent)?.let {
            it.onRelease(getRelativeMousePos(mousePos, it), clickPos, buttonId)
        }
    }

    override fun onDrag(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        super.onDrag(mousePos, clickPos, buttonId)
        if (!minimized) (hoveredChild as? InteractiveComponent)?.let {
            it.onDrag(getRelativeMousePos(mousePos, it), getRelativeMousePos(clickPos, it), buttonId)
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
            if (optimalHeight < height) {
                return
            }
            val maxHeight = scaledDisplayHeight - 2.0f

            height = min(optimalHeight, scaledDisplayHeight - 2.0f)
            posY = min(posY, maxHeight - optimalHeight)

            -1L
        }
    }

    private fun getRelativeMousePos(mousePos: Vec2f, component: InteractiveComponent): Vec2f {
        return mousePos.minus(posX, posY - scrollProgress).minus(component.posX, component.posY)
    }
}