package dev.luna5ama.trollhack.util.extension

import dev.luna5ama.trollhack.translation.TranslationKey
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.interfaces.Nameable

val DisplayEnum.rootName: String
    get() = (displayName as? TranslationKey)?.rootString ?: displayName.toString()

val Nameable.rootName: String
    get() = (name as? TranslationKey)?.rootString ?: name.toString()