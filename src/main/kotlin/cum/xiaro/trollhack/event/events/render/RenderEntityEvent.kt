package cum.xiaro.trollhack.event.events.render

import cum.xiaro.trollhack.event.Cancellable
import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting
import net.minecraft.client.model.ModelBase
import net.minecraft.client.renderer.entity.Render
import net.minecraft.entity.Entity

sealed class RenderEntityEvent(
    val entity: Entity,
) : Event, Cancellable() {

    abstract fun render()

    sealed class All(
        entity: Entity,
        private val x: Double,
        private val y: Double,
        private val z: Double,
        private val yaw: Float,
        private val partialTicks: Float,
        private val render: Render<Entity>,
    ) : RenderEntityEvent(entity) {
        override fun render() {
            render.doRender(entity, x, y, z, yaw, partialTicks)
        }

        class Pre(
            entity: Entity,
            x: Double,
            y: Double,
            z: Double,
            yaw: Float,
            partialTicks: Float,
            render: Render<Entity>
        ) : All(entity, x, y, z, yaw, partialTicks, render), EventPosting by Companion {
            companion object : EventBus()
        }

        class Post(
            entity: Entity,
            x: Double,
            y: Double,
            z: Double,
            yaw: Float,
            partialTicks: Float,
            render: Render<Entity>
        ) : All(entity, x, y, z, yaw, partialTicks, render), EventPosting by Companion {
            companion object : EventBus()
        }
    }

    sealed class Model private constructor(
        entity: Entity,
        private val block: () -> Unit
    ) : RenderEntityEvent(entity) {
        override fun render() {
            block.invoke()
        }

        class Pre(entity: Entity, block: () -> Unit) : Model(entity, block), EventPosting by Companion {
            companion object : EventBus() {
                @JvmStatic
                fun of(
                    entity: Entity,
                    x: Double,
                    y: Double,
                    z: Double,
                    yaw: Float,
                    partialTicks: Float,
                    render: Render<Entity>,
                ): Pre {
                    return Pre(entity) {
                        render.doRender(entity, x, y, z, yaw, partialTicks)
                    }
                }

                @JvmStatic
                fun of(
                    entity: Entity,
                    limbSwing: Float,
                    limbSwingAmount: Float,
                    ageInTicks: Float,
                    netHeadYaw: Float,
                    headPitch: Float,
                    scaleFactor: Float,
                    model: ModelBase,
                ): Pre {
                    return Pre(entity) {
                        model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor)
                    }
                }
            }
        }

        class Post(entity: Entity, block: () -> Unit) : Model(entity, block), EventPosting by Companion {
            companion object : EventBus() {
                @JvmStatic
                fun of(
                    entity: Entity,
                    x: Double,
                    y: Double,
                    z: Double,
                    yaw: Float,
                    partialTicks: Float,
                    render: Render<Entity>,
                ): Post {
                    return Post(entity) {
                        render.doRender(entity, x, y, z, yaw, partialTicks)
                    }
                }

                @JvmStatic
                fun of(
                    entity: Entity,
                    limbSwing: Float,
                    limbSwingAmount: Float,
                    ageInTicks: Float,
                    netHeadYaw: Float,
                    headPitch: Float,
                    scaleFactor: Float,
                    model: ModelBase,
                ): Post {
                    return Post(entity) {
                        model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor)
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        var renderingEntities = false
    }
}