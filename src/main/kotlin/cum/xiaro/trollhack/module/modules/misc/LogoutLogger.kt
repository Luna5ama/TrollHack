package cum.xiaro.trollhack.module.modules.misc

import com.mojang.authlib.GameProfile
import cum.xiaro.trollhack.event.events.ConnectionEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeParallelListener
import cum.xiaro.trollhack.manager.managers.WaypointManager
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.EntityUtils.flooredPosition
import cum.xiaro.trollhack.util.EntityUtils.isFakeOrSelf
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.TimeUnit
import cum.xiaro.trollhack.util.math.CoordinateConverter.asString
import cum.xiaro.trollhack.util.text.MessageSendUtils
import cum.xiaro.trollhack.util.threads.onMainThread
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