package me.luna.trollhack.module.modules.misc

import kotlinx.coroutines.launch
import me.luna.trollhack.event.events.InputEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.manager.managers.FriendManager
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.TickTimer
import me.luna.trollhack.util.text.MessageSendUtils
import me.luna.trollhack.util.threads.defaultScope
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.util.math.RayTraceResult

internal object MidClickFriends : Module(
    name = "MidClickFriends",
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
            MessageSendUtils.sendNoSpamChatMessage("§b$name§r has been unfriended.")
        }
    }

    private fun add(name: String) {
        defaultScope.launch {
            if (FriendManager.addFriend(name)) MessageSendUtils.sendNoSpamChatMessage("§b$name§r has been friended.")
            else MessageSendUtils.sendNoSpamChatMessage("Failed to find UUID of $name")
        }
    }
}