package cum.xiaro.trollhack.util.collections

import cum.xiaro.trollhack.util.interfaces.Nameable
import java.util.concurrent.ConcurrentHashMap

open class NameableSet<T : Nameable>(
    protected val map: MutableMap<String, T> = ConcurrentHashMap()
) : AbstractMutableSet<T>() {

    override val size get() = map.size

    fun containsName(name: String): Boolean = map.containsKey(name.lowercase())

    fun containsNames(names: Iterable<String>): Boolean = names.all { containsName(it) }

    fun containsNames(names: Array<String>): Boolean = names.all { containsName(it) }

    override fun contains(element: T): Boolean {
        return map.containsKey(element.name.toString().lowercase())
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return elements.all { contains(it) }
    }

    override fun iterator() = map.values.iterator()

    operator fun get(name: String) = map[name.lowercase()]

    fun getOrPut(name: String, value: () -> T) = get(name) ?: value().also { add(it) }

    override fun add(element: T) = map.put(element.name.toString().lowercase(), element) == null

    override fun addAll(elements: Collection<T>): Boolean {
        var modified = false
        elements.forEach {
            modified = add(it) || modified
        }
        return modified
    }

    override fun remove(element: T) = map.remove(element.name.toString().lowercase()) != null

    override fun removeAll(elements: Collection<T>): Boolean {
        var modified = false
        elements.forEach {
            modified = remove(it) || modified
        }
        return modified
    }

    override fun clear() {
        map.clear()
    }

}
