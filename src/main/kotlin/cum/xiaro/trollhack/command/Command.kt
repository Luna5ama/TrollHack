package cum.xiaro.trollhack.command

import cum.xiaro.trollhack.util.command.args.FinalArg
import cum.xiaro.trollhack.util.command.execute.IExecuteEvent
import cum.xiaro.trollhack.util.command.utils.Invokable
import cum.xiaro.trollhack.util.command.utils.SubCommandNotFoundException
import cum.xiaro.trollhack.util.interfaces.Alias
import cum.xiaro.trollhack.util.interfaces.Nameable

/**
 * Command built from [CommandBuilder], this shouldn't be used
 * directly for instance creation in implementation.
 *
 * @param E Type of [IExecuteEvent], can be itself or its subtype
 * @param name Name of this [Command], used to call the [Command] or identifying
 * @param alias Alias of [Command], functions the same as [name]
 * @param description Description of this [Command]
 * @param finalArgs Possible argument combinations of this [Command]
 */
class Command<E : IExecuteEvent> internal constructor(
    override val name: CharSequence,
    override val alias: Array<out CharSequence>,
    val description: String,
    val finalArgs: Array<FinalArg<E>>,
    val builder: CommandBuilder<E>
) : Nameable, Alias, Invokable<E> {

    /**
     * [name] + [alias]
     */
    val allNames = arrayOf(name, *alias)

    /**
     * Invoke this [Command] with [event].
     *
     * @param event Event being used for invoking, must match the type [E]
     *
     * @throws SubCommandNotFoundException if no sub command is found
     */
    override suspend fun invoke(event: E) {
        finalArgs.firstOrNull { it.checkArgs(event.args) }?.invoke(event)
            ?: throw SubCommandNotFoundException(event.args, this)
    }

    /**
     * Returns argument help for this [Command].
     */
    fun printArgHelp(): String {
        return finalArgs.joinToString("\n\n") {
            var argHelp = it.printArgHelp()
            val description = it.toString()

            if (argHelp.isBlank()) argHelp = "<No Argument>"
            if (description.isNotBlank()) argHelp += "\n$it"
            argHelp
        }
    }

    override fun equals(other: Any?) = this === other
        || (other is Command<*>
        && name == other.name
        && alias.contentEquals(other.alias)
        && description == other.description
        && finalArgs.contentEquals(other.finalArgs)
        && builder == other.builder)

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + alias.contentHashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + finalArgs.contentHashCode()
        return 31 * result + builder.hashCode()
    }

}
