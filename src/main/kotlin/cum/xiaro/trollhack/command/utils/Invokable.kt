package cum.xiaro.trollhack.util.command.utils

import cum.xiaro.trollhack.util.command.execute.IExecuteEvent

/**
 * Interface for class that can be invoked with an [IExecuteEvent]
 *
 * @param E Type of [IExecuteEvent], can be itself or its subtype
 */
interface Invokable<E : IExecuteEvent> {

    /**
     * Invoke this with [event]
     */
    suspend fun invoke(event: E)

}
