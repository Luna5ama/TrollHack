package dev.luna5ama.trollhack.config.settings

import dev.luna5ama.trollhack.i18n.I18N
import dev.luna5ama.trollhack.utils.BiPredicate
import dev.luna5ama.trollhack.utils.Combiner
import dev.luna5ama.trollhack.utils.Predicate

abstract class AbstractSteppingRangedSetting<V : Comparable<V>, S : AbstractSteppingRangedSetting<V, S>>(
    translateKey: String, i18N: I18N,
    defaultValue: V, range: ClosedRange<V>, val step: V,
    description: String, visibility: Predicate<V>,
    onModified: MutableList<BiPredicate<V, V>>, transformer: Combiner<V>,
    defaultName: String = translateKey
) : AbstractRangedSetting<V, S>(
    translateKey, i18N,
    defaultValue, range, description,
    visibility, onModified, transformer,
    defaultName
)