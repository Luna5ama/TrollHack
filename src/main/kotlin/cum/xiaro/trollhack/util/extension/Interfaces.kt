package cum.xiaro.trollhack.util.extension

import cum.xiaro.trollhack.util.interfaces.DisplayEnum
import cum.xiaro.trollhack.util.interfaces.Nameable
import cum.xiaro.trollhack.util.translation.TranslationKey

val DisplayEnum.rootName: String
    get() = (displayName as? TranslationKey)?.rootString ?: displayName.toString()

val Nameable.rootName: String
    get() = (name as? TranslationKey)?.rootString ?: name.toString()