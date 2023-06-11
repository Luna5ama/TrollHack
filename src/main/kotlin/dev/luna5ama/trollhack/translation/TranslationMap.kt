package dev.luna5ama.trollhack.translation

import dev.fastmc.common.collection.FastIntMap

class TranslationMap private constructor(
    val language: String,
    private val translations: FastIntMap<String>
) {

    operator fun get(key: TranslationKey): String {
        return translations[key.id] ?: key.rootString
    }

    companion object {
        private val regex = "^(\\\$.+\\\$)=(.+)$".toRegex()

        fun fromString(language: String, input: String): TranslationMap {
            val map = FastIntMap<String>()

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