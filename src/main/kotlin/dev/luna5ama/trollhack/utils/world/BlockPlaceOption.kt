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
package dev.luna5ama.trollhack.utils.world

class BlockPlaceOption(
    val isEntityCollisionIgnored: Boolean,
    val isBlockCollisionIgnored: Boolean,
    val isAirPlaceIgnored: Boolean
) {

    constructor(vararg options: PlaceOptions) : this(
        contains(options, PlaceOptions.IGNORE_ENTITY),
        contains(options, PlaceOptions.IGNORE_BLOCK),
        contains(options, PlaceOptions.IGNORE_AIR)
    )

    companion object {
        private fun contains(options: Array<out PlaceOptions>, any: PlaceOptions): Boolean {
            return any in options
        }
    }
}