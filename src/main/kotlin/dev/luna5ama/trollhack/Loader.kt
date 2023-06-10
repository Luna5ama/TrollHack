package dev.luna5ama.trollhack

import dev.luna5ama.trollhack.command.CommandManager
import dev.luna5ama.trollhack.gui.GuiManager
import dev.luna5ama.trollhack.manager.ManagerLoader
import dev.luna5ama.trollhack.module.ModuleManager
import dev.luna5ama.trollhack.util.ClassUtils
import dev.luna5ama.trollhack.util.threads.DefaultScope
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

internal object LoaderWrapper {
    private val loaderList = ArrayList<AsyncLoader<*>>()

    init {
        loaderList.add(ModuleManager)
        loaderList.add(CommandManager)
        loaderList.add(ManagerLoader)
        loaderList.add(GuiManager)
    }

    @JvmStatic
    fun preLoadAll() {
        loaderList.forEach { it.preLoad() }
    }

    @JvmStatic
    fun loadAll() {
        runBlocking {
            loaderList.forEach { it.load() }
        }
    }
}

internal interface AsyncLoader<T> {
    var deferred: Deferred<T>?

    fun preLoad() {
        deferred = preLoadAsync()
    }

    private fun preLoadAsync(): Deferred<T> {
        return DefaultScope.async(singleContext) { preLoad0() }
    }

    suspend fun load() {
        load0((deferred ?: preLoadAsync()).await())
    }

    suspend fun preLoad0(): T
    suspend fun load0(input: T)

    companion object {
        @OptIn(ExperimentalCoroutinesApi::class)
        private val singleContext = Dispatchers.Default.limitedParallelism(1)

        val classes = DefaultScope.async(singleContext) {
            val list: List<Class<*>>
            val time = measureTimeMillis {
                list = ClassUtils.findClasses("dev.luna5ama.trollhack") {
                    !it.contains("mixins")
                }
            }

            TrollHackMod.logger.info("${list.size} classes found, took ${time}ms")
            list
        }
    }
}