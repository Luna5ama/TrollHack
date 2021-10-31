package cum.xiaro.trollhack.event

import cum.xiaro.trollhack.util.command.execute.ExecuteEvent
import cum.xiaro.trollhack.util.command.execute.IExecuteEvent
import cum.xiaro.trollhack.command.CommandManager
import cum.xiaro.trollhack.event.events.ConnectionEvent
import cum.xiaro.trollhack.event.events.RunGameLoopEvent
import cum.xiaro.trollhack.event.events.WorldEvent
import cum.xiaro.trollhack.util.Wrapper
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.PlayerControllerMP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraftforge.fml.common.eventhandler.Event

abstract class AbstractClientEvent {
    val mc = Wrapper.minecraft
    abstract val world: WorldClient?
    abstract val player: EntityPlayerSP?
    abstract val playerController: PlayerControllerMP?
    abstract val connection: NetHandlerPlayClient?
}

open class ClientEvent : AbstractClientEvent() {
    final override val world: WorldClient? = mc.world
    final override val player: EntityPlayerSP? = mc.player
    final override val playerController: PlayerControllerMP? = mc.playerController
    final override val connection: NetHandlerPlayClient? = mc.connection

    inline operator fun <T> invoke(block: ClientEvent.() -> T) = run(block)
}

open class SafeClientEvent internal constructor(
    override val world: WorldClient,
    override val player: EntityPlayerSP,
    override val playerController: PlayerControllerMP,
    override val connection: NetHandlerPlayClient
) : AbstractClientEvent() {
    inline operator fun <T> invoke(block: SafeClientEvent.() -> T) = run(block)

    companion object : ListenerOwner() {
        var instance: SafeClientEvent? = null; private set

        init {
            listener<ConnectionEvent.Disconnect>(Int.MAX_VALUE, true) {
                reset()
            }

            listener<WorldEvent.Unload>(Int.MAX_VALUE, true) {
                reset()
            }

            listener<RunGameLoopEvent.Tick>(Int.MAX_VALUE, true) {
                update()
            }
        }

        fun update() {
            val world = Wrapper.world ?: return
            val player = Wrapper.player ?: return
            val playerController = Wrapper.minecraft.playerController ?: return
            val connection = Wrapper.minecraft.connection ?: return

            instance = SafeClientEvent(world, player, playerController, connection)
        }

        fun reset() {
            instance = null
        }
    }
}

class ClientExecuteEvent(
    args: Array<String>
) : ClientEvent(), IExecuteEvent by ExecuteEvent(CommandManager, args)

class SafeExecuteEvent internal constructor(
    world: WorldClient,
    player: EntityPlayerSP,
    playerController: PlayerControllerMP,
    connection: NetHandlerPlayClient,
    event: ClientExecuteEvent
) : SafeClientEvent(world, player, playerController, connection), IExecuteEvent by event

fun Event.cancel() {
    this.isCanceled = true
}