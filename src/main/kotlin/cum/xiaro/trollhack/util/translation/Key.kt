package cum.xiaro.trollhack.util.translation

import cum.xiaro.trollhack.util.extension.map
import cum.xiaro.trollhack.util.IDRegistry
import cum.xiaro.trollhack.util.translation.TranslationManager.getTranslated
import kotlin.reflect.KProperty

class TranslationKey private constructor(
    val type: I18nType,
    val keyString: String,
    val rootString: String
) : CharSequence {
    val id = idRegistry.register()
    private var cached: String? = null

    init {
        translationKeyMap[keyString] = this
    }

    override val length: Int
        get() = get().length

    override fun get(index: Int): Char {
        return get()[index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return get().subSequence(startIndex, endIndex)
    }

    private fun update() {
        cached = null
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return get()
    }

    fun get(): String {
        return cached
            ?: this.getTranslated().also { cached = it }
    }

    override fun toString(): String {
        return get()
    }

    override fun equals(other: Any?) =
        this === other
            || (other is TranslationKey
            && type == other.type
            && keyString == other.keyString
            && rootString == other.rootString)

    override fun hashCode(): Int {
        return 31 * type.hashCode() +
            31 * keyString.hashCode() +
            rootString.hashCode()
    }

    companion object {
        private val idRegistry = IDRegistry()
        private val translationKeyMap = HashMap<String, TranslationKey>()

        val allKeys get() = translationKeyMap.values

        fun getOrPut(type: I18nType, keyString: String, rootString: String) =
            translationKeyMap.getOrPut(keyString) {
                TranslationKey(type, keyString, rootString)
            }

        operator fun get(string: String) = translationKeyMap[string]

        fun updateAll() {
            allKeys.forEach {
                it.update()
            }
        }
    }
}

enum class I18nType(override val typeName: String) : ITranslationType {
    COMMON("c") {
        override fun commonKey(string: String): TranslationKey {
            return TranslationKey.getOrPut(this, transform(string), string)
        }

        override fun commonKey(pair: Pair<String, String>): TranslationKey {
            return TranslationKey.getOrPut(this, transform(pair.first), pair.second)
        }

        override fun ITranslateSrc.transform(string: String): String {
            return this@COMMON.transform(string)
        }

        private fun transform(string: String): String {
            val replaced = replaceChars(string)
            return "$I18N_PREFIX$typeName.${replaced}$I18N_SUFFIX"
        }
    },
    SPECIFIC("s") {
        override fun ITranslateSrc.transform(string: String): String {
            val replaced = replaceChars(string)
            return "$I18N_PREFIX$typeName.$srcIdentifier.${replaced}$I18N_SUFFIX"
        }
    },
    LONG("l") {
        override fun ITranslateSrc.transform(string: String): String {
            val replaced = replaceChars(string)
            return "$I18N_PREFIX$typeName.$srcIdentifier.${keyID++}.${replaced}$I18N_SUFFIX"
        }
    };
}

interface ITranslationType {
    val typeName: String
    infix fun commonKey(string: String): TranslationKey {
        throw UnsupportedOperationException()
    }

    infix fun commonKey(pair: Pair<String, String>): TranslationKey {
        throw UnsupportedOperationException()
    }

    fun ITranslateSrc.transform(string: String): String
}

class TranslateSrc(srcIdentifierIn: String) : ITranslateSrc {
    override val srcIdentifier: String = replaceChars(srcIdentifierIn)
    override var keyID: Int = 0
}

interface ITranslateSrc {
    val srcIdentifier: String
    var keyID: Int

    infix fun I18nType.key(string: String): TranslationKey {
        return TranslationKey.getOrPut(this, transform(string), string)
    }

    infix fun I18nType.key(pair: Pair<String, String>): TranslationKey {
        return TranslationKey.getOrPut(this, transform(pair.first), pair.second)
    }
}

private fun replaceChars(string: String): String {
    return string.map {
        var char = it.lowercaseChar()
        when (char) {
            ' ', '.' -> char = '_'
        }
        char
    }
}