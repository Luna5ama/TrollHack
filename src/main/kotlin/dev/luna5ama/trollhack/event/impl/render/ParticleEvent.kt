package dev.luna5ama.trollhack.event.impl.render

import dev.luna5ama.trollhack.event.api.*
import net.minecraft.client.particle.Particle
import net.minecraft.core.particles.ParticleOptions

sealed class ParticleEvent : IEvent {
    class AddParticle(
        val particle: Particle,
    ) : ParticleEvent(), ICancellable by Cancellable(),IPosting by Companion {
        companion object : EventBus()
    }

    class AddEmmiter(
        val emmiter: ParticleOptions,
    ) : ParticleEvent(), ICancellable by Cancellable(),IPosting by Companion {
        companion object : EventBus()
    }
}