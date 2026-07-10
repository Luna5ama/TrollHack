package dev.luna5ama.trollhack.mixins.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.luna5ama.trollhack.RenderSystem;
import dev.luna5ama.trollhack.event.impl.render.ResolutionUpdateEvent;
import dev.luna5ama.trollhack.graphics.skia.SkiaMinecraftBridge;
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings;
import dev.luna5ama.trollhack.modules.impl.player.NoEntityTrace;
import dev.luna5ama.trollhack.modules.impl.visual.AspectRatio;
import dev.luna5ama.trollhack.modules.impl.visual.MotionBlur;
import dev.luna5ama.trollhack.modules.impl.visual.NoRender;
import dev.luna5ama.trollhack.graphics.buffer.Render3DUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL11C.glGetInteger;
import static org.lwjgl.opengl.GL13C.GL_ACTIVE_TEXTURE;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Shadow private float renderDistance;

    @Inject(method = "resize", at = @At("HEAD"))
    public void onResized$HEAD(int width, int height, CallbackInfo ci) {
        new ResolutionUpdateEvent(width, height).post();
        GlStateManager._glUseProgram(0);
    }

    @Inject(
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 0),
            method = "renderLevel",
            require = 0)
    private void onRenderWorld(DeltaTracker deltaTracker,
                               CallbackInfo ci,
                               @Local(ordinal = 2) Matrix4f matrix4f2,
                               @Local(ordinal = 0) float tickDelta
    ) {
        if (MotionBlur.INSTANCE.getEnable()) {
            Matrix4f projectionMatrix = ((GameRenderer) (Object) this).getProjectionMatrix(tickDelta);
            dev.luna5ama.trollhack.graphics.shader.MotionBlur.INSTANCE.updateMatrix(
                    com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix(),
                    new Matrix4f(projectionMatrix).mul(matrix4f2)
            );
            dev.luna5ama.trollhack.graphics.shader.MotionBlur.INSTANCE.draw();
        }
        PoseStack matrixStack = new PoseStack();
        matrixStack.mulPose(matrix4f2);
        Render3DUtils.INSTANCE.getLastProjectionMatrix().set(((GameRenderer) (Object) this).getProjectionMatrix(tickDelta));
        Render3DUtils.INSTANCE.getLastModelViewMatrix().set(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix());
        Render3DUtils.INSTANCE.getLastWorldSpaceMatrix().set(matrixStack.last().pose());
        var prevTex = glGetInteger(GL_ACTIVE_TEXTURE);
        RenderSystem.INSTANCE.render3D(matrixStack, tickDelta);
        GlStateManager._glUseProgram(0);
        glActiveTexture(prevTex);
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(770, 771, 1, 0);
        GlStateManager._enableCull();
        GlStateManager._enableDepthTest();
//        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onRender2D(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci, @Local GuiGraphics guiGraphics) {
        float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
        SkiaMinecraftBridge.INSTANCE.render2D(tickDelta);
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void tiltViewWhenHurtHook(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getHurtCam()) {
            ci.cancel();
        }
    }

    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack stack, CallbackInfo ci) {
        if (stack.getItem() == Items.TOTEM_OF_UNDYING && NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getTotem()) {
            ci.cancel();
        }
    }

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F"))
    private float applyCameraTransformationsMathHelperLerpProxy(float delta, float first, float second) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getNausea()) return 0;
        return Mth.lerp(delta, first, second);
    }
    @Unique
    private static final Minecraft mc = Minecraft.getInstance();

    @Inject(method = "getProjectionMatrix", at = @At("TAIL"), cancellable = true)
    public void getBasicProjectionMatrixHook(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
        if (AspectRatio.INSTANCE.isEnabled()) {
            PoseStack matrixStack = new PoseStack();
            matrixStack.last().pose().identity();
            matrixStack.last().pose().mul(new Matrix4f().setPerspective((float) (fovDegrees * (Math.PI / 180d)), AspectRatio.getRatio(), 0.05f, renderDistance * 4.0f));
            cir.setReturnValue(matrixStack.last().pose());
        }
    }

    @Inject(method = "pick(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;raycastHitResult(FLnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/HitResult;"), cancellable = true)
    private void onUpdateTargetedEntity(float tickDelta, CallbackInfo info) {
        if (mc.crosshairPickEntity != null && mc.player != null
                && NoEntityTrace.INSTANCE.isEnabled()
                && (mc.player.getMainHandItem().is(ItemTags.PICKAXES) || !NoEntityTrace.INSTANCE.getPonly())
                && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            if (mc.player.getMainHandItem().is(ItemTags.SWORDS) && NoEntityTrace.INSTANCE.getNoSword()) return;
            Profiler.get().pop();
            info.cancel();
        }
    }
}
