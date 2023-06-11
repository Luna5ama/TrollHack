package dev.luna5ama.trollhack.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeConcurrentListener
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.UUIDManager
import dev.luna5ama.trollhack.manager.managers.WaypointManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.flooredPosition
import dev.luna5ama.trollhack.util.EntityUtils.isFakeOrSelf
import dev.luna5ama.trollhack.util.extension.synchronized
import dev.luna5ama.trollhack.util.math.CoordinateConverter.asString
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.runSynchronized
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.BlockPos

internal object LogoutLogger : Module(
    name = "Logout Logger",
    category = Category.MISC,
    description = "Logs when a player leaves the game"
) {
    private val ezLog by setting("EZ Log", false)
    private val ezLogAttackTimeout by setting("EZ Log Attack Timeout", 15, 0..60, 1, { ezLog })
    private val ezLogMessage by setting("EZ Log Message", "Ez log %s!", { ezLog })
    private val saveWaypoint by setting("Save Waypoint", true)
    private val print by setting("Print To Chat", true)

    private val removed = LinkedHashSet<EntityPlayer>().synchronized()
    private val loggedPlayers = LinkedHashMap<EntityPlayer, BlockPos>()
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
            removed.add(it.entity)
        }

        safeConcurrentListener<TickEvent.Post> {
            for (loadedPlayer in EntityManager.players) {
                if (loadedPlayer !is EntityOtherPlayerMP) continue
                if (loadedPlayer.isFakeOrSelf) continue
                @Suppress("SENSELESS_COMPARISON")
                if (connection.getPlayerInfo(loadedPlayer.gameProfile.id) == null) continue
                if (UUIDManager.getByUUID(loadedPlayer.uniqueID, true)?.name != loadedPlayer.name) continue

                loggedPlayers[loadedPlayer] = loadedPlayer.flooredPosition
            }

            if (timer.tickAndReset(1L)) {
                loggedPlayers.entries.removeIf { (player, pos) ->
                    @Suppress("SENSELESS_COMPARISON")
                    if (connection.getPlayerInfo(player.gameProfile.id) == null) {
                        handleLogout(player, pos)
                        true
                    } else {
                        false
                    }
                }
                removed.runSynchronized {
                    loggedPlayers.keys.removeAll(this)
                }
                removed.clear()
            }
        }
    }

    private fun handleLogout(player: EntityPlayer, pos: BlockPos) {
        if (saveWaypoint) {
            WaypointManager.add(pos, "${player.name} Logout Spot")
        }
        if (print) {
            NoSpamMessage.sendMessage(
                LogoutLogger.hashCode() xor player.name.hashCode(),
                "${player.name} logged out at ${pos.asString()}"
            )
        }
        if (ezLog) {
            if (ezLogAttackTimeout == 0
                || System.currentTimeMillis() - CombatManager.getPlayerAttackTime(player) <= ezLogAttackTimeout * 1000L
            ) {
                sendServerMessage(ezLogMessage.format(player.name))
            }
        }
    }
}