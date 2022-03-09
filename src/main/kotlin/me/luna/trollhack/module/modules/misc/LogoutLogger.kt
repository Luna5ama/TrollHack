package me.luna.trollhack.module.modules.misc

import com.mojang.authlib.GameProfile
import me.luna.trollhack.event.events.ConnectionEvent
import me.luna.trollhack.event.events.TickEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.event.safeParallelListener
import me.luna.trollhack.manager.managers.WaypointManager
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.EntityUtils.flooredPosition
import me.luna.trollhack.util.EntityUtils.isFakeOrSelf
import me.luna.trollhack.util.TickTimer
import me.luna.trollhack.util.TimeUnit
import me.luna.trollhack.util.math.CoordinateConverter.asString
import me.luna.trollhack.util.text.MessageSendUtils
import me.luna.trollhack.util.threads.onMainThread
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.util.math.BlockPos

internal object LogoutLogger : Module(
    name = "LogoutLogger",
    category = Category.MISC,
    description = "Logs when a player leaves the game"
) {
    private val saveWaypoint by setting("Save Waypoint", true)
    private val print by setting("Print To Chat", true)

    private val loggedPlayers = LinkedHashMap<GameProfile, BlockPos>()
    private val timer = TickTimer(TimeUnit.SECONDS)

    init {
        listener<ConnectionEvent.Disconnect> {
            onMainThread {
                loggedPlayers.clear()
            }
        }

        safeParallelListener<TickEvent.Post> {
            for (loadedPlayer in world.playerEntities) {
                if (loadedPlayer !is EntityOtherPlayerMP) continue
                if (loadedPlayer.isFakeOrSelf) continue

                val info = connection.getPlayerInfo(loadedPlayer.gameProfile.id) ?: continue
                loggedPlayers[info.gameProfile] = loadedPlayer.flooredPosition
            }

            if (timer.tickAndReset(1L)) {
                loggedPlayers.entries.removeIf { (profile, pos) ->
                    @Suppress("SENSELESS_COMPARISON")
                    if (connection.getPlayerInfo(profile.id) == null) {
                        if (saveWaypoint) WaypointManager.add(pos, "${profile.name} Logout Spot")
                        if (print) MessageSendUtils.sendNoSpamChatMessage("${profile.name} logged out at ${pos.asString()}")
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }
}