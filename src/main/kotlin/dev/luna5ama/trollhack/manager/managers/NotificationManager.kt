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

package dev.luna5ama.trollhack.manager.managers


import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.gui.hud.impl.Notification
import dev.luna5ama.trollhack.utils.ChatUtils
import dev.luna5ama.trollhack.utils.runSafe
import net.minecraft.ChatFormatting

object NotificationManager : AlwaysListening {
    fun push(title: String, text: String, length: Long, type: Notification.NotificationType) {
        runSafe {
            if (Notification.isEnabled) {
                Notification.send(text, length)
            } else {
                ChatUtils.sendMessage("[$title] ${
                    when (type) {
                        Notification.NotificationType.INFO -> ChatFormatting.GREEN
                        Notification.NotificationType.WARN -> ChatFormatting.GOLD
                        Notification.NotificationType.ERROR -> ChatFormatting.RED
                    }
                }$text")
            }
        }
    }
}