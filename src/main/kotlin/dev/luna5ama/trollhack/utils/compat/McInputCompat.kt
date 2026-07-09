package dev.luna5ama.trollhack.utils.compat

import net.minecraft.client.player.ClientInput
import net.minecraft.world.entity.player.Input

val ClientInput.forwardImpulseCompat: Float
    get() = when {
        keyPresses.forward() && !keyPresses.backward() -> 1.0f
        keyPresses.backward() && !keyPresses.forward() -> -1.0f
        else -> 0.0f
    }

val ClientInput.leftImpulseCompat: Float
    get() = when {
        keyPresses.left() && !keyPresses.right() -> 1.0f
        keyPresses.right() && !keyPresses.left() -> -1.0f
        else -> 0.0f
    }

fun ClientInput.clearDirectionalInputCompat() {
    keyPresses = Input(false, false, false, false, keyPresses.jump(), keyPresses.shift(), keyPresses.sprint())
}
