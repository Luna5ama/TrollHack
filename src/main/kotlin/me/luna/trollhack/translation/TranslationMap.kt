package me.luna.trollhack.translation

import me.luna.trollhack.util.collections.ArrayMap
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

        fun fromString(language: String, input: String): TranslationMap {
            val map = ArrayMap<String>()

            input.lines().forEach { line ->
                val result = regex.matchEntire(line) ?: return@forEach

                val keyString = result.groupValues[1]

                TranslationKey[keyString]?.let {
                    map[it.id] = result.groupValues[2]
                }
            }

            return TranslationMap(language, map)
        }
    }
}