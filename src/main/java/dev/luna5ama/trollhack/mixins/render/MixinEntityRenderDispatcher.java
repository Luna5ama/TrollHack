package dev.luna5ama.trollhack.mixins.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luna5ama.trollhack.event.impl.render.RenderEntityEvent;
import dev.luna5ama.trollhack.utils.render.EntityRenderStateTracker;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRenderDispatcher.class, priority = Integer.MAX_VALUE)
public abstract class MixinEntityRenderDispatcher {

    @Shadow public abstract <S extends EntityRenderState> EntityRenderer<?, ? super S> getRenderer(S state);

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "submit", at = @At("RETURN"))
    public <S extends EntityRenderState> void submit$RETURN(
            S state, CameraRenderState cameraRenderState, double xOffset, double yOffset, double zOffset,
            PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci
    ) {
        Entity entity = EntityRenderStateTracker.remove(state);
        if (entity == null) return;

        EntityRenderer renderer = getRenderer(state);

        RenderEntityEvent eventPost = new RenderEntityEvent.All.Post(
                entity, state,
                poseStack, null,
                (EntityRenderer<Entity, EntityRenderState>) renderer,
                state.lightCoords
        );
        eventPost.post();
    }
}
