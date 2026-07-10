package dev.luna5ama.trollhack.graphics.skia

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import dev.luna5ama.trollhack.RenderSystem
import dev.luna5ama.trollhack.gui.TrollHackCompose
import dev.luna5ama.trollhack.event.impl.render.Skia2DEvent
import dev.luna5ama.trollhack.utils.Helper
import dev.luna5ama.trollhack.utils.compat.bindWrite
import dev.luna5ama.trollhack.utils.compat.frameBufferId
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.lwjgl.opengl.GL11.GL_SCISSOR_TEST
import org.lwjgl.opengl.GL11.GL_CULL_FACE
import org.lwjgl.opengl.GL11.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11.GL_STENCIL_TEST
import org.lwjgl.opengl.GL11.glColorMask
import org.lwjgl.opengl.GL11.glDepthMask
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glViewport
import org.lwjgl.glfw.GLFW
import java.awt.event.KeyEvent as AwtKeyEvent

@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
object SkiaMinecraftBridge : Helper {
    var enabled = false
    private var directContext: DirectContext? = null
    private var composeScene: ComposeScene? = null
    private var composeSurface: Surface? = null
    private var composeContent: (@Composable () -> Unit)? = null
    private var composeScale = Float.NaN
    private val pressedButtons = BooleanArray(5)
    private var lastPointerPosition = Offset.Zero
    private var inputActive = false

    fun setComposeContent(content: (@Composable () -> Unit)?) {
        composeContent = content
        composeScene?.close()
        composeScene = null
        composeSurface?.close()
        composeSurface = null
        composeScale = Float.NaN
        enabled = content != null || Skia2DEvent.hasListeners
    }

    fun activateInput() {
        inputActive = true
    }

    fun deactivateInput() {
        inputActive = false
        composeScene?.sendPointerEvent(
            eventType = PointerEventType.Exit,
            position = lastPointerPosition,
            buttons = pointerButtons(),
            keyboardModifiers = pointerModifiers(currentGlfwModifiers())
        )
        composeScene?.cancelPointerInput()
        composeScene?.focusManager?.releaseFocus()
        pressedButtons.fill(false)
    }

    fun sendPointerMove(x: Float, y: Float, modifiers: Int = currentGlfwModifiers()): Boolean {
        if (!inputActive) return false
        lastPointerPosition = Offset(x, y)
        val result = composeScene?.sendPointerEvent(
            eventType = PointerEventType.Move,
            position = lastPointerPosition,
            buttons = pointerButtons(),
            keyboardModifiers = pointerModifiers(modifiers)
        )
        return result != null
    }

    fun sendPointerButton(
        x: Float,
        y: Float,
        button: Int,
        pressed: Boolean,
        modifiers: Int = currentGlfwModifiers()
    ): Boolean {
        if (!inputActive) return false
        lastPointerPosition = Offset(x, y)
        if (button in pressedButtons.indices) pressedButtons[button] = pressed
        val result = composeScene?.sendPointerEvent(
            eventType = if (pressed) PointerEventType.Press else PointerEventType.Release,
            position = lastPointerPosition,
            buttons = pointerButtons(),
            keyboardModifiers = pointerModifiers(modifiers),
            button = button.toComposeButton()
        )
        return result != null
    }

    fun sendScroll(
        x: Float,
        y: Float,
        horizontal: Float,
        vertical: Float,
        modifiers: Int = currentGlfwModifiers()
    ): Boolean {
        if (!inputActive) return false
        lastPointerPosition = Offset(x, y)
        val result = composeScene?.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = lastPointerPosition,
            buttons = pointerButtons(),
            keyboardModifiers = pointerModifiers(modifiers),
            scrollDelta = Offset(-horizontal * 48f, -vertical * 48f)
        )
        return result != null
    }

    fun sendKey(
        keyCode: Int,
        codePoint: Int = 0,
        pressed: Boolean,
        modifiers: Int = currentGlfwModifiers()
    ): Boolean {
        if (!inputActive) return false
        val composeKey = if (keyCode == GLFW.GLFW_KEY_UNKNOWN || keyCode == AwtKeyEvent.VK_UNDEFINED) {
            Key.Unknown
        } else {
            Key(keyCode.toAwtKeyCode())
        }
        return composeScene?.sendKeyEvent(
            KeyEvent(
                key = composeKey,
                type = if (pressed) KeyEventType.KeyDown else KeyEventType.KeyUp,
                codePoint = codePoint,
                isCtrlPressed = modifiers and GLFW.GLFW_MOD_CONTROL != 0,
                isMetaPressed = modifiers and GLFW.GLFW_MOD_SUPER != 0,
                isAltPressed = modifiers and GLFW.GLFW_MOD_ALT != 0,
                isShiftPressed = modifiers and GLFW.GLFW_MOD_SHIFT != 0
            )
        ) == true
    }

    fun sendCharacter(codePoint: Int, modifiers: Int = currentGlfwModifiers()): Boolean {
        val consumed = sendKey(AwtKeyEvent.VK_UNDEFINED, codePoint, true, modifiers)
        sendKey(AwtKeyEvent.VK_UNDEFINED, codePoint, false, modifiers)
        return consumed
    }

    fun render2D(ticksDelta: Float) {
        if (!enabled || (!Skia2DEvent.hasListeners && composeContent == null)) return
        if (!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) return

        val framebufferWidth = mc.window.width
        val framebufferHeight = mc.window.height
        if (framebufferWidth <= 0 || framebufferHeight <= 0) return

        val projection = RenderSystem.guiProjection.update(
            framebufferWidth,
            framebufferHeight,
            RenderSystem.renderScaleF
        )
        val minecraftGlState = MinecraftGlState.capture()
        var context: DirectContext? = null

        try {
            minecraftGlState.prepareForSkia()
            context = directContext ?: DirectContext.makeGL().also { directContext = it }
            mc.mainRenderTarget.bindWrite(false)
            context.resetGLAll()
            glViewport(0, 0, framebufferWidth, framebufferHeight)
            glDisable(GL_SCISSOR_TEST)
            glDisable(GL_DEPTH_TEST)
            glDisable(GL_STENCIL_TEST)
            glDisable(GL_CULL_FACE)
            glDepthMask(false)
            glColorMask(true, true, true, true)

            BackendRenderTarget.makeGL(
            framebufferWidth,
            framebufferHeight,
            0,
            0,
            mc.mainRenderTarget.frameBufferId,
            FramebufferFormat.GR_GL_RGBA8
            ).use { target ->
            val surface = Surface.makeFromBackendRenderTarget(
                context,
                target,
                SurfaceOrigin.BOTTOM_LEFT,
                SurfaceColorFormat.RGBA_8888,
                ColorSpace.sRGB
            ) ?: return@use
            surface.use {
                val canvas = surface.canvas
                canvas.save()
                canvas.scale(projection.scale, projection.scale)
                Skia2DEvent(
                    context = context,
                    surface = surface,
                    canvas = canvas,
                    framebufferWidth = framebufferWidth,
                    framebufferHeight = framebufferHeight,
                    width = projection.width,
                    height = projection.height,
                    scale = projection.scale,
                    ticksDelta = ticksDelta
                ).post()
                canvas.restore()

                composeContent?.let { content ->
                    val scene = composeScene.takeIf { composeScale == projection.scale }
                        ?: CanvasLayersComposeScene(
                            density = Density(projection.scale),
                            size = IntSize(framebufferWidth, framebufferHeight),
                            coroutineContext = Dispatchers.Unconfined,
                            invalidate = {}
                        ).also {
                            composeScene?.close()
                            composeScene = it
                            composeScale = projection.scale
                            it.setContent(content)
                        }
                    scene.size = IntSize(framebufferWidth, framebufferHeight)
                    TrollHackCompose.syncFrame()
                    val layer = composeSurface?.takeIf {
                        it.width == framebufferWidth && it.height == framebufferHeight
                    } ?: Surface.makeRenderTarget(
                        context,
                        true,
                        ImageInfo(
                            framebufferWidth,
                            framebufferHeight,
                            ColorType.RGBA_8888,
                            ColorAlphaType.PREMUL,
                            ColorSpace.sRGB
                        )
                    ).also {
                            composeSurface?.close()
                            composeSurface = it
                        }
                    layer.canvas.clear(0x00000000)
                    scene.render(layer.canvas.asComposeCanvas(), System.nanoTime())
                    layer.flushAndSubmit()
                    layer.makeImageSnapshot().use { image ->
                        canvas.drawImage(image, 0f, 0f)
                    }
                }

                surface.flushAndSubmit()
            }
            }
        } finally {
            context?.resetGLAll()
            minecraftGlState.restore()
        }
    }

    fun close() {
        composeScene?.close()
        composeScene = null
        composeSurface?.close()
        composeSurface = null
        directContext?.close()
        directContext = null
    }

    private fun Int.toComposeButton() = when (this) {
        GLFW.GLFW_MOUSE_BUTTON_LEFT -> PointerButton.Primary
        GLFW.GLFW_MOUSE_BUTTON_RIGHT -> PointerButton.Secondary
        GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> PointerButton.Tertiary
        GLFW.GLFW_MOUSE_BUTTON_4 -> PointerButton.Back
        GLFW.GLFW_MOUSE_BUTTON_5 -> PointerButton.Forward
        else -> PointerButton(this)
    }

    private fun pointerButtons() = PointerButtons(
        isPrimaryPressed = pressedButtons[GLFW.GLFW_MOUSE_BUTTON_LEFT],
        isSecondaryPressed = pressedButtons[GLFW.GLFW_MOUSE_BUTTON_RIGHT],
        isTertiaryPressed = pressedButtons[GLFW.GLFW_MOUSE_BUTTON_MIDDLE],
        isBackPressed = pressedButtons[GLFW.GLFW_MOUSE_BUTTON_4],
        isForwardPressed = pressedButtons[GLFW.GLFW_MOUSE_BUTTON_5]
    )

    private fun pointerModifiers(modifiers: Int) = PointerKeyboardModifiers(
        isCtrlPressed = modifiers and GLFW.GLFW_MOD_CONTROL != 0,
        isMetaPressed = modifiers and GLFW.GLFW_MOD_SUPER != 0,
        isAltPressed = modifiers and GLFW.GLFW_MOD_ALT != 0,
        isShiftPressed = modifiers and GLFW.GLFW_MOD_SHIFT != 0,
        isCapsLockOn = modifiers and GLFW.GLFW_MOD_CAPS_LOCK != 0,
        isNumLockOn = modifiers and GLFW.GLFW_MOD_NUM_LOCK != 0
    )

    private fun currentGlfwModifiers(): Int {
        val window = mc.window.handle()
        var modifiers = 0
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS
        ) modifiers = modifiers or GLFW.GLFW_MOD_SHIFT
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS
        ) modifiers = modifiers or GLFW.GLFW_MOD_CONTROL
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS
        ) modifiers = modifiers or GLFW.GLFW_MOD_ALT
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SUPER) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SUPER) == GLFW.GLFW_PRESS
        ) modifiers = modifiers or GLFW.GLFW_MOD_SUPER
        return modifiers
    }

    private fun Int.toAwtKeyCode() = when (this) {
        GLFW.GLFW_KEY_ESCAPE -> AwtKeyEvent.VK_ESCAPE
        GLFW.GLFW_KEY_ENTER -> AwtKeyEvent.VK_ENTER
        GLFW.GLFW_KEY_TAB -> AwtKeyEvent.VK_TAB
        GLFW.GLFW_KEY_BACKSPACE -> AwtKeyEvent.VK_BACK_SPACE
        GLFW.GLFW_KEY_INSERT -> AwtKeyEvent.VK_INSERT
        GLFW.GLFW_KEY_DELETE -> AwtKeyEvent.VK_DELETE
        GLFW.GLFW_KEY_RIGHT -> AwtKeyEvent.VK_RIGHT
        GLFW.GLFW_KEY_LEFT -> AwtKeyEvent.VK_LEFT
        GLFW.GLFW_KEY_DOWN -> AwtKeyEvent.VK_DOWN
        GLFW.GLFW_KEY_UP -> AwtKeyEvent.VK_UP
        GLFW.GLFW_KEY_PAGE_UP -> AwtKeyEvent.VK_PAGE_UP
        GLFW.GLFW_KEY_PAGE_DOWN -> AwtKeyEvent.VK_PAGE_DOWN
        GLFW.GLFW_KEY_HOME -> AwtKeyEvent.VK_HOME
        GLFW.GLFW_KEY_END -> AwtKeyEvent.VK_END
        GLFW.GLFW_KEY_CAPS_LOCK -> AwtKeyEvent.VK_CAPS_LOCK
        GLFW.GLFW_KEY_SCROLL_LOCK -> AwtKeyEvent.VK_SCROLL_LOCK
        GLFW.GLFW_KEY_NUM_LOCK -> AwtKeyEvent.VK_NUM_LOCK
        GLFW.GLFW_KEY_PRINT_SCREEN -> AwtKeyEvent.VK_PRINTSCREEN
        GLFW.GLFW_KEY_PAUSE -> AwtKeyEvent.VK_PAUSE
        in GLFW.GLFW_KEY_F1..GLFW.GLFW_KEY_F25 -> AwtKeyEvent.VK_F1 + (this - GLFW.GLFW_KEY_F1)
        else -> this
    }
}
