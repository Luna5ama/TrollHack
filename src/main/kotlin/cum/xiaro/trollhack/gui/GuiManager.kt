package cum.xiaro.trollhack.gui

import cum.xiaro.trollhack.util.collections.AliasSet
import cum.xiaro.trollhack.AsyncLoader
import cum.xiaro.trollhack.TrollHackMod
import cum.xiaro.trollhack.gui.clickgui.TrollClickGui
import cum.xiaro.trollhack.gui.hudgui.AbstractHudElement
import cum.xiaro.trollhack.gui.hudgui.TrollHudGui
import cum.xiaro.trollhack.util.ClassUtils.instance
import cum.xiaro.trollhack.util.TimeUnit
import cum.xiaro.trollhack.util.delegate.AsyncCachedValue
import kotlinx.coroutines.Deferred
import java.lang.reflect.Modifier
import kotlin.system.measureTimeMillis

internal object GuiManager : AsyncLoader<List<Class<out AbstractHudElement>>> {
    override var deferred: Deferred<List<Class<out AbstractHudElement>>>? = null
    private val hudElementSet = AliasSet<AbstractHudElement>()

    val hudElements by AsyncCachedValue(5L, TimeUnit.SECONDS) {
        hudElementSet.distinct().sortedBy { it.nameAsString }
    }

    override suspend fun preLoad0(): List<Class<out AbstractHudElement>> {
        val classes = AsyncLoader.classes.await()
        val list: List<Class<*>>

        val time = measureTimeMillis {
            val clazz = AbstractHudElement::class.java

            list = classes.asSequence()
                .filter { Modifier.isFinal(it.modifiers) }
                .filter { it.name.startsWith("cum.xiaro.trollhack.gui.hudgui.elements") }
                .filter { clazz.isAssignableFrom(it) }
                .sortedBy { it.simpleName }
                .toList()
        }

        TrollHackMod.logger.info("${list.size} hud elements found, took ${time}ms")

        @Suppress("UNCHECKED_CAST")
        return list as List<Class<out AbstractHudElement>>
    }

    override suspend fun load0(input: List<Class<out AbstractHudElement>>) {
        val time = measureTimeMillis {
            for (clazz in input) {
                register(clazz.instance)
            }
        }

        TrollHackMod.logger.info("${input.size} hud elements loaded, took ${time}ms")

        TrollClickGui.onGuiClosed()
        TrollHudGui.onGuiClosed()

        TrollClickGui.subscribe()
        TrollHudGui.subscribe()
    }

    internal fun register(hudElement: AbstractHudElement) {
        hudElementSet.add(hudElement)
        TrollHudGui.register(hudElement)
    }

    internal fun unregister(hudElement: AbstractHudElement) {
        hudElementSet.remove(hudElement)
        TrollHudGui.unregister(hudElement)
    }

    fun getHudElementOrNull(name: String?) = name?.let { hudElementSet[it] }
}