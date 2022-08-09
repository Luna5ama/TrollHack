package me.luna.trollhack

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import me.luna.trollhack.command.CommandManager
import me.luna.trollhack.gui.GuiManager
import me.luna.trollhack.manager.ManagerLoader
import me.luna.trollhack.module.ModuleManager
import me.luna.trollhack.util.ClassUtils
import me.luna.trollhack.util.threads.mainScope
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
        return mainScope.async { preLoad0() }
    }

    suspend fun load() {
        load0((deferred ?: preLoadAsync()).await())
    }

    suspend fun preLoad0(): T
    suspend fun load0(input: T)

    companion object {
        val classes = mainScope.async {
            val list: List<Class<*>>
            val time = measureTimeMillis {
                list = ClassUtils.findClasses("me.luna.trollhack") {
                    !it.contains("mixins")
                }
            }

            TrollHackMod.logger.info("${list.size} classes found, took ${time}ms")
            list
        }
    }
}