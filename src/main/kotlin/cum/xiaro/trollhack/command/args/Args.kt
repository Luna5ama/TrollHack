package cum.xiaro.trollhack.util.command.args

import cum.xiaro.trollhack.util.interfaces.Alias
import cum.xiaro.trollhack.util.interfaces.DisplayEnum

/**
 * Argument that takes a [Boolean] as input
 *
 * @param name Name of this argument
 */
class BooleanArg(
    override val name: String
) : AbstractArg<Boolean>() {
    override suspend fun convertToType(string: String?): Boolean? {
        return string.toTrueOrNull() ?: string.toFalseOrNull()
    }

    private fun String?.toTrueOrNull() =
        if (this != null && (this.equals("true", true) || this.equals("on", true))) true
        else null

    private fun String?.toFalseOrNull() =
        if (this != null && (this.equals("false", true) || this.equals("off", true))) false
        else null
}

/**
 * Argument that takes a [Enum] as input
 *
 * @param E Type of input [Enum]
 * @param name Name of this argument
 * @param enumClass Class of [E]
 */
class EnumArg<E : Enum<E>>(
    override val name: String,
    enumClass: Class<E>
) : AbstractArg<E>(), AutoComplete by StaticPrefixMatch(getAllNames(enumClass)) {
    private val enumValues = enumClass.enumConstants

    override suspend fun convertToType(string: String?): E? {
        return enumValues.find { it.name.equals(string, true) }
    }

    private companion object {
        private fun <E : Enum<E>> getAllNames(clazz: Class<E> ) = ArrayList<String>().apply {
            for (enum in clazz.enumConstants) {
                if (enum is DisplayEnum) add(enum.displayName.toString())
                add(enum.name)
            }
        }
    }
}

/**
 * Argument that takes a [Int] as input
 *
 * @param name Name of this argument
 */
class IntArg(
    override val name: String
) : AbstractArg<Int>() {

    override suspend fun convertToType(string: String?): Int? {
        return string?.toIntOrNull()
    }

}

/**
 * Argument that takes a [Long] as input
 *
 * @param name Name of this argument
 */
class LongArg(
    override val name: String
) : AbstractArg<Long>() {

    override suspend fun convertToType(string: String?): Long? {
        return string?.toLongOrNull()
    }

}

/**
 * Argument that takes a [Float] as input
 *
 * @param name Name of this argument
 */
class FloatArg(
    override val name: String
) : AbstractArg<Float>() {

    override suspend fun convertToType(string: String?): Float? {
        return string?.toFloatOrNull()
    }

}

/**
 * Argument that takes a [Double] as input
 *
 * @param name Name of this argument
 */
class DoubleArg(
    override val name: String
) : AbstractArg<Double>() {

    override suspend fun convertToType(string: String?): Double? {
        return string?.toDoubleOrNull()
    }

}

/**
 * Argument that takes a [String] as input, and must be
 * matched with [name] or one of the [alias]
 *
 * @param name Name of this argument
 * @param alias Alias of this literal argument
 */
open class LiteralArg(
    override val name: String,
    override val alias: Array<out String>,
) : AbstractArg<String>(), Alias, AutoComplete by StaticPrefixMatch(listOf(name, *alias)) {

    override suspend fun convertToType(string: String?): String? {
        return if (string.equals(name, true) || alias.any { string.equals(it, false) }) {
            string
        } else {
            null
        }
    }

    override fun toString(): String {
        return "[$name]"
    }

}

/**
 * Argument that takes a [String] as input
 *
 * @param name Name of this argument
 */
class StringArg(
    override val name: String
) : AbstractArg<String>() {
    override suspend fun convertToType(string: String?): String? {
        return string
    }
}

/**
 * Argument that takes all [String] after as input
 *
 * @param name Name of this argument
 */
class GreedyStringArg(
    override val name: String
) : AbstractArg<String>() {
    override suspend fun convertToType(string: String?): String? {
        return string
    }
}
