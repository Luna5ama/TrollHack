package dev.luna5ama.trollhack.command.args

import dev.luna5ama.trollhack.util.interfaces.Nameable

/**
 * The ID for an argument
 */
@Suppress("UNUSED")
data class ArgIdentifier<T : Any>(override val name: CharSequence) : Nameable
