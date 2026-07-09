package dev.luna5ama.trollhack.utils.threads

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger


class MinPriorityThreadFactory : ThreadFactory {
    private val poolNumber = AtomicInteger(1)
    private var group = Thread.currentThread().threadGroup
    private val threadNumber = AtomicInteger(1)
    private var namePrefix = "pool-min-" + poolNumber.getAndIncrement() + "-thread-"

    override fun newThread(r: Runnable): Thread {
        val name = namePrefix + threadNumber.getAndIncrement()
        val t = Thread(group, r, name)
        t.setDaemon(true)
        t.setPriority(Thread.MIN_PRIORITY)
        return t
    }

    companion object {
        fun newFixedThreadPool(): ExecutorService {
            return Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                MinPriorityThreadFactory()
            )
        }
    }
}