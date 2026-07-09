package dev.luna5ama.trollhack.command.execute

import dev.luna5ama.trollhack.command.AbstractCommandManager
import dev.luna5ama.trollhack.command.Command
import dev.luna5ama.trollhack.command.args.AbstractArg
import dev.luna5ama.trollhack.command.args.ArgIdentifier

/**
 * Event being used for executing the [Command]
 */
interface IExecuteEvent {

    val commandManager: AbstractCommandManager<*>

    /**
     * Parsed arguments
     */
    val args: Array<String>

    /**
     * Maps argument for the [argTree]
     */
    suspend fun mapArgs(argTree: List<AbstractArg<*>>)

    /**
     * Gets mapped value for an [ArgIdentifier]
     *
     * @throws NullPointerException If this [ArgIdentifier] isn't mapped
     */
    val <T : Any> ArgIdentifier<T>.value: T

}
