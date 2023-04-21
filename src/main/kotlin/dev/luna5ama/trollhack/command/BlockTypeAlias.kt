package dev.luna5ama.trollhack.command

import dev.luna5ama.trollhack.command.args.AbstractArg
import dev.luna5ama.trollhack.command.args.ArgIdentifier
import dev.luna5ama.trollhack.command.execute.IExecuteEvent

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
