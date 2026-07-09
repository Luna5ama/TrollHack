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

package dev.luna5ama.trollhack.utils.delegates

import dev.luna5ama.trollhack.utils.timing.TickTimer
import dev.luna5ama.trollhack.utils.timing.TimeUnit
import java.util.function.Supplier
import kotlin.experimental.ExperimentalTypeInference
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@OptIn(ExperimentalTypeInference::class)
open class DynamicCachedValue<T>(
    @BuilderInference private val invalidValue: T? = null,
    @BuilderInference private val block: () -> T
) : Supplier<T>, ReadOnlyProperty<Any?, T> {
    private var value: T? = null
    private val timer = TickTimer()

    fun get(updateTime: Int, timeUnit: TimeUnit): T {
        return get(updateTime * timeUnit.multiplier)
    }

    fun get(updateTime: Long, timeUnit: TimeUnit): T {
        return get(updateTime * timeUnit.multiplier)
    }

    fun get(updateTime: Int): T {
        val cached = value

        return if (cached == null || cached == invalidValue || timer.tick(updateTime)) {
            timer.reset()
            block().also { value = it }
        } else {
            cached
        }
    }

    fun get(updateTime: Long): T {
        val cached = value

        return if (cached == null || cached == invalidValue || timer.tick(updateTime)) {
            timer.reset()
            block().also { value = it }
        } else {
            cached
        }
    }

    fun getForce(): T {
        return block().also { value = it }
    }

    fun getLazy(): T? {
        return value
    }

    fun updateForce() {
        timer.reset()
        value = block()
    }

    fun updateLazy() {
        value = null
    }

    fun wrapped(updateTime: Long): ReadWriteProperty<Any?, T> {
        return object : ReadWriteProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return this@DynamicCachedValue.get(updateTime)
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                this@DynamicCachedValue.value = value
            }
        }
    }

    fun wrapped(updateTime: Long, timeUnit: TimeUnit): ReadWriteProperty<Any?, T> {
        return object : ReadWriteProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return this@DynamicCachedValue.get(updateTime, timeUnit)
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                this@DynamicCachedValue.value = value
            }
        }
    }

    override fun get(): T {
        return getForce()
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return getForce()
    }
}