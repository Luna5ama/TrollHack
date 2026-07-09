package dev.luna5ama.trollhack.utils.threads

import com.mojang.blaze3d.systems.RenderSystem
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.timing.TickTimer
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.system.measureTimeMillis

fun newCoroutineScope(nThreads: Int, name: String): CoroutineScope {
    require(nThreads >= 1) { "Expected at least one thread, but $nThreads specified" }
    val threadNo = AtomicInteger()
    val executor = Executors.newScheduledThreadPool(nThreads) { runnable ->
        val t = Thread(runnable, if (nThreads == 1) name else name + "-" + threadNo.incrementAndGet())
        t.isDaemon = true
        t
    }
    return CoroutineScope(executor.asCoroutineDispatcher())
}

val pool0 = Runtime.getRuntime().availableProcessors().let { cpuCount ->
    // we reduce the threads to avoid low fps
    val maxSize = max(cpuCount * 2, 8)
    ThreadPoolExecutor(
//        cpuCount * 2,
        4,
        maxSize,
        5L,
        TimeUnit.SECONDS,
        SynchronousQueue(),
        CountingThreadFactory("${TrollHackMod.NAME}-Task"),
    ).apply {
        rejectedExecutionHandler = RejectedExecutionHandler { runnable, executor ->
            TrollHackMod.LOGGER.debug("The task {} was rejected from {}, \n force executing it.", runnable, executor)
            runnable.run()
        }
    }
}

private val context0 = pool0.asCoroutineDispatcher()

internal object Coroutine : CoroutineScope by CoroutineScope(context0) {
    val pool = pool0
    val context = context0
}

val Job?.isActiveOrFalse get() = this?.isActive == true

internal object RenderThreadExecutor : AbstractExecutorService() {
    private val pendingTasks = ObjectLists.synchronize(ObjectArrayList<Any>())

    override fun execute(_command: Runnable) {
        val command = {
            val time = measureTimeMillis {
                _command.run()
            }
            if (time > ClientSettings.renderThreadTimeThreshold) {
                TrollHackMod.LOGGER.warn("An task scheduled to render thread has taken more " +
                        "than ${ClientSettings.renderThreadTimeThreshold}ms: time elapsed ${time}ms after execution")
            }
        }
        if (NonNullContext.instance == null) TrollHackMod.LOGGER.warn("You are using RenderThreadScheduler out of the game.")
        if (RenderSystem.isOnRenderThread()) {
            TrollHackMod.LOGGER.warn("You are using RenderThreadScheduler in RenderThread. This may cause dead lock.")
            command()
        }
        else {
            pendingTasks.add(command)
            RenderSystem.queueFencedTask {
                if (command in pendingTasks) {
                    command()
                    pendingTasks.remove(command)
                }
            }
        }
    }

    override fun shutdown() {
        TrollHackMod.LOGGER.warn("Render Thread cannot be terminated, shutdown() will do nothing.")
    }

    override fun shutdownNow(): MutableList<Runnable> {
        val cancelledTasks = ArrayList(pendingTasks.filterIsInstance<Runnable>())
        pendingTasks.clear()
        return cancelledTasks
    }

    override fun isShutdown(): Boolean {
        return false
    }

    override fun isTerminated(): Boolean {
        return false
    }

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        val timer = TickTimer(unit)
        while (true) {
            Thread.sleep(10)
            if (pendingTasks.isEmpty() || timer.tick(timeout)) break
        }
        return pendingTasks.isEmpty()
    }
}

/*internal object RenderThreadExecutor : ExecutorService {
    private val pendingTasks = ObjectLists.synchronize(ObjectArrayList<Any>())

    override fun execute(command: Runnable) {
        if (RenderSystem.isOnRenderThread()) command.run()
        else {
            pendingTasks.add(command)
            RenderSystem.queueFencedTask {
                if (command in pendingTasks) {
                    command.run()
                    pendingTasks.remove(command)
                }
            }
        }
    }

    override fun shutdown() {}

    override fun shutdownNow(): MutableList<Runnable> {
        val cancelledTasks = ArrayList(pendingTasks.filterIsInstance<Runnable>())
        pendingTasks.clear()
        return cancelledTasks
    }

    override fun isShutdown(): Boolean {
        return false
    }

    override fun isTerminated(): Boolean {
        return false
    }

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        val timer = TickTimer(unit)
        while (true) {
            Thread.sleep(10)
            if (pendingTasks.isEmpty() || timer.tick(timeout)) break
        }
        return pendingTasks.isEmpty()
    }

    override fun <T : Any?> submit(_task: Callable<T>): Future<T> {
        return object : Future<T> {
            private val task = _task
            @Volatile
            private var done = false
            private var result: Result<T> by Delegates.notNull()

            init {
                if (RenderSystem.isOnRenderThread()) {
                    result = runCatching { _task.call() }
                    done = true
                } else {
                    pendingTasks.add(task)
                    RenderSystem.queueFencedTask {
                        if (task in pendingTasks) {
                            result = runCatching { task.call() }
                            done = true
                            pendingTasks.remove(task)
                        }
                    }
                }
            }

            override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
                return pendingTasks.remove(task)
            }

            override fun isCancelled(): Boolean {
                return task !in pendingTasks
            }

            override fun isDone(): Boolean {
                return done
            }

            override fun get(): T {
                while (true) {
                    if (isCancelled) throw CancellationException()
                    try {
                        return result.getOrThrow()
                    } catch (e: IllegalStateException) {
                        continue
                    } catch (e: InterruptedException) {
                        throw e
                    } catch (e: Exception) {
                        throw ExecutionException(e)
                    }
                }
            }

            override fun get(timeout: Long, unit: TimeUnit): T {
                val timer = TickTimer(unit)
                while (!timer.tick(timeout)) {
                    if (isCancelled) throw CancellationException()
                    try {
                        return result.getOrThrow()
                    } catch (e: IllegalStateException) {
                        continue
                    } catch (e: InterruptedException) {
                        throw e
                    } catch (e: Exception) {
                        throw ExecutionException(e)
                    }
                }
                throw TimeoutException()
            }
        }
    }

    override fun <T : Any?> submit(_task: Runnable, _result: T): Future<T> {
        return object : Future<T> {
            private val task = _task
            @Volatile
            private var done = false
            private var result: Result<T> by Delegates.notNull()

            init {
                if (RenderSystem.isOnRenderThread()) {
                    result = runCatching { _task.run(); _result }
                    done = true
                } else {
                    pendingTasks.add(task)
                    RenderSystem.queueFencedTask {
                        if (task in pendingTasks) {
                            result = runCatching { _task.run(); _result }
                            done = true
                            pendingTasks.remove(task)
                        }
                    }
                }
            }

            override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
                return pendingTasks.remove(task)
            }

            override fun isCancelled(): Boolean {
                return task !in pendingTasks
            }

            override fun isDone(): Boolean {
                return done
            }

            override fun get(): T {
                while (true) {
                    if (isCancelled) throw CancellationException()
                    try {
                        return result.getOrThrow()
                    } catch (e: IllegalStateException) {
                        continue
                    } catch (e: InterruptedException) {
                        throw e
                    } catch (e: Exception) {
                        throw ExecutionException(e)
                    }
                }
            }

            override fun get(timeout: Long, unit: TimeUnit): T {
                val timer = TickTimer(unit)
                while (!timer.tick(timeout)) {
                    if (isCancelled) throw CancellationException()
                    try {
                        return result.getOrThrow()
                    } catch (e: IllegalStateException) {
                        continue
                    } catch (e: InterruptedException) {
                        throw e
                    } catch (e: Exception) {
                        throw ExecutionException(e)
                    }
                }
                throw TimeoutException()
            }
        }
    }

    override fun submit(_task: Runnable): Future<*> {
        return object : Future<Unit?> {
            private val task = _task
            @Volatile
            private var done = false
            private var result: Result<Unit?> by Delegates.notNull()

            init {
                if (RenderSystem.isOnRenderThread()) {
                    result = runCatching { _task.run(); null }
                    done = true
                } else {
                    pendingTasks.add(task)
                    RenderSystem.queueFencedTask {
                        if (task in pendingTasks) {
                            result = runCatching { _task.run(); null }
                            done = true
                            pendingTasks.remove(task)
                        }
                    }
                }
            }

            override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
                return pendingTasks.remove(task)
            }

            override fun isCancelled(): Boolean {
                return task !in pendingTasks
            }

            override fun isDone(): Boolean {
                return done
            }

            override fun get(): Unit? {
                while (true) {
                    if (isCancelled) throw CancellationException()
                    try {
                        return result.getOrThrow()
                    } catch (e: IllegalStateException) {
                        continue
                    } catch (e: InterruptedException) {
                        throw e
                    } catch (e: Exception) {
                        throw ExecutionException(e)
                    }
                }
            }

            override fun get(timeout: Long, unit: TimeUnit): Unit? {
                val timer = TickTimer(unit)
                while (!timer.tick(timeout)) {
                    if (isCancelled) throw CancellationException()
                    try {
                        return result.getOrThrow()
                    } catch (e: IllegalStateException) {
                        continue
                    } catch (e: InterruptedException) {
                        throw e
                    } catch (e: Exception) {
                        throw ExecutionException(e)
                    }
                }
                throw TimeoutException()
            }
        }
    }

    override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>): MutableList<Future<T>> {
        return tasks.map(this::submit).toMutableList()
    }

    override fun <T : Any?> invokeAll(
        tasks: MutableCollection<out Callable<T>>,
        timeout: Long,
        unit: TimeUnit
    ): MutableList<Future<T>> {
        val futures = invokeAll(tasks)
    }

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>): T {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit): T {
        TODO("Not yet implemented")
    }
}*/

internal object RenderThreadCoroutine : CoroutineScope by CoroutineScope(RenderThreadExecutor.asCoroutineDispatcher())
