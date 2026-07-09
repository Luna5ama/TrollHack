package dev.luna5ama.trollhack.utils

import kotlin.system.measureTimeMillis

open class Profiler {
    private val sections0 = mutableListOf<ProfilerScope>()
    val sections: List<ProfilerScope> get() = sections0

    fun clear() = sections0.clear()

    fun register(profilerScope: ProfilerScope) = sections0.add(profilerScope)

    fun query(name: CharSequence, function: ProfilerScope.() -> Unit) {
        val section = ProfilerScope(name, 0, 0)
        register(section)
        val time = measureTimeMillis { section.function() }
        section.blocking = time
    }

    operator fun invoke(name: CharSequence, function: ProfilerScope.() -> Unit) = query(name, function)

    operator fun get(key: String) = sections0.find { it.name == key }

    override fun toString(): String {
        return "Profiler(sections=${sections.joinToString("\n")})"
    }

    data class ProfilerScope(override val name: CharSequence, var blocking: Long, var nonblocking: Long) : Nameable

    data object BootstrapProfiler : Profiler()
}
