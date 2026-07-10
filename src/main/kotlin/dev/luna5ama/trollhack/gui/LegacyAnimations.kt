package dev.luna5ama.trollhack.gui

import androidx.compose.animation.core.Easing
import kotlin.math.pow

internal val LegacyOutCubic = Easing { progress ->
    1f - (1f - progress).pow(3)
}

internal val LegacyOutQuart = Easing { progress ->
    1f - (1f - progress).pow(4)
}

internal const val LegacyGuiDuration = 400
internal const val LegacyWindowDuration = 300
internal const val LegacyComponentDuration = 200
internal const val LegacyFillDuration = 300
