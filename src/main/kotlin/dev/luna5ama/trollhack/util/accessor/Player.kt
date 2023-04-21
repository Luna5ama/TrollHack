package dev.luna5ama.trollhack.util.accessor

import net.minecraft.client.multiplayer.PlayerControllerMP

var PlayerControllerMP.blockHitDelay: Int
    get() = (this as dev.luna5ama.trollhack.mixins.accessor.player.AccessorPlayerControllerMP).blockHitDelay
    set(value) {
        (this as dev.luna5ama.trollhack.mixins.accessor.player.AccessorPlayerControllerMP).blockHitDelay = value
    }

val PlayerControllerMP.currentPlayerItem: Int get() = (this as dev.luna5ama.trollhack.mixins.accessor.player.AccessorPlayerControllerMP).currentPlayerItem

fun PlayerControllerMP.syncCurrentPlayItem() =
    (this as dev.luna5ama.trollhack.mixins.accessor.player.AccessorPlayerControllerMP).trollInvokeSyncCurrentPlayItem()