package dev.luna5ama.trollhack.event.impl.render

import com.mojang.blaze3d.vertex.PoseStack
import dev.luna5ama.trollhack.event.api.Cancellable
import dev.luna5ama.trollhack.event.api.EventBus
import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.IPosting
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.world.entity.Entity

sealed class RenderEntityEvent(
    val entity: Entity,
) : IEvent, Cancellable() {

    abstract fun render()

    sealed class All(
        entity: Entity,
        val state: EntityRenderState?,
        private val matrices: PoseStack?,
        private val vertexConsumer: MultiBufferSource?,
        private val renderer: EntityRenderer<Entity, EntityRenderState>?,
        private val light: Int
        ) : RenderEntityEvent(entity) {

        override fun render() {
            // Entity rendering moved to the submit pipeline in 1.21.11.
        }

        class Pre(
            entity: Entity,
            state: EntityRenderState?,
            matrices: PoseStack?,
            vertexConsumer: MultiBufferSource?,
            renderer: EntityRenderer<Entity, EntityRenderState>?,
            light: Int
        ) : All(entity, state, matrices, vertexConsumer, renderer, light), IPosting by Companion {
            companion object : EventBus()
        }

        class Post(
            entity: Entity,
            state: EntityRenderState?,
            matrices: PoseStack?,
            vertexConsumer: MultiBufferSource?,
            renderer: EntityRenderer<Entity, EntityRenderState>?,
            light: Int
        ) : All(entity, state, matrices, vertexConsumer, renderer, light), IPosting by Companion {
            companion object : EventBus()
        }
    }

    companion object {
        @JvmStatic
        var renderingEntities = false
    }
}
