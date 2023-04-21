package dev.luna5ama.trollhack.module.modules.misc

import com.mojang.authlib.GameProfile
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.managers.WaypointManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.flooredPosition
import dev.luna5ama.trollhack.util.EntityUtils.isFakeOrSelf
import dev.luna5ama.trollhack.util.TickTimer
import dev.luna5ama.trollhack.util.TimeUnit
import dev.luna5ama.trollhack.util.math.CoordinateConverter.asString
import dev.luna5ama.trollhack.util.text.MessageSendUtils
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.util.math.BlockPos

internal object LogoutLogger : Module(
    name = "LogoutLogger",
    category = Category.MISC,
    description = "Logs when a player leaves the game"
) {
    private val saveWaypoint by setting("Save Waypoint", true)
    private val print by setting("Print To Chat", true)

    private val removed = LinkedHashSet<GameProfile>()
    private val loggedPlayers = LinkedHashMap<GameProfile, BlockPos>()
    private val timer = TickTimer(TimeUnit.SECONDS)

    init {
        onDisable {
            loggedPlayers.clear()
        }

        listener<WorldEvent.Unload> {
            loggedPlayers.clear()
        }

        listener<WorldEvent.Entity.Remove> {
            if (it.entity !is EntityOtherPlayerMP) return@listener
            removed.add(it.entity.gameProfile)
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
                loggedPlayers.keys.removeAll(removed)
                removed.clear()
            }
        }
    }
}