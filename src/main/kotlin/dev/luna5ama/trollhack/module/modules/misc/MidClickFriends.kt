package dev.luna5ama.trollhack.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.events.InputEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.BackgroundScope
import kotlinx.coroutines.launch
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.util.math.RayTraceResult

internal object MidClickFriends : Module(
    name = "Mid Click Friends",
    category = Category.MISC,
    description = "Middle click players to friend or unfriend them",
    visible = false
) {
    private val timer = TickTimer()
    private var lastPlayer: EntityOtherPlayerMP? = null

    init {
        listener<InputEvent.Mouse> {
            // 0 is left, 1 is right, 2 is middle
            if (it.state || it.button != 2 || mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != RayTraceResult.Type.ENTITY) return@listener
            val player = mc.objectMouseOver.entityHit as? EntityOtherPlayerMP ?: return@listener

            if (timer.tickAndReset(5000L) || player != lastPlayer && timer.tickAndReset(500L)) {
                if (FriendManager.isFriend(player.name)) remove(player.name)
                else add(player.name)
                lastPlayer = player
            }
        }
    }

    private fun remove(name: String) {
        if (FriendManager.removeFriend(name)) {
            NoSpamMessage.sendMessage("§b$name§r has been unfriended.")
        }
    }

    private fun add(name: String) {
        BackgroundScope.launch {
            if (FriendManager.addFriend(name)) NoSpamMessage.sendMessage("§b$name§r has been friended.")
            else NoSpamMessage.sendMessage("Failed to find UUID of $name")
        }
    }
}