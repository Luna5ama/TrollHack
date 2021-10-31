package cum.xiaro.trollhack.gui.hudgui

import cum.xiaro.trollhack.util.extension.remove
import cum.xiaro.trollhack.event.events.InputEvent
import cum.xiaro.trollhack.event.events.render.Render2DEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.gui.AbstractTrollGui
import cum.xiaro.trollhack.gui.clickgui.TrollClickGui
import cum.xiaro.trollhack.gui.hudgui.component.HudButton
import cum.xiaro.trollhack.gui.hudgui.window.HudSettingWindow
import cum.xiaro.trollhack.gui.rgui.Component
import cum.xiaro.trollhack.gui.rgui.windows.ListWindow
import cum.xiaro.trollhack.module.modules.client.GuiSetting
import cum.xiaro.trollhack.module.modules.client.Hud
import cum.xiaro.trollhack.module.modules.client.HudEditor
import cum.xiaro.trollhack.util.extension.rootName
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.math.vector.Vec2f
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11.*
import java.util.*

object TrollHudGui : AbstractTrollGui<HudSettingWindow, AbstractHudElement>() {

    override val alwaysTicking = true
    private val hudWindows = EnumMap<AbstractHudElement.Category, ListWindow>(AbstractHudElement.Category::class.java)

    init {
        var posX = 0.0f
        var posY = 0.0f
        val screenWidth = TrollClickGui.mc.displayWidth / GuiSetting.scaleFactorFloat

        for (category in AbstractHudElement.Category.values()) {
            val window = ListWindow(category.displayName, posX, 0.0f, 90.0f, 300.0f, Component.SettingGroup.HUD_GUI)
            windowList.add(window)
            hudWindows[category] = window

            posX += 90.0f

            if (posX > screenWidth) {
                posX = 0.0f
                posY += 100.0f
            }
        }

        listener<InputEvent.Keyboard> {
            if (!it.state || it.key == Keyboard.KEY_NONE || Keyboard.isKeyDown(Keyboard.KEY_F3)) return@listener

            for (child in windowList) {
                if (child !is AbstractHudElement) continue
                if (!child.bind.isDown(it.key)) continue
                child.visible = !child.visible
            }
        }
    }

    internal fun register(hudElement: AbstractHudElement) {
        val button = HudButton(hudElement)
        hudWindows[hudElement.category]!!.children.add(button)
        windowList.add(hudElement)
    }

    internal fun unregister(hudElement: AbstractHudElement) {
        hudWindows[hudElement.category]!!.children.removeIf { it is HudButton && it.hudElement == hudElement }
        windowList.remove(hudElement)
    }

    override fun onGuiClosed() {
        super.onGuiClosed()
        setHudButtonVisibility { true }
    }

    override fun newSettingWindow(element: AbstractHudElement, mousePos: Vec2f): HudSettingWindow {
        return HudSettingWindow(element, mousePos.x, mousePos.y)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_ESCAPE || HudEditor.bind.value.isDown(keyCode) && !searching && settingWindow?.listeningChild == null) {
            HudEditor.disable()
        } else {
            super.keyTyped(typedChar, keyCode)

            val string = typedString.remove(" ")

            if (string.isNotEmpty()) {
                setHudButtonVisibility { hudButton ->
                    hudButton.hudElement.name.contains(string, true)
                        || hudButton.hudElement.alias.any { it.contains(string, true) }
                }
            } else {
                setHudButtonVisibility { true }
            }
        }
    }

    private fun setHudButtonVisibility(function: (HudButton) -> Boolean) {
        windowList.filterIsInstance<ListWindow>().forEach {
            for (child in it.children) {
                if (child !is HudButton) continue
                child.visible = function(child)
            }
        }
    }

    init {
        listener<Render2DEvent.Troll>(-1000) {
            if (mc == null || mc.world == null || mc.player == null || mc.currentScreen == this) return@listener

            if (Hud.isEnabled) {
                GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE)

                for (window in windowList) {
                    if (window !is AbstractHudElement || !window.visible) continue
                    mc.profiler.startSection(window.rootName)
                    renderHudElement(window)
                    mc.profiler.endSection()
                }

                GlStateUtils.depth(true)
            }
        }
    }

    private fun renderHudElement(window: AbstractHudElement) {
        glPushMatrix()
        glTranslatef(window.renderPosX, window.renderPosY, 0.0f)

        glScalef(window.scale, window.scale, window.scale)
        window.renderHud()

        glPopMatrix()
    }

}