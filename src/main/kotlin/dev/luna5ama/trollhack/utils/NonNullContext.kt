package dev.luna5ama.trollhack.utils

import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import dev.luna5ama.trollhack.event.api.ListenerOwner
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.impl.LoopEvent
import dev.luna5ama.trollhack.event.impl.world.ConnectionEvent
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <R> runSafeOrElse(defaultValue: R, block: NonNullContext.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val instance = NonNullContext.instance
    return if (instance != null) {
        block.invoke(instance)
    } else {
        defaultValue
    }
}

@OptIn(ExperimentalContracts::class)
inline fun runSafeOrFalse(block: NonNullContext.() -> Boolean): Boolean {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val instance = NonNullContext.instance
    return if (instance != null) {
        block.invoke(instance)
    } else {
        false
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <R> runSafe(block: NonNullContext.() -> R): R? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val instance = NonNullContext.instance
    return if (instance != null) {
        block.invoke(instance)
    } else {
        null
    }
}

suspend fun <R> runSafeSuspend(block: suspend NonNullContext.() -> R): R? {
    return NonNullContext.instance?.let { block(it) }
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Any, R> T.runSynchronized(block: T.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return synchronized(this@runSynchronized) {
        block.invoke(this@runSynchronized)
    }
}

abstract class AbstractContext {
    val mc = MinecraftWrapper.mc
    abstract val world: ClientLevel?
    abstract val player: LocalPlayer?
    abstract val interaction: MultiPlayerGameMode?
    abstract val netHandler: ClientPacketListener?
}

open class ClientContext : AbstractContext() {
    override val world: ClientLevel? = mc.level
    override val player: LocalPlayer? = mc.player
    override val interaction: MultiPlayerGameMode? = mc.gameMode
    override val netHandler: ClientPacketListener? = mc.connection

    inline operator fun <T> invoke(block: ClientContext.() -> T) = run(block)
}

open class NonNullContext internal constructor(
    override val world: ClientLevel,
    override val player: LocalPlayer,
    override val interaction: MultiPlayerGameMode,
    override val netHandler: ClientPacketListener
) : AbstractContext() {
    inline operator fun <T> invoke(block: NonNullContext.() -> T) = run(block)

    fun cut(): Any {
        return object {
            val player = this@NonNullContext.player
            val world = this@NonNullContext.world
            val interaction = this@NonNullContext.interaction
            val netHandler = this@NonNullContext.netHandler
        }
    }

    companion object : ListenerOwner() {
        var instance: NonNullContext? = null; private set

        init {
            handler<ConnectionEvent.Disconnect>(Int.MAX_VALUE, true) {
                reset()
            }

            handler<WorldEvent.Unload>(Int.MAX_VALUE, true) {
                reset()
            }

            handler<LoopEvent.Tick>(Int.MAX_VALUE, true) {
                update()
            }
        }

        @OptIn(ImplicitOverriding::class)
        fun update() {
            val world = MinecraftWrapper.world ?: return
            val player = MinecraftWrapper.player ?: return
            val playerController = MinecraftWrapper.mc.gameMode ?: return
            val connection = MinecraftWrapper.mc.connection ?: return

            instance = NonNullContext(world, player, playerController, connection)
        }

        fun reset() {
            instance = null
        }
    }
}