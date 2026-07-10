package dev.luna5ama.trollhack.gui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.luna5ama.trollhack.config.settings.BindSetting
import dev.luna5ama.trollhack.graphics.skia.SkiaMinecraftBridge
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.utils.input.KeyBind
import org.lwjgl.glfw.GLFW

object TrollHackCompose {
    enum class Mode { NONE, CLICK_GUI, HUD_EDITOR }

    var mode by mutableStateOf(Mode.NONE)
        private set

    private var revision by mutableIntStateOf(0)
    private var bindTarget: BindSetting? = null
    private var lastSync = 0L

    fun start() {
        SkiaMinecraftBridge.setComposeContent { TrollHackRoot() }
    }

    fun show(mode: Mode) {
        this.mode = mode
        SkiaMinecraftBridge.activateInput()
        refresh()
    }

    fun hide() {
        mode = Mode.NONE
        bindTarget = null
        ClickGuiState.closeSettings()
        ClickGuiState.textInputFocused = false
        SkiaMinecraftBridge.deactivateInput()
        refresh()
    }

    fun beginBind(setting: BindSetting) {
        bindTarget = setting
        refresh()
    }

    fun consumeBind(keyCode: Int, scanCode: Int): Boolean {
        val setting = bindTarget ?: return false
        when (keyCode) {
            GLFW.GLFW_KEY_ESCAPE -> Unit
            GLFW.GLFW_KEY_DELETE, GLFW.GLFW_KEY_BACKSPACE -> setting.value = KeyBind.NONE
            else -> setting.value = KeyBind(KeyBind.Category.KEYBOARD, keyCode, scanCode)
        }
        bindTarget = null
        refresh()
        return true
    }

    fun consumeMouseBind(button: Int): Boolean {
        val setting = bindTarget ?: return false
        setting.value = KeyBind(KeyBind.Category.MOUSE, button, -1)
        bindTarget = null
        refresh()
        return true
    }

    fun isBinding(setting: BindSetting) = bindTarget === setting

    fun isTextInputFocused() = ClickGuiState.textInputFocused

    fun handleEscape(): Boolean {
        if (bindTarget != null) {
            bindTarget = null
            refresh()
            return true
        }
        return ClickGuiState.handleEscape()
    }

    fun refresh() {
        revision++
    }

    @Composable
    internal fun observeRevision(): Int = revision

    fun syncFrame() {
        val now = System.nanoTime()
        if (now - lastSync >= 50_000_000L) {
            lastSync = now
            revision++
        }
    }

    @Composable
    private fun TrollHackRoot() {
        TrollHackTheme {
            Box(Modifier.fillMaxSize()) {
                if (mode == Mode.NONE && mc.screen == null && mc.level != null) HudOverlay()

                AnimatedVisibility(
                    visible = mode == Mode.CLICK_GUI,
                    enter = fadeIn(tween(LegacyGuiDuration, easing = LegacyOutCubic)),
                    exit = fadeOut(tween(LegacyGuiDuration, easing = LegacyOutCubic))
                ) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                }
                AnimatedVisibility(
                    visible = mode == Mode.CLICK_GUI,
                    enter = slideInVertically(
                        tween(LegacyGuiDuration, easing = LegacyOutCubic),
                        initialOffsetY = { -it }
                    ),
                    exit = slideOutVertically(
                        tween(LegacyGuiDuration, easing = LegacyOutCubic),
                        targetOffsetY = { -it }
                    )
                ) {
                    ClickGuiContent()
                }

                if (mode == Mode.HUD_EDITOR) HudEditorContent()
            }
        }
    }
}
