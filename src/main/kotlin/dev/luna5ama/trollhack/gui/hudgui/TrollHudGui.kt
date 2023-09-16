package dev.luna5ama.trollhack.gui.hudgui

import dev.fastmc.common.EnumMap
import dev.luna5ama.trollhack.event.events.InputEvent
import dev.luna5ama.trollhack.event.events.render.Render2DEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.graphics.GlStateUtils
import dev.luna5ama.trollhack.graphics.Resolution
import dev.luna5ama.trollhack.gui.AbstractTrollGui
import dev.luna5ama.trollhack.gui.IGuiScreen.Companion.forEachWindow
import dev.luna5ama.trollhack.gui.hudgui.component.HudButton
import dev.luna5ama.trollhack.gui.rgui.Component
import dev.luna5ama.trollhack.gui.rgui.windows.ListWindow
import dev.luna5ama.trollhack.module.modules.client.Hud
import dev.luna5ama.trollhack.module.modules.client.HudEditor
import dev.luna5ama.trollhack.util.extension.remove
import dev.luna5ama.trollhack.util.extension.rootName
import dev.luna5ama.trollhack.util.threads.runSynchronized
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11.*

object TrollHudGui : AbstractTrollGui() {
    override val alwaysTicking = true
    private val hudWindows = EnumMap<AbstractHudElement.Category, ListWindow>()

    override var searchString: String
        get() = super.searchString
        set(value) {
            super.searchString = value

            val string = value.remove(" ")

            if (string.isNotEmpty()) {
                setHudButtonVisibility { hudButton ->
                      hudButton.hudElement.allNames.any { it.contains(string, true) }
                }
            } else {
                setHudButtonVisibility { true }
            }
        }

    init {
        var posX = 0.0f
        var posY = 0.0f

        for (category in AbstractHudElement.Category.values()) {
            val window = ListWindow(this, category.displayName, Component.UiSettingGroup.HUD_GUI)
            window.forcePosX = posX
            window.forcePosY = posY
            window.forceWidth = 80.0f
            window.forceHeight = 400.0f

            hudWindows[category] = window
            posX += 90.0f

            if (posX > Resolution.trollWidthF) {
                posX = 0.0f
                posY += 100.0f
            }

        }

        windows.runSynchronized { addAll(hudWindows.values) }

        listener<InputEvent.Keyboard> {
            if (!it.state || it.key == Keyboard.KEY_NONE || Keyboard.isKeyDown(Keyboard.KEY_F3)) return@listener

            forEachWindow { child ->
                if (child !is AbstractHudElement) return@forEachWindow
                if (!child.bind.isDown(it.key)) return@forEachWindow
                child.visible = !child.visible
            }
        }
    }

    internal fun register(hudElement: AbstractHudElement) {
        val button = HudButton(this, hudElement)
        hudWindows[hudElement.category]!!.children.add(button)
        windows.runSynchronized { addAndMoveToLast(hudElement) }
    }

    internal fun unregister(hudElement: AbstractHudElement) {
        hudWindows[hudElement.category]!!.children.removeIf { it is HudButton && it.hudElement == hudElement }
        windows.runSynchronized { remove(hudElement) }
    }

    override fun onGuiClosed() {
        super.onGuiClosed()
        setHudButtonVisibility { true }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_ESCAPE || HudEditor.bind.value.isDown(keyCode) && !searching && lastClicked?.keybordListening == null) {
            HudEditor.disable()
        } else {
            super.keyTyped(typedChar, keyCode)
        }
    }

    private inline fun setHudButtonVisibility(function: (HudButton) -> Boolean) {
        hudWindows.values.asSequence()
            .flatMap { it.children.asSequence() }
            .filterIsInstance<HudButton>()
            .forEach { it.visible = function(it) }
    }

    init {
        listener<Render2DEvent.Troll>(-1000) {
            if (mc == null || mc.world == null || mc.player == null || mc.currentScreen == this) return@listener

            if (Hud.isEnabled) {
                GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE)

                forEachWindow { window ->
                    if (window !is AbstractHudElement || !window.visible) return@forEachWindow
                    mc.profiler.startSection(window.rootName)
                    renderHudElement(window)
                    mc.profiler.endSection()
                }

                GlStateUtils.depth(true)
            }
        }
    }

    private fun renderHudElement(window: AbstractHudElement) {
        GlStateManager.pushMatrix()
         GlStateManager.translate(window.renderPosX, window.renderPosY, 0.0f)

        GlStateManager.scale(window.scale, window.scale, window.scale)
        window.renderHud()

        GlStateManager.popMatrix()
    }

}