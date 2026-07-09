package dev.luna5ama.trollhack.mixins.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.luna5ama.trollhack.event.impl.render.RenderEntityEvent;
import dev.luna5ama.trollhack.graphics.shader.MotionBlur;
import dev.luna5ama.trollhack.utils.render.EntityRenderStateTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = LevelRenderer.class, priority = Integer.MAX_VALUE)
public abstract class MixinWorldRenderer {
    @Shadow private @Nullable ClientLevel level;

    @Shadow private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow protected abstract boolean shouldShowEntityOutlines();

    @Inject(method = "extractVisibleEntities", at = @At("HEAD"))
    private void onExtractVisibleEntitiesHead(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState levelRenderState, CallbackInfo ci) {
        RenderEntityEvent.setRenderingEntities(true);
    }

    @Inject(method = "extractVisibleEntities", at = @At("RETURN"))
    private void onExtractVisibleEntitiesReturn(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState levelRenderState, CallbackInfo ci) {
        RenderEntityEvent.setRenderingEntities(false);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "extractEntity", at = @At("HEAD"), cancellable = true)
    private void onExtractEntityHead(Entity entity, float tickDelta, CallbackInfoReturnable<EntityRenderState> cir) {
        EntityRenderer renderer = entityRenderDispatcher.getRenderer(entity);
        RenderEntityEvent event = new RenderEntityEvent.All.Pre(
                entity, null, null, null,
                (EntityRenderer<Entity, EntityRenderState>) renderer,
                0
        );
        event.post();

        if (event.getCancelled()) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "extractEntity", at = @At("RETURN"))
    private void onExtractEntityReturn(Entity entity, float tickDelta, CallbackInfoReturnable<EntityRenderState> cir) {
        EntityRenderState state = cir.getReturnValue();
        if (state != null) {
            EntityRenderStateTracker.bind(state, entity);
        }
    }

    @Redirect(method = "extractVisibleEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean onAddEntityRenderState(List<EntityRenderState> states, Object state) {
        if (state == null) return false;
        return states.add((EntityRenderState) state);
    }

    @Redirect(method = "extractVisibleEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;appearsGlowing()Z"))
    private boolean onAppearsGlowing(EntityRenderState state) {
        return state != null && state.appearsGlowing();
    }

    @Inject(
            method = "method_62214",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V",
                    ordinal = 1
            )
    )
    private void onRenderTranslucentChunks(
            GpuBufferSlice gpuBufferSlice, LevelRenderState levelRenderState, ProfilerFiller profilerFiller,
            Matrix4f matrix4f, ResourceHandle resourceHandle, ResourceHandle resourceHandle2,
            boolean renderBlockOutline, ResourceHandle resourceHandle3, ResourceHandle resourceHandle4,
            CallbackInfo ci
    ) {
        MotionBlur.INSTANCE.copyDepthBuffer();
    }

//    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/PostEffectProcessor;render(F)V", ordinal = 0))
//    private void replaceShaderHook(PostEffectProcessor instance, float tickDelta) {
//        if (Shaders.INSTANCE.isEnabled()) Shaders.INSTANCE.applyShader(
//                Objects.requireNonNull(((WorldRenderer) (Object) this).getEntityOutlinesFramebuffer())
//        );
//    }
}
