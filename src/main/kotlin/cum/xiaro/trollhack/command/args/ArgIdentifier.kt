package cum.xiaro.trollhack.util.command.args

import cum.xiaro.trollhack.util.interfaces.Nameable

/**
 * The ID for an argument
 */
@Suppress("UNUSED")
data class ArgIdentifier<T : Any>(override val name: CharSequence) : Nameable
