package cum.xiaro.trollhack.util.command.execute

import cum.xiaro.trollhack.util.command.args.FinalArg

/**
 * Used to check if a [FinalArg] can be invoke with an [IExecuteEvent].
 * The default behavior is all [ExecuteOption]'s [canExecute] must returns true.
 *
 * @param E Type of [IExecuteEvent]
 */
interface ExecuteOption<E : IExecuteEvent> {
    /**
     * A predicate to check if the [event] can be used to invoke a [FinalArg]
     */
    suspend fun canExecute(event: E): Boolean

    /**
     * Action to perform if [canExecute] returns false
     */
    suspend fun onFailed(event: E)
}

/**
 * A wrapper for allowing `or` operation check on multiple [ExecuteOption]
 */
class AnyOption<E : IExecuteEvent>(private vararg val options: ExecuteOption<E>) : ExecuteOption<E> {
    override suspend fun canExecute(event: E): Boolean {
        return options.any { it.canExecute(event) }
    }

    override suspend fun onFailed(event: E) {
        options.last().onFailed(event)
    }
}
