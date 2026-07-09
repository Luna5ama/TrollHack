/*
 * Copyright (c) 2021-2022, SagiriXiguajerry. All rights reserved.
 * This repository will be transformed to SuperMic_233.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package dev.luna5ama.trollhack.modules.impl.visual


import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.CoreRender2DEvent
import dev.luna5ama.trollhack.event.impl.render.CoreRender3DEvent
import dev.luna5ama.trollhack.event.impl.render.Render2DEvent
import dev.luna5ama.trollhack.gui.hud.impl.Notification
import dev.luna5ama.trollhack.manager.managers.UnicodeFontManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.modules.impl.client.Colors
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.graphics.GLHelper
import dev.luna5ama.trollhack.graphics.animations.DynamicAnimationFlag
import dev.luna5ama.trollhack.graphics.animations.Easing
import dev.luna5ama.trollhack.graphics.buffer.Render2DUtils
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.shader.BlurRenderer
import dev.luna5ama.trollhack.utils.timing.TickTimer
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.absoluteValue

object Notification : Module("Notification", "123", Category.VISUAL) {
    private val startY by setting("Start Y", -40f, -120f..0f, 0.5f)
    private val animLength by setting("Animation Length", 150, 0..1000, 50)
    private val queue = ArrayList<NotificationElement>()

    private val mainFont get() = UnicodeFontManager.CURRENT_FONT

    init {
        nonNullHandler<CoreRender2DEvent> {
            GLHelper.blend = true
            queue.forEachIndexed { i, n ->
                n.render(i)
            }
            queue.removeIf { it.destroy }
        }
    }

    fun push(title: String, text: String, length: Int, type: Notification.NotificationType) {
        queue.add(
            NotificationElement(title, text, length, type)
        )
    }

    data class NotificationElement(val title: String, val text: String, val length: Int, val type: Notification.NotificationType) {
        private val fontHeight = mainFont.height
        private val padding = 1f
        private val barHeight = 2f
        private val rectHeight = fontHeight + padding * 2 + barHeight
        private val rectWidth = 2 + mainFont.getWidth(text) + padding * 2
        private val animX = DynamicAnimationFlag({ Easing.OUT_CUBIC }, { animLength.toFloat() })
        private val animY = DynamicAnimationFlag({ Easing.OUT_CUBIC }, { animLength.toFloat() })
        private val timer = TickTimer()
        @Volatile
        private var shown = false // already rendering fully on screen
        @Volatile
        private var ended = false // timed up, preparing to shorten the rect
        @Volatile
        var destroy = false; private set
        @Volatile
        private var showNormally = true
        private val maxX get() = mc.window.guiScaledWidth - rectWidth
        private val minX get() = mc.window.guiScaledWidth

        init {
            animX.forceUpdate(minX.toFloat(), minX.toFloat())
            animY.forceUpdate(mc.window.guiScaledHeight + startY, mc.window.guiScaledHeight + startY)
        }

        private fun getTargetY(index: Int) =
            if (showNormally) mc.window.guiScaledHeight - rectHeight
            else mc.window.guiScaledHeight.toFloat()

        private fun computeY(index: Int): Float {
            // index == 0 -> resolution.height + startY
            // index == 1 -> resolution.height + startY - rectHeight
            // index == n -> resolution.height + startY - n * rectHeight
            return animY.getAndUpdate(
                if (showNormally && !ended && !destroy) mc.window.guiScaledHeight + startY - index * (rectHeight + 6)
                else mc.window.guiScaledHeight + startY
            ) // back to the start point if we cancelled the notification
            // in advance or timed up
        }

        private fun computeX(index: Int): Float {
            // resolution.width - rectWidth
            return animX.getAndUpdate(
                if (showNormally && !ended && !destroy) mc.window.guiScaledWidth - rectWidth
                else mc.window.guiScaledWidth.toFloat()
            ) // back out of the screen if we cancelled the notification
            // in advance or timed up
        }

        fun destroyInAdvance() {
            showNormally = false
        }

        fun render(index: Int) {
            try {
                val x = computeX(index)
                val y = computeY(index)
                if ((x - maxX).absoluteValue <= 0.001f) { // already shown
                    shown = true
                    if (timer.tick(length) || !showNormally) {
                        ended = true
                    } else {
                        val passed = timer.passed
                        val percentage = 1 - (passed.toFloat() / length)
                        if (Colors.blur) BlurRenderer.render(x, y, x + rectWidth, y + rectHeight, ClientSettings.windowBlurPass)
                        Render2DUtils.drawRect(x, y, x + rectWidth, y + rectHeight, ColorRGBA(255, 255, 255, 100))
                        Render2DUtils.drawRect(x, y + rectHeight - barHeight, x + rectWidth * percentage, y + rectHeight, ColorRGBA(255, 255, 255,150))
                        mainFont.drawText(text, x + padding + 1.5f, y + padding, shadow = true)
                    }
                } else if (shown && ended) { // time up
                    if ((x - minX).absoluteValue <= 0.001f) { // shortened
                        destroy = true
                    }
                    if (Colors.blur) BlurRenderer.render(x, y, x + rectWidth, y + rectHeight, ClientSettings.windowBlurPass)
                    Render2DUtils.drawRect(x, y, x + rectWidth, y + rectHeight, ColorRGBA(255, 255, 255,100))
                    mainFont.drawText(text, x + padding + 1.5f, y + padding, shadow = true)
                } else { // not shown yet
                    if (Colors.blur) BlurRenderer.render(x, y, x + rectWidth, y + rectHeight, ClientSettings.windowBlurPass)
                    Render2DUtils.drawRect(x, y, x + rectWidth, y + rectHeight, ColorRGBA(255, 255, 255,100))
                    Render2DUtils.drawRect(x, y + rectHeight - barHeight, x + rectWidth, y + rectHeight, ColorRGBA(255, 255, 255,150))
                    mainFont.drawText(text, x + padding + 1.5f, y + padding, shadow = true)
                    timer.reset()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}