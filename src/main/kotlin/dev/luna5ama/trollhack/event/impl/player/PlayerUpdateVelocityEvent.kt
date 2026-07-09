package dev.luna5ama.trollhack.event.impl.player

import dev.luna5ama.trollhack.event.api.Cancellable
import dev.luna5ama.trollhack.event.api.EventBus
import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.IPosting
import net.minecraft.world.phys.Vec3

class PlayerUpdateVelocityEvent(
    var movementInput: Vec3, var speed: Float, var yaw: Float, var velocity: Vec3
) : Cancellable(), IEvent, IPosting by Companion {
    companion object : EventBus()
}