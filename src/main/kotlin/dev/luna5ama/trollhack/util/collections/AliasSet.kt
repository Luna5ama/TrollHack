package dev.luna5ama.trollhack.util.collections

import dev.luna5ama.trollhack.util.interfaces.Alias
import java.util.concurrent.ConcurrentHashMap

class AliasSet<T : Alias>(
    map: MutableMap<String, T> = ConcurrentHashMap()
) : NameableSet<T>(map) {
    override fun add(element: T): Boolean {
        var modified = super.add(element)
        element.alias.forEach { alias ->
            val prevValue = map.put(alias.toString().lowercase(), element)
            if (prevValue != null && prevValue != element) {
                remove(prevValue)
            }
            modified = prevValue != element || modified
        }
        return modified
    }

    override fun remove(element: T): Boolean {
        var modified = super.remove(element)
        element.alias.forEach {
            modified = map.remove(it.toString().lowercase()) != null || modified
        }
        return modified
    }
}
