package dev.luna5ama.trollhack.manager

import dev.luna5ama.trollhack.utils.Profiler

// TODO: Complete this
abstract class AbstractManager {
    // Some managers don't need to be initialized explicitly
    open fun load(profilerScope: Profiler.ProfilerScope) {}
}