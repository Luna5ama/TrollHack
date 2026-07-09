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

import net.minecraft.core.BlockPos
class BlockEasingRender : DynamicBlockEasingRender {
    constructor(pos: BlockPos, movingLength: Float, fadingLength: Float): this(pos, movingLength, fadingLength, Easing.OUT_QUINT, Easing.OUT_QUINT)

    constructor(pos: BlockPos, movingLength: Number, fadingLength: Number, moveType: Easing, fadingType: Easing) : super(pos, { movingLength.toFloat() }, { fadingLength.toFloat() }, { moveType }, { fadingType })

    constructor(pos: BlockPos, movingLength: Number, fadingLength: Number) : this(pos, movingLength, fadingLength, Easing.OUT_QUINT, Easing.OUT_QUINT)
}