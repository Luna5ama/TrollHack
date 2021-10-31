package cum.xiaro.trollhack.util.translation

import cum.xiaro.trollhack.util.collections.ArrayMap
import java.io.File
import java.util.*

class TranslationMap private constructor(
    val language: String,
    private val translations: ArrayMap<String>
) {

    operator fun get(key: TranslationKey): String {
        return translations[key.id] ?: key.rootString
    }

    companion object {
        private val regex = "^(\\\$.+\\\$)=(.+)$".toRegex()

        fun fromFile(file: File): TranslationMap {
            if (!file.exists()) {
                throw IllegalArgumentException("File $file does not exist!")
            }

            return file.bufferedReader().use { reader ->
                val map = ArrayMap<String>()

                reader.forEachLine { line ->
                    val result = regex.matchEntire(line) ?: return@forEachLine

                    val keyString = result.groupValues[1]

                    TranslationKey[keyString]?.let {
                        map[it.id] = result.groupValues[2]
                    }
                }

                if (map.isEmpty()) {
                    throw IllegalArgumentException("File $file is not a valid lang file!")
                }

                TranslationMap(file.nameWithoutExtension.lowercase(Locale.ROOT), map)
            }
        }
    }
}