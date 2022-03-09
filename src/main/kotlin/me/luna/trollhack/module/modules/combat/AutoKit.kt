package me.luna.trollhack.module.modules.combat

import me.luna.trollhack.event.events.ConnectionEvent
import me.luna.trollhack.event.events.EntityEvent
import me.luna.trollhack.event.events.TickEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.TickTimer
import me.luna.trollhack.util.text.MessageSendUtils
import me.luna.trollhack.util.text.MessageSendUtils.sendServerMessage

internal object AutoKit : Module(
    name = "AutoKit",
    description = "Do /kit automatically",
    category = Category.COMBAT
) {
    private val kitName by setting("Kit Name", "")

    private var shouldSend = false
    private val timer = TickTimer()

    init {
        listener<ConnectionEvent.Connect> {
            shouldSend = true
            timer.reset(3000L)
        }

        safeListener<EntityEvent.Death> {
            if (it.entity == player) {
                shouldSend = true
                timer.reset(1500L)
            }
        }

        safeListener<TickEvent.Post> {
            if (player.isDead) {
                shouldSend = true
            } else if (shouldSend && timer.tick(0)) {
                val name = kitName
                if (name.isNotBlank()) {
                    MessageSendUtils.sendServerMessage("/kit $name")
                }
                shouldSend = false
            }
        }
    }
}
