package me.luna.trollhack.util.threads

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private val pool0 = Runtime.getRuntime().availableProcessors().let { cpuCount ->
    ThreadPoolExecutor(
        cpuCount * 4,
        cpuCount * 4,
        15L,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(),
        CountingThreadFactory("Troll Hack Pool")
    ).apply {
        allowCoreThreadTimeOut(true)
    }
}

private val context0 = pool0.asCoroutineDispatcher()

/**
 * Scope for heavy loaded task in Troll Hack
 */
object TrollHackScope : CoroutineScope by CoroutineScope(context0) {
    val pool = pool0
    val context = context0
}

private val backgroundPool0 = Runtime.getRuntime().availableProcessors().let { cpuCount ->
    ThreadPoolExecutor(
        cpuCount,
        cpuCount,
        15L,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(),
        CountingThreadFactory("Troll Hack Pool") { priority = 1 }
    ).apply {
        allowCoreThreadTimeOut(true)
    }
}

private val backgroundContext0 = backgroundPool0.asCoroutineDispatcher()

/**
 * Scope for background loads task in Troll Hack
 */
object TrollHackBackgroundScope : CoroutineScope by CoroutineScope(backgroundContext0) {
    val pool = pool0
    val context = context0
}