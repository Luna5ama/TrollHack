package cum.xiaro.trollhack.command

import cum.xiaro.trollhack.util.command.args.*
import cum.xiaro.trollhack.util.command.execute.ExecuteOption
import cum.xiaro.trollhack.util.command.execute.IExecuteEvent
import cum.xiaro.trollhack.util.command.utils.BuilderBlock
import cum.xiaro.trollhack.util.command.utils.ExecuteBlock

/**
 * Builder for [Command], extend this or subtype of this
 * to build a command. Or extend this to add more arg types.
 *
 * @param E Type of [IExecuteEvent], can be itself or its subtype
 * @param name (Optional) Name for the [Command]
 * @param description (Optional) Description for the [Command]
 */
open class CommandBuilder<E : IExecuteEvent>(
    name: String,
    alias: Array<out String> = emptyArray(),
    private val description: String = "No description",
) : LiteralArg(name, alias) {

    /**
     * Final arguments to be used for building the command
     */
    protected val finalArgs = ArrayList<FinalArg<E>>()

    /**
     * Appends a [FinalArg], adds it to [finalArgs]
     *
     * @param options (Optional) [ExecuteOption] used to check before invoking [block]
     * @param block [ExecuteBlock] to run on invoking
     */
    @CommandBuilder
    protected fun AbstractArg<*>.execute(
        vararg options: ExecuteOption<E>,
        block: ExecuteBlock<E>
    ) {
        execute("No description", *options, block = block)
    }

    /**
     * Appends a [FinalArg], adds it to [finalArgs]
     *
     * @param description (Optional) Description for this argument combination
     * @param options (Optional) [ExecuteOption] used to check before invoking [block]
     * @param block [ExecuteBlock] to run on invoking
     */
    @CommandBuilder
    protected fun AbstractArg<*>.execute(
        description: String = "No description",
        vararg options: ExecuteOption<E>,
        block: ExecuteBlock<E>
    ) {
        val arg = FinalArg(description, options, block)
        this.append(arg)
        finalArgs.add(arg)
    }

    /**
     * Appends a [BooleanArg]
     *
     * @param name Name of this argument
     * @param block [BuilderBlock] to appends more arguments
     */
    @CommandBuilder
    protected inline fun AbstractArg<*>.boolean(
        name: String,
        block: BuilderBlock<Boolean>
    ) {
        arg(BooleanArg(name), block)
    }

    /**
     * Appends a [EnumArg]
     *
     * @param E Type of Enum
     * @param name Name of this argument
     * @param block [BuilderBlock] to appends more arguments
     */
    @CommandBuilder
    protected inline fun <reified E : Enum<E>> AbstractArg<*>.enum(
        name: String,
        block: BuilderBlock<E>
    ) {
        arg(EnumArg(name, E::class.java), block)
    }

    /**
     * Appends a [LongArg]
     *
     * @param name Name of this argument
     * @param block [BuilderBlock] to appends more arguments
     */
    @CommandBuilder
    protected inline fun AbstractArg<*>.long(
        name: String,
        block: BuilderBlock<Long>
    ) {
        arg(LongArg(name), block)
    }

    /**
     * Appends a [IntArg]
     *
     * @param name Name of this argument
     * @param block [BuilderBlock] to appends more arguments
     */
    @CommandBuilder
    protected inline fun AbstractArg<*>.int(
        name: String,
        block: BuilderBlock<Int>
    ) {
        arg(IntArg(name), block)
    }

    /**
     * Appends a [FloatArg]
     *
     * @param name Name of this argument
     * @param block [BuilderBlock] to appends more arguments
     */
    @CommandBuilder
    protected inline fun AbstractArg<*>.float(
        name: String,
        block: BuilderBlock<Float>
    ) {
        arg(FloatArg(name), block)
    }

    /**
     * Appends a [DoubleArg]
     *
     * @param name Name of this argument
     * @param block [BuilderBlock] to appends more arguments
     */
    @CommandBuilder
    protected inline fun AbstractArg<*>.double(
        name: String,
        block: BuilderBlock<Double>
    ) {
        arg(DoubleArg(name), block)
    }

    /**
     * Appends a [LiteralArg]
     *
     * @param name Name of this argument
     * @param alias Alias of this literal argument
     * @param block [BuilderBlock] to appends more arguments
     */
    @CommandBuilder
    protected inline fun AbstractArg<*>.literal(
        name: String,
        vararg alias: String,
        block: LiteralArg.() -> Unit
    ) {
        val arg = LiteralArg(name, alias)
        this.append(arg)
        arg.block()
    }

    /**
     * Appends a [StringArg]
     *
     * @param name Name of this argument
     * @param block [BuilderBlock] to appends more arguments
     */
    @CommandBuilder
    protected inline fun AbstractArg<*>.string(
        name: String,
        block: BuilderBlock<String>
    ) {
        arg(StringArg(name), block)
    }

    /**
     * Appends a [GreedyStringArg]
     *
     * @param name Name of this argument
     * @param block [BuilderBlock] to appends more arguments
     */
    @CommandBuilder
    protected inline fun AbstractArg<*>.greedy(
        name: String,
        block: BuilderBlock<String>
    ) {
        arg(GreedyStringArg(name), block)
    }

    /**
     * Appends a [AbstractArg] with type of [T]
     *
     * @param T The type of [arg]
     * @param arg Argument to append
     * @param block [BuilderBlock] to appends more arguments
     */
    @CommandBuilder
    protected inline fun <T : Any> AbstractArg<*>.arg(
        arg: AbstractArg<T>,
        block: BuilderBlock<T>
    ) {
        this.append(arg)
        arg.block(arg.identifier)
    }

    /**
     * Annotation to mark the builder methods
     */
    @DslMarker
    protected annotation class CommandBuilder

    /**
     * Built this into a [Command]
     */
    internal fun buildCommand(): Command<E> {
        return Command(name, alias, description, finalArgs.toTypedArray(), this)
    }

}
