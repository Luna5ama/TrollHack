package cum.xiaro.trollhack

import cum.xiaro.trollhack.command.CommandManager
import cum.xiaro.trollhack.gui.GuiManager
import cum.xiaro.trollhack.manager.ManagerLoader
import cum.xiaro.trollhack.module.ModuleManager
import cum.xiaro.trollhack.util.ClassUtils
import cum.xiaro.trollhack.util.threads.mainScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
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
                list = ClassUtils.findClasses("cum.xiaro.trollhack") {
                    val subString = it.substring(20)
                    !subString.startsWith("mixin")
                        && !subString.startsWith("accessor")
                        && !subString.startsWith("patch")
                }
            }

            TrollHackMod.logger.info("${list.size} classes found, took ${time}ms")
            list
        }
    }
}