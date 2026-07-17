package dev.luna5ama.trollhack.mixins;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import dev.luna5ama.trollhack.RenderSystem;
import dev.luna5ama.trollhack.TrollHackMod;
import dev.luna5ama.trollhack.event.impl.LoopEvent;
import dev.luna5ama.trollhack.event.impl.TickEvent;
import dev.luna5ama.trollhack.event.impl.world.WorldEvent;
import dev.luna5ama.trollhack.graphics.skia.SkiaMinecraftBridge;
import dev.luna5ama.trollhack.graphics.blaze3d.Blaze3DPostProcessor;
import dev.luna5ama.trollhack.manager.managers.ConfigManager;
import dev.luna5ama.trollhack.manager.managers.ProcessExitHook;
import dev.luna5ama.trollhack.modules.impl.player.MultiTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MixinMinecraft {
    @Shadow private LocalPlayer player;
    @Shadow private int missTime;
    @Shadow private HitResult hitResult;
    @Shadow private ClientLevel level;
    @Shadow private MultiPlayerGameMode gameMode;
    @Shadow @Final private ParticleEngine particleEngine;
    @Shadow private MetricsRecorder metricsRecorder;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/System;currentTimeMillis()J"))
    private long onInitBackendSystem() {
        return System.currentTimeMillis();
    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/metrics/profiling/MetricsRecorder;startTick()V"))
    private void onLoopStart(CallbackInfo ci) {
        TrollHackMod.INSTANCE.getProfiler().clear();
        LoopEvent.Start.INSTANCE.post();
    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runTick(Z)V"))
    private void onRender(CallbackInfo ci) {
        LoopEvent.Render.INSTANCE.post();
    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runTick(Z)V", shift = At.Shift.AFTER))
    private void onRenderPost(CallbackInfo ci) {
        LoopEvent.RenderPost.INSTANCE.post();
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"))
    private void onTick(boolean tick, CallbackInfo ci) {
        LoopEvent.Tick.INSTANCE.post();
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/FramerateLimitTracker;getFramerateLimit()I"))
    private int updateFpsLimit(FramerateLimitTracker instance) {
        return RenderSystem.INSTANCE.getCurrentMaxFps(Minecraft.getInstance().options.framerateLimit().get());
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen()V"))
    private void onBlitFramebuffer(RenderTarget instance) {
        Blaze3DPostProcessor.INSTANCE.apply(instance);
        instance.blitToScreen();
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V"))
    private void onThreadYield() {
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onPreTick(CallbackInfo ci) {
        metricsRecorder.getProfiler().push(TrollHackMod.ID + "_pre_update");
        TickEvent.Pre.INSTANCE.post();
        metricsRecorder.getProfiler().pop();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onPostTick(CallbackInfo ci) {
        metricsRecorder.getProfiler().push(TrollHackMod.ID + "_pre_update");
        TickEvent.Post.INSTANCE.post();
        metricsRecorder.getProfiler().pop();
    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/metrics/profiling/MetricsRecorder;endTick()V"))
    private void onLoopEnd(CallbackInfo ci) {
        LoopEvent.End.INSTANCE.post();
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void onJoinWorld(ClientLevel level, CallbackInfo ci) {
        WorldEvent.Load.INSTANCE.post();
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void handleBlockBreaking(boolean breaking, CallbackInfo ci) {
        if (missTime <= 0 && player != null && player.isUsingItem() && MultiTask.INSTANCE.isEnabled()) {
            if (breaking && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                if (level != null && !level.getBlockState(blockHitResult.getBlockPos()).isAir()) {
                    if (gameMode != null && gameMode.continueDestroyBlock(blockHitResult.getBlockPos(), blockHitResult.getDirection())) {
                        player.swing(InteractionHand.MAIN_HAND);
                    }
                }
            } else if (gameMode != null) {
                gameMode.stopDestroyBlock();
            }
            ci.cancel();
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V"))
    private void onUnloadWorld(Screen nextScreen, boolean keepResourcePacks, boolean transferring, CallbackInfo ci) {
        WorldEvent.Unload.INSTANCE.post();
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/thread/ReentrantBlockableEventLoop;<init>(Ljava/lang/String;)V", shift = At.Shift.AFTER))
    private void onInitPre(GameConfig gameConfig, CallbackInfo ci) {
        TrollHackMod.INSTANCE.initializePre();
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void onInitPost(GameConfig gameConfig, CallbackInfo ci) {
        TrollHackMod.INSTANCE.initialize();
        TrollHackMod.INSTANCE.postInitialize();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        TrollHackMod.LOGGER.warn("Shutting down " + TrollHackMod.NAME);
        Blaze3DPostProcessor.INSTANCE.close();
        SkiaMinecraftBridge.INSTANCE.close();
        ProcessExitHook.INSTANCE.onExit();
        ConfigManager.INSTANCE.save();
    }
}
