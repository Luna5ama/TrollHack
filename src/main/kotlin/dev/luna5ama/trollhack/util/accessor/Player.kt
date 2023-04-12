package dev.luna5ama.trollhack.util.accessor

import dev.luna5ama.trollhack.mixins.accessor.player.AccessorPlayerControllerMP
import dev.luna5ama.trollhack.util.Wrapper
import net.minecraft.client.multiplayer.PlayerControllerMP

var PlayerControllerMP.blockHitDelay: Int
    get() = (this as AccessorPlayerControllerMP).blockHitDelay
    set(value) {
        (this as AccessorPlayerControllerMP).blockHitDelay = value
    }

val PlayerControllerMP.currentPlayerItem: Int get() = (this as AccessorPlayerControllerMP).currentPlayerItem

fun PlayerControllerMP.syncCurrentPlayItem() {
    if (Wrapper.player == null) return
    (this as AccessorPlayerControllerMP).trollInvokeSyncCurrentPlayItem()
}