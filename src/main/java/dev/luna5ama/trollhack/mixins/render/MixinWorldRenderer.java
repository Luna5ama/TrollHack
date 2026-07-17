package dev.luna5ama.trollhack.mixins.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.luna5ama.trollhack.event.impl.render.Render3DEvent;
import dev.luna5ama.trollhack.event.impl.render.RenderEntityEvent;
import dev.luna5ama.trollhack.graphics.blaze3d.Render3DScheduler;
import dev.luna5ama.trollhack.graphics.blaze3d.WorldProjection;
import dev.luna5ama.trollhack.modules.impl.visual.CrystalChams;
import dev.luna5ama.trollhack.modules.impl.visual.PopChams;
import dev.luna5ama.trollhack.modules.impl.visual.Shaders;
import dev.luna5ama.trollhack.utils.render.EntityRenderStateTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
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
    @Shadow private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow @Final private LevelTargetBundle targets;

    @Inject(
            method = "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZLnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;addLateDebugPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4fc;)V",
                    shift = At.Shift.BEFORE
            ),
            require = 1
    )
    private void addTrollHackRenderPass(
            GraphicsResourceAllocator allocator,
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            CameraRenderState cameraState,
            Matrix4fc viewMatrix,
            GpuBufferSlice fogBuffer,
            Vector4f fogColor,
            boolean renderSky,
            ChunkSectionsToRender chunkSectionsToRender,
            CallbackInfo ci,
            @Local FrameGraphBuilder frameGraphBuilder
    ) {
        FramePass framePass = frameGraphBuilder.addPass("trollhack_esp");
        this.targets.main = framePass.readsAndWrites(this.targets.main);
        ResourceHandle<RenderTarget> targetHandle = this.targets.main;
        Matrix4f capturedViewMatrix = new Matrix4f(viewMatrix);
        Matrix4f capturedProjectionMatrix = new Matrix4f(cameraState.projectionMatrix);
        Vec3 cameraPosition = cameraState.pos;
        float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);

        framePass.executes(() -> {
            RenderTarget target = targetHandle.get();
            GpuTextureView previousColorTarget = RenderSystem.outputColorTextureOverride;
            GpuTextureView previousDepthTarget = RenderSystem.outputDepthTextureOverride;
            RenderSystem.outputColorTextureOverride = target.getColorTextureView();
            RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
            try {
                WorldProjection.capture(capturedProjectionMatrix, capturedViewMatrix, cameraPosition);

                PoseStack poseStack = new PoseStack();
                poseStack.mulPose(capturedViewMatrix);
                new Render3DEvent(poseStack, tickDelta).post();
                Render3DScheduler.INSTANCE.flush(capturedViewMatrix, cameraPosition);
            } finally {
                Render3DScheduler.INSTANCE.clear();
                RenderSystem.outputColorTextureOverride = previousColorTarget;
                RenderSystem.outputDepthTextureOverride = previousDepthTarget;
            }
        });
    }

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
            int outlineColor = CrystalChams.outlineArgb(entity);
            if (outlineColor == 0) outlineColor = PopChams.outlineArgb(entity);
            if (outlineColor == 0) outlineColor = Shaders.outlineArgb(entity);
            if (outlineColor != 0) state.outlineColor = outlineColor;
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

}
