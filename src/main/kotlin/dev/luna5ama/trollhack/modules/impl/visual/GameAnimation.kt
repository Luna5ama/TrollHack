package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import kotlin.math.roundToInt

object GameAnimation : Module("Game Animation", category = Category.RENDER) {
    val hotbar by setting("Hotbar", true)
    private var current = Float.NaN
    private var lastNanos = 0L

    init {
        onDisabled {
            current = Float.NaN
            lastNanos = 0L
        }
    }

    @JvmStatic
    fun hotbarSelectionX(vanillaX: Int): Int {
        if (!isEnabled || !hotbar) {
            current = Float.NaN
            lastNanos = 0L
            return vanillaX
        }
        val now = System.nanoTime()
        if (!current.isFinite() || kotlin.math.abs(current - vanillaX) > 240f) current = vanillaX.toFloat()
        val delta = if (lastNanos == 0L) 0f else ((now - lastNanos) / 150_000_000.0f).coerceIn(0f, 1f)
        lastNanos = now
        val eased = 1f - (1f - delta) * (1f - delta) * (1f - delta)
        current += (vanillaX - current) * eased
        return current.roundToInt()
    }
}
