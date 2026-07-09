package dev.luna5ama.trollhack.utils.extension

import com.google.common.util.concurrent.AtomicDouble
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty

fun <T> AtomicReference<T>.getOrElse(provider: () -> T): T = this.get() ?: provider()

fun <T> AtomicReference<T>.getOrElse(provider: T): T = this.get() ?: provider

fun <T> AtomicReference<T>.isNull(): Boolean = this.get() == null

fun <T> AtomicReference<T>.isNotNull(): Boolean = this.get() != null

operator fun <T> AtomicReference<T>.getValue(thisRef: Any?, property: KProperty<*>): T = this.get()

operator fun <T> AtomicReference<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) = this.set(value)

operator fun AtomicDouble.getValue(thisRef: Any?, property: KProperty<*>): Double = this.get()

operator fun AtomicDouble.setValue(thisRef: Any?, property: KProperty<*>, value: Double) = this.set(value)

operator fun AtomicInteger.getValue(thisRef: Any?, property: KProperty<*>): Int = this.get()

operator fun AtomicInteger.setValue(thisRef: Any?, property: KProperty<*>, value: Int) = this.set(value)

operator fun AtomicLong.getValue(thisRef: Any?, property: KProperty<*>): Long = this.get()

operator fun AtomicLong.setValue(thisRef: Any?, property: KProperty<*>, value: Long) = this.set(value)

operator fun AtomicBoolean.getValue(thisRef: Any?, property: KProperty<*>): Boolean = this.get()

operator fun AtomicBoolean.setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) = this.set(value)