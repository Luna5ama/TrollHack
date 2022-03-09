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