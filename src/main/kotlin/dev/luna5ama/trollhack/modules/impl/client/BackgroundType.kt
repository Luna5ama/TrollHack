package dev.luna5ama.trollhack.modules.impl.client

import dev.luna5ama.trollhack.RenderSystem
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.graphics.shader.bg.IgniteParticles

enum class BackgroundType(override val displayName: CharSequence) : Displayable {
    NONE("None") {
        override fun draw(alpha: Float) {}
    },
    PARTICLES("Particles") {
        override fun draw(alpha: Float) {
            RenderSystem.particleSystem.render(alpha)
        }
    },
    IGNITE("Ignite") {
        override fun draw(alpha: Float) {
            IgniteParticles.draw(alpha)
        }
    };

    abstract fun draw(alpha: Float)
}