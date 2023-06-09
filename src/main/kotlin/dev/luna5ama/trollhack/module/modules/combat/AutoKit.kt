package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.EntityEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.text.MessageSendUtils
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage

internal object AutoKit : Module(
    name = "Auto Kit",
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