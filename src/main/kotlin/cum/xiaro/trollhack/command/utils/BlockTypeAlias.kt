package cum.xiaro.trollhack.util.command.utils

import cum.xiaro.trollhack.command.CommandBuilder
import cum.xiaro.trollhack.util.command.args.AbstractArg
import cum.xiaro.trollhack.util.command.args.ArgIdentifier
import cum.xiaro.trollhack.util.command.execute.IExecuteEvent

/**
 * Type alias for a block used for execution of a argument combination
 *
 * @param E Type of [IExecuteEvent], can be itself or its subtype
 *
 * @see CommandBuilder.execute
 */
typealias ExecuteBlock<E> = suspend E.() -> Unit

/**
 * Type alias for a block used for Argument building
 *
 * @param T Type of argument
 *
 * @see CommandBuilder
 */
typealias BuilderBlock<T> = AbstractArg<T>.(ArgIdentifier<T>) -> Unit
