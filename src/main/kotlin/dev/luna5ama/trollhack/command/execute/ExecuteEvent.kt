package dev.luna5ama.trollhack.command.execute

import dev.luna5ama.trollhack.command.AbstractCommandManager
import dev.luna5ama.trollhack.command.args.AbstractArg
import dev.luna5ama.trollhack.command.args.ArgIdentifier
import dev.luna5ama.trollhack.command.args.GreedyStringArg

/**
 * Default implementation of [IExecuteEvent]
 */
open class ExecuteEvent(
    override val commandManager: AbstractCommandManager<*>,
    override val args: Array<String>
) : IExecuteEvent {

    /**
     * Mapping [ArgIdentifier] to their converted arguments
     */
    private val mappedArgs = HashMap<ArgIdentifier<*>, Any>()

    override suspend fun mapArgs(argTree: List<AbstractArg<*>>) {
        for ((index, arg) in argTree.withIndex()) {
            if (arg is GreedyStringArg) {
                arg.convertToType(args.slice(index until args.size).joinToString(" "))?.let {
                    mappedArgs[arg.identifier] = it
                }
                break
            } else {
                arg.convertToType(args.getOrNull(index))?.let {
                    mappedArgs[arg.identifier] = it
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override val <T : Any> ArgIdentifier<T>.value: T
        get() = mappedArgs[this] as T

}
