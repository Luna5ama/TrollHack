package cum.xiaro.trollhack.util.threads

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.newSingleThreadContext

/**
 * Single thread scope to use in Troll Hack
 */
@Suppress("EXPERIMENTAL_API_USAGE")
val mainScope = CoroutineScope(newSingleThreadContext("Troll Hack Main"))

/**
 * Common scope with [Dispatchers.Default]
 */
val defaultScope = CoroutineScope(Dispatchers.Default)

/**
 * Return true if the job is active, or false is not active or null
 */
inline val Job?.isActiveOrFalse get() = this?.isActive ?: false

suspend inline fun delay(timeMillis: Int) {
    kotlinx.coroutines.delay(timeMillis.toLong())
}