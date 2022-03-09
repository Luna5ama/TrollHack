package me.luna.trollhack.util.extension

import me.luna.trollhack.translation.TranslationKey
import me.luna.trollhack.util.interfaces.DisplayEnum
import me.luna.trollhack.util.interfaces.Nameable

val DisplayEnum.rootName: String
    get() = (displayName as? TranslationKey)?.rootString ?: displayName.toString()

val Nameable.rootName: String
    get() = (name as? TranslationKey)?.rootString ?: name.toString()