package dev.luna5ama.trollhack.modules.impl.client

import dev.luna5ama.trollhack.gui.hud.impl.Notification
import dev.luna5ama.trollhack.manager.managers.ConfigManager
import dev.luna5ama.trollhack.manager.managers.NotificationManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.runSafe

object Presets : Module("Presets", category = Category.CLIENT, excludedFromConfig = true) {
    var target by setting("Target", "default")
    private val save by setting("Save", false).register { _, _ ->
        runSafe {
            trySave()
        }
        false
    }
    private val load by setting("Load", false).register { _, _ ->
        runSafe {
            tryLoad()
        }
        false
    }

    fun trySave() {
        ConfigManager.namespace = target
        ConfigManager.save()
        NotificationManager.push("Config", "Saved current config to ${ConfigManager.namespace}",
            10000, Notification.NotificationType.INFO)
    }

    fun tryLoad() {
        ConfigManager.namespace = target
        val result = ConfigManager.tryLoad()
        if (result) NotificationManager.push("Config", "Loaded current config from ${ConfigManager.namespace}",
            10000, Notification.NotificationType.INFO)
        else NotificationManager.push("Config", "Failed to load current config from ${ConfigManager.namespace}",
            10000, Notification.NotificationType.WARN)
    }
}