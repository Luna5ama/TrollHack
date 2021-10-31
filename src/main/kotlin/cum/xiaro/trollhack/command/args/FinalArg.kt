package cum.xiaro.trollhack.util.command.args

import cum.xiaro.trollhack.util.command.execute.ExecuteOption
import cum.xiaro.trollhack.util.command.execute.IExecuteEvent
import cum.xiaro.trollhack.util.command.utils.ExecuteBlock
import cum.xiaro.trollhack.util.command.utils.Invokable

/**
 * An argument that take no input and has a [ExecuteBlock]
 *
 * @param description (Optional) Description for this argument combination
 * @param options (Optional) [ExecuteOption] used to check before invoking [block]
 * @param block [ExecuteBlock] to run on invoking
 */
class FinalArg<E : IExecuteEvent>(
    private val description: String,
    private val options: Array<out ExecuteOption<E>>,
    private val block: ExecuteBlock<E>
) : AbstractArg<Unit>(), Invokable<E> {

    override val name: String
        get() = argTree.joinToString(".")

    override suspend fun convertToType(string: String?): Unit? {
        return if (string == null) Unit
        else null
    }

    /**
     * Check if [argsIn] matches with all arguments in [argTree]
     *
     * @return True if all matched
     */
    suspend fun checkArgs(argsIn: Array<String>): Boolean {
        val lastArgType = argTree.last()

        if (argsIn.size != argTree.size
            && !(argsIn.size - 1 == argTree.size && argsIn.last().isBlank())
            && !(argsIn.size > argTree.size && lastArgType is GreedyStringArg)
        ) return false

        return countArgs(argsIn) == argTree.size
    }

    /**
     * Count matched arguments in [argsIn]
     *
     * @return Number of matched arguments
     */
    suspend fun countArgs(argsIn: Array<String>): Int {
        var matched = 0

        for ((index, argType) in argTree.withIndex()) {
            val success = if (argType is GreedyStringArg) {
                matched++
                break
            } else {
                argType.checkType(argsIn.getOrNull(index))
            }

            if (success) matched++
            else break
        }

        return matched
    }

    /**
     * Maps arguments in the [event] and invoke the [block] if passed all the [options]
     */
    override suspend fun invoke(event: E) {
        event.mapArgs(argTree)

        for (option in options) {
            if (!option.canExecute(event)) {
                option.onFailed(event)
                return
            }
        }

        block.invoke(event)
    }

    override fun toString(): String {
        return if (description.isNotBlank()) "- $description" else ""
    }

    fun printArgHelp(): String {
        return (argTree.first().name +
            argTree.subList(1, argTree.size).joinToString(" ", " ")).trimEnd()
    }

}
