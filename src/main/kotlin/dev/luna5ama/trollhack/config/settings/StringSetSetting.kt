package dev.luna5ama.trollhack.config.settings

import dev.luna5ama.trollhack.i18n.I18N
import dev.luna5ama.trollhack.utils.BiPredicate
import dev.luna5ama.trollhack.utils.Combiner
import dev.luna5ama.trollhack.utils.Predicate

class StringSetSetting(
    translateKey: String, i18N: I18N,
    defaultValue: Set<String>, description: String,
    visibility: Predicate<Set<String>>,
    onModified: MutableList<BiPredicate<Set<String>, Set<String>>>,
    transformer: Combiner<Set<String>>,
    defaultName: String = translateKey
) : AbstractSetSetting<String>(
    translateKey, i18N,
    defaultValue, description,
    visibility, onModified, transformer,
    defaultName
) {
    override fun element2String(element: String): String {
        return element
    }

    override fun string2Element(string: String): String {
        return string
    }
}