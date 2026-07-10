package dev.luna5ama.trollhack.gui.hud.impl

import dev.luna5ama.trollhack.gui.HudModule
import dev.luna5ama.trollhack.utils.math.vectors.HAlign

object Notification : HudModule(
    name = "Notification",
    description = "Client notifications"
) {
    private var dockingH by setting("Docking H", HAlign.LEFT)
    val moduleToggle by setting("Module Toggle", true)
    val moduleToggleMessageTimeout by setting(
        "Module Toggle Message Timeout",
        3000,
        0..10000,
        100,
        { moduleToggle }
    )
    private val defaultTimeout by setting("Default Timeout", 5000, 0..10000, 100)
    private val nvidia by setting("Nvidia Theme", false)
    private val backgroundAlpha by setting("Background Alpha", 180, 0..255, 1, { nvidia })

    private val lock = Any()
    private val notifications = LinkedHashMap<Long, Message>()

    fun send(message: String, length: Long = defaultTimeout.toLong()) {
        send(message.hashCode().toLong(), message, length)
    }

    fun send(identifier: Any, message: String, length: Long = defaultTimeout.toLong()) {
        send(identifier.hashCode().toLong(), message, length)
    }

    fun send(id: Long, message: String, length: Long = defaultTimeout.toLong()) {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            notifications[id] = Message(message, now + length.coerceAtLeast(0L))
        }
    }

    fun messages(showExample: Boolean = false): List<String> {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            notifications.entries.removeIf { it.value.expiresAt <= now }
            if (showExample && notifications.isEmpty()) return listOf("Example Notification")
            return notifications.values.map { it.text }
        }
    }

    fun alignRight(): Boolean = dockingH == HAlign.RIGHT
    fun useNvidiaTheme(): Boolean = nvidia
    fun configuredBackgroundAlpha(): Int = backgroundAlpha

    private data class Message(val text: String, val expiresAt: Long)

    enum class NotificationType(val icon: Char) {
        INFO('I'),
        WARN('?'),
        ERROR('!')
    }
}
