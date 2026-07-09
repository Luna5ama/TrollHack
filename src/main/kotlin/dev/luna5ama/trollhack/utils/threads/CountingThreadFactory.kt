/*
 * Copyright (c) 2021-2022, SagiriXiguajerry. All rights reserved.
 * This repository will be transformed to SuperMic_233.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package dev.luna5ama.trollhack.utils.threads

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

open class CountingThreadFactory(private val prefix: String) : ThreadFactory {
    protected val count = AtomicInteger(1)

    override fun newThread(r: Runnable): Thread {
        return Thread(r, "$prefix-${count.getAndIncrement()}").apply {
            isDaemon = true
        }
    }
}