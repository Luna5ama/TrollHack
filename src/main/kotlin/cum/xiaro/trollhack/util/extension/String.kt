@file:Suppress("NOTHING_TO_INLINE")

package cum.xiaro.trollhack.util.extension

/**
 * Limit the length of this string to [max]
 */
fun String.max(max: Int) = this.substring(0, this.length.coerceAtMost(max))

/**
 * Limit the length to this string [max] with [suffix] appended
 */
inline fun String.max(max: Int, suffix: String): String {
    return if (this.length > max) {
        this.max(max - suffix.length) + suffix
    } else {
        this.max(max)
    }
}

inline fun String.surroundedBy(prefix: CharSequence, suffix: CharSequence, ignoreCase: Boolean = false) =
    this.startsWith(prefix, ignoreCase) && this.endsWith(suffix, ignoreCase)

inline fun String.surroundedBy(prefix: Char, suffix: Char, ignoreCase: Boolean = false) =
    this.startsWith(prefix, ignoreCase) && this.endsWith(suffix, ignoreCase)

inline fun String.mapEach(vararg delimiters: Char, transformer: (String) -> String) =
    split(*delimiters).map(transformer)

inline fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecaseChar() else it }
}

inline fun String.normalizeCase(): String {
    return this.mapIndexed { (i, char) ->
        if (i == 0) {
            char.titlecaseChar()
        } else {
            char.lowercaseChar()
        }
    }
}

inline fun String.mapIndexed(transformer: (IndexedValue<Char>) -> Char): String {
    val charArray = CharArray(this.length)

    for (it in this.withIndex()) {
        charArray[it.index] = transformer.invoke(it)
    }

    return String(charArray)
}

inline fun String.map(transformer: (Char) -> Char): String {
    val charArray = CharArray(this.length)

    for ((i, char) in this.withIndex()) {
        charArray[i] = transformer.invoke(char)
    }

    return String(charArray)
}

inline fun String.remove(char: Char): String {
    return buildString {
        for (c in this@remove) {
            if (c != char) {
                append(c)
            }
        }
    }
}

inline fun String.remove(vararg chars: Char): String {
    return buildString {
        for (c in this@remove) {
            if (!chars.contains(c)) {
                append(c)
            }
        }
    }
}

inline fun String.remove(charSequence: CharSequence): String {
    return buildString {
        val first = charSequence.first()
        val l = charSequence.length
        var i = 0

        while (i < this@remove.length) {
            val char = this@remove[i]
            if (i + l <= this@remove.length && char == first) {
                if (this@remove.subSequence(i, i + l) == charSequence) {
                    i += l
                    continue
                }
            }

            append(char)
            i++
        }
    }
}

inline fun String.remove(vararg charSequences: CharSequence): String {
    return buildString {
        var i = 0

        whileLoop@ while (i < this@remove.length) {
            val char = this@remove[i]
            for (charSequence in charSequences) {
                val first = charSequence.first()
                val l = charSequence.length

                if (i + l <= this@remove.length && char == first) {
                    if (this@remove.subSequence(i, i + l) == charSequences) {
                        i += l
                        continue@whileLoop
                    }
                }
            }

            append(char)
            i++
        }
    }
}