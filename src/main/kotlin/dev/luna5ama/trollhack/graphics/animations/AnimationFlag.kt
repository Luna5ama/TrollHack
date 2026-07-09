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

package dev.luna5ama.trollhack.graphics.animations

import dev.luna5ama.trollhack.utils.state.FrameFloat

class AnimationFlag(private val interpolation: InterpolateFunction) {

    constructor(easing: Easing, length: Float) : this({ time, prev, current ->
        easing.incOrDec(Easing.toDelta(time, length), prev, current)
    })

    constructor(easing: Easing, length: () -> Float) : this({ time, prev, current ->
        easing.incOrDec(Easing.toDelta(time, length()), prev, current)
    })

    private var prev = 0.0f
    private var current = 0.0f
    private var time = System.currentTimeMillis()

    private val cachedValue by FrameFloat {
        interpolation.invoke(time, prev, current)
    }

    fun forceUpdate(prev: Float, current: Float) {
        this.prev = prev
        this.current = current
        time = System.currentTimeMillis()
    }

    fun getAndUpdate(input: Float): Float {
        return get(input, true)
    }

    fun get(input: Float, update: Boolean): Float {
//        val render = interpolation.invoke(time, prev, current)
        val render = cachedValue

        if (update && input != current) {
            prev = render
            current = input
            time = System.currentTimeMillis()
        }

        return render
    }
}