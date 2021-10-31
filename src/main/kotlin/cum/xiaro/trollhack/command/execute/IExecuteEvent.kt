package cum.xiaro.trollhack.util.command.execute

import cum.xiaro.trollhack.command.AbstractCommandManager
import cum.xiaro.trollhack.command.Command
import cum.xiaro.trollhack.util.command.args.AbstractArg
import cum.xiaro.trollhack.util.command.args.ArgIdentifier

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
