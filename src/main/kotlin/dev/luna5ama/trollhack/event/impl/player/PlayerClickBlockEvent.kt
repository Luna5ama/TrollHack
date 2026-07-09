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

package dev.luna5ama.trollhack.event.impl.player


import dev.luna5ama.trollhack.event.api.*
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

class PlayerClickBlockEvent(val pos: BlockPos, val facing: Direction) : IEvent, ICancellable by Cancellable(), IPosting by Companion {
    companion object : EventBus()
}