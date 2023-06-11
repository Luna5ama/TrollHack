package dev.luna5ama.trollhack.util.threads

import dev.fastmc.common.ParallelUtils
import kotlinx.coroutines.*
import net.minecraft.client.Minecraft
import net.minecraft.crash.CrashReport
import java.util.concurrent.ScheduledThreadPoolExecutor
import kotlin.math.max

private val defaultContext =
    CoroutineName("Troll Hack Default") +
        Dispatchers.Default +
        CoroutineExceptionHandler { _, throwable ->
            Minecraft.getMinecraft().crashed(CrashReport.makeCrashReport(throwable, "Troll Hack Default Scope"))
        }

/**
 * Common scope with [Dispatchers.Default]
 */
object DefaultScope : CoroutineScope by CoroutineScope(defaultContext) {
    val context = defaultContext
}

@OptIn(ExperimentalCoroutinesApi::class)
private val concurrentContext =
    CoroutineName("Troll Hack Concurrent") +
        Dispatchers.Default.limitedParallelism(max(ParallelUtils.CPU_THREADS / 2, 1)) +
        CoroutineExceptionHandler { _, throwable ->
            Minecraft.getMinecraft().crashed(CrashReport.makeCrashReport(throwable, "Troll Hack Concurrent Scope"))
        }

object ConcurrentScope :
    CoroutineScope by CoroutineScope(concurrentContext) {
    val context = concurrentContext
}

/**
 * Return true if the job is active, or false is not active or null
 */
val Job?.isActiveOrFalse get() = this?.isActive ?: false

suspend inline fun delay(timeMillis: Int) {
    delay(timeMillis.toLong())
}

private val backgroundPool = ScheduledThreadPoolExecutor(
    ParallelUtils.CPU_THREADS,
    CountingThreadFactory("Troll Hack Background") {
        isDaemon = true
        priority = 3
    }
)

private val backgroundContext =
    CoroutineName("Troll Hack Background") +
        backgroundPool.asCoroutineDispatcher() +
        CoroutineExceptionHandler { _, throwable ->
            Minecraft.getMinecraft().crashed(CrashReport.makeCrashReport(throwable, "Troll Hack Background Scope"))
        }

object BackgroundScope : CoroutineScope by CoroutineScope(backgroundContext) {
    val pool = backgroundPool
    val context = backgroundContext
}