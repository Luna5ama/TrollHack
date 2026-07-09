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

package dev.luna5ama.trollhack.utils.math.vectors

import dev.luna5ama.trollhack.utils.Displayable

enum class HAlign(override val displayName: CharSequence, val multiplier: Float, val offset: Float) : Displayable {
    LEFT("Left", 0.0f, -1.0f),
    CENTER("Center", 0.5f, 0.0f),
    RIGHT("Right", 1.0f, 1.0f)
}

enum class VAlign(override val displayName: CharSequence, val multiplier: Float, val offset: Float) : Displayable {
    TOP("Top", 0.0f, -1.0f),
    CENTER("Center", 0.5f, 0.0f),
    BOTTOM("Bottom", 1.0f, 1.0f)
}