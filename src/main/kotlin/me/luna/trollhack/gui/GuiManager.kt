package me.luna.trollhack.gui

import kotlinx.coroutines.Deferred
import me.luna.trollhack.TrollHackMod
import me.luna.trollhack.gui.clickgui.TrollClickGui
import me.luna.trollhack.gui.hudgui.AbstractHudElement
import me.luna.trollhack.gui.hudgui.TrollHudGui
import me.luna.trollhack.util.ClassUtils.instance
import me.luna.trollhack.util.TimeUnit
import me.luna.trollhack.util.collections.AliasSet
import me.luna.trollhack.util.delegate.AsyncCachedValue
import java.lang.reflect.Modifier
import kotlin.system.measureTimeMillis

internal object GuiManager : me.luna.trollhack.AsyncLoader<List<Class<out AbstractHudElement>>> {
    override var deferred: Deferred<List<Class<out AbstractHudElement>>>? = null
    private val hudElementSet = AliasSet<AbstractHudElement>()

    val hudElements by AsyncCachedValue(5L, TimeUnit.SECONDS) {
        hudElementSet.distinct().sortedBy { it.nameAsString }
    }

    override suspend fun preLoad0(): List<Class<out AbstractHudElement>> {
        val classes = me.luna.trollhack.AsyncLoader.classes.await()
        val list: List<Class<*>>

        val time = measureTimeMillis {
            val clazz = AbstractHudElement::class.java

            list = classes.asSequence()
                .filter { Modifier.isFinal(it.modifiers) }
                .filter { it.name.startsWith("me.luna.trollhack.gui.hudgui.elements") }
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