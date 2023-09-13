package dev.luna5ama.trollhack.mixins.core;

import dev.luna5ama.trollhack.event.events.GuiEvent;
import dev.luna5ama.trollhack.event.events.ProcessKeyBindEvent;
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent;
import dev.luna5ama.trollhack.event.events.TickEvent;
import dev.luna5ama.trollhack.mixins.accessor.player.AccessorEntityPlayerSP;
import dev.luna5ama.trollhack.mixins.accessor.player.AccessorPlayerControllerMP;
import dev.luna5ama.trollhack.module.modules.client.MainMenu;
import dev.luna5ama.trollhack.module.modules.misc.UnfocusedFps;
import dev.luna5ama.trollhack.module.modules.player.BlockInteraction;
import dev.luna5ama.trollhack.module.modules.player.FastUse;
import dev.luna5ama.trollhack.module.modules.player.PacketMine;
import dev.luna5ama.trollhack.util.Wrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.math.RayTraceResult;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.PixelFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow
    public WorldClient world;
    @Shadow
    public EntityPlayerSP player;
    @Shadow
    public GuiScreen currentScreen;
    @Shadow
    public GameSettings gameSettings;
    @Shadow
    public PlayerControllerMP playerController;
    @Shadow
    public RayTraceResult objectMouseOver;

    private RayTraceResult.Type type = null;
    private boolean handActive = false;
    private boolean isHittingBlock = false;

    @Shadow
    protected abstract void clickMouse();

    @Shadow
    protected abstract void init() throws LWJGLException, IOException;

    @Redirect(method = "createDisplay", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;create(Lorg/lwjgl/opengl/PixelFormat;)V", remap = false), require = 0)
    public void Redirect$createDisplay$INVOKE$Display$create(PixelFormat pixel_format) throws LWJGLException {
        Display.create(pixel_format, new ContextAttribs(4, 5).withProfileCompatibility(true));
    }

    @ModifyVariable(method = "displayGuiScreen", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    public GuiScreen displayGuiScreen$ModifyVariable$HEAD(GuiScreen value) {
        GuiScreen current = this.currentScreen;
        if (current != null) {
            GuiEvent.Closed closed = new GuiEvent.Closed(current);
            closed.post();
        }

        GuiEvent.Displayed displayed = new GuiEvent.Displayed(value);
        displayed.post();
        return displayed.getScreen();
    }

    @ModifyVariable(method = "displayGuiScreen", at = @At(value = "STORE", ordinal = 0), ordinal = 0, argsOnly = true)
    public GuiScreen displayGuiScreen$ModifyVariable$STORE(GuiScreen value) {
        if (MainMenu.INSTANCE.isEnabled()) {
            return new MainMenu.TrollGuiMainMenu();
        } else {
            return value;
        }
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Timer;updateTimer()V", shift = At.Shift.BEFORE))
    public void runGameLoop$Inject$INVOKE$updateTimer(CallbackInfo ci) {
        Wrapper.getMinecraft().profiler.startSection("trollRunGameLoop");
        RunGameLoopEvent.Start.INSTANCE.post();
        Wrapper.getMinecraft().profiler.endSection();
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endSection()V", ordinal = 0, shift = At.Shift.AFTER))
    public void runGameLoop$INVOKE$endSection(CallbackInfo ci) {
        Wrapper.getMinecraft().profiler.startSection("trollRunGameLoop");
        RunGameLoopEvent.Tick.INSTANCE.post();
        Wrapper.getMinecraft().profiler.endSection();
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", ordinal = 0, shift = At.Shift.BEFORE))
    public void runGameLoop$Inject$INVOKE$endStartSection(CallbackInfo ci) {
        Wrapper.getMinecraft().profiler.endStartSection("trollRunGameLoop");
        RunGameLoopEvent.Render.INSTANCE.post();
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isFramerateLimitBelowMax()Z", shift = At.Shift.BEFORE))
    public void runGameLoop$Inject$INVOKE$isFramerateLimitBelowMax(CallbackInfo ci) {
        Wrapper.getMinecraft().profiler.startSection("trollRunGameLoop");
        RunGameLoopEvent.End.INSTANCE.post();
        Wrapper.getMinecraft().profiler.endSection();
    }

    @Redirect(method = "runGameLoop", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V", remap = false))
    public void runGameLoop$Redirect$INVOKE$yield() {

    }

    @Inject(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;", ordinal = 0))
    public void runTick$Inject$FIELD$currentScreen$0(CallbackInfo ci) {
        Wrapper.getMinecraft().profiler.endStartSection("gui");
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    public void runTick$Inject$HEAD(CallbackInfo ci) {
        TickEvent.Pre.INSTANCE.post();
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    public void runTick$Inject$RETURN(CallbackInfo ci) {
        TickEvent.Post.INSTANCE.post();
    }

    @Inject(method = "processKeyBinds", at = @At("HEAD"))
    public void processKeyBinds$Inject$HEAD(CallbackInfo ci) {
        Wrapper.getMinecraft().profiler.startSection("trollProcessKeyBind");
        ProcessKeyBindEvent.Pre.INSTANCE.post();
        Wrapper.getMinecraft().profiler.endSection();
    }

    @Inject(method = "processKeyBinds", at = @At("RETURN"))
    public void processKeyBinds$Inject$RETURN(CallbackInfo ci) {
        Wrapper.getMinecraft().profiler.startSection("trollProcessKeyBind");
        ProcessKeyBindEvent.Post.INSTANCE.post();
        Wrapper.getMinecraft().profiler.endSection();
    }

    // Allows left click attack while eating lol
    @Inject(method = "processKeyBinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;isKeyDown()Z", shift = At.Shift.BEFORE, ordinal = 2))
    public void processKeyBinds$Inject$INVOKE$isKeyDown(CallbackInfo ci) {
        if (BlockInteraction.isMultiTaskEnabled()) {
            while (this.gameSettings.keyBindAttack.isPressed()) {
                this.clickMouse();
            }
        }
    }

    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    public void clickMouse$Inject$HEAD(CallbackInfo ci) {
        if (BlockInteraction.isNoAirHitEnabled()) {
            RayTraceResult result = this.objectMouseOver;
            if (result != null && result.typeOfHit == RayTraceResult.Type.MISS) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "clickMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;swingArm(Lnet/minecraft/util/EnumHand;)V"), cancellable = true)
    public void clickMouse$Inject$INVOKE$swingArm(CallbackInfo ci) {
        if (PacketMine.INSTANCE.isEnabled() && PacketMine.INSTANCE.getNoSwing()) {
            RayTraceResult rayTraceResult = this.objectMouseOver;
            if (rayTraceResult != null && rayTraceResult.typeOfHit == RayTraceResult.Type.BLOCK) {
                PacketMine.IMiningInfo miningInfo = PacketMine.INSTANCE.getMiningInfo();
                if (miningInfo != null && rayTraceResult.getBlockPos() == miningInfo.getPos()) {
                    ci.cancel();
                }
            }
        }
    }

    // Hacky but safer than using @Redirect
    @Inject(method = "rightClickMouse", at = @At("HEAD"))
    public void rightClickMouse$Inject$HEAD(CallbackInfo ci) {
        if (BlockInteraction.isMultiTaskEnabled()) {
            isHittingBlock = playerController.getIsHittingBlock();
            ((AccessorPlayerControllerMP) playerController).trollSetIsHittingBlock(false);
        }

        if (BlockInteraction.isPrioritizingEating()) {
            RayTraceResult rayTraceResult = this.objectMouseOver;
            if (rayTraceResult != null) {
                type = rayTraceResult.typeOfHit;
                rayTraceResult.typeOfHit = RayTraceResult.Type.MISS;
            }
        }
    }

    @Inject(method = "rightClickMouse", at = @At("RETURN"))
    public void rightClickMouse$Inject$RETURN(CallbackInfo ci) {
        if (BlockInteraction.isMultiTaskEnabled() && !playerController.getIsHittingBlock()) {
            ((AccessorPlayerControllerMP) playerController).trollSetIsHittingBlock(isHittingBlock);
        }

        RayTraceResult.Type cache = type;
        type = null;

        if (cache != null) {
            RayTraceResult rayTraceResult = this.objectMouseOver;
            if (rayTraceResult != null) {
                rayTraceResult.typeOfHit = cache;
            }
        }
    }

    @Inject(method = "sendClickBlockToController", at = @At("HEAD"))
    public void sendClickBlockToController$Inject$HEAD(boolean leftClick, CallbackInfo ci) {
        if (BlockInteraction.isMultiTaskEnabled()) {
            handActive = player.isHandActive();
            ((AccessorEntityPlayerSP) player).trollSetHandActive(false);
        }
    }

    @Inject(method = "sendClickBlockToController", at = @At("RETURN"))
    public void sendClickBlockToController$Inject$RETURN(boolean leftClick, CallbackInfo ci) {
        if (BlockInteraction.isMultiTaskEnabled() && !player.isHandActive()) {
            ((AccessorEntityPlayerSP) player).trollSetHandActive(handActive);
        }
    }

    @Inject(method = "getLimitFramerate", at = @At("HEAD"), cancellable = true)
    public void getLimitFramerate$Inject$HEAD(CallbackInfoReturnable<Integer> cir) {
        MainMenu.handleGetLimitFramerate(cir);
        UnfocusedFps.handleGetLimitFramerate(cir);
    }

    @Inject(method = "rightClickMouse", at = @At(value = "RETURN", ordinal = 0))
    public void rightClickMouseBlock$Inject$RETURN$0(CallbackInfo ci) {
        FastUse.updateRightClickDelay();
    }

    @Inject(method = "rightClickMouse", at = @At(value = "RETURN", ordinal = 1))
    public void rightClickMouseBlock$Inject$RETURN$1(CallbackInfo ci) {
        FastUse.updateRightClickDelay();
    }

    @Inject(method = "rightClickMouse", at = @At(value = "RETURN", ordinal = 2))
    public void rightClickMouseBlock$Inject$RETURN$2(CallbackInfo ci) {
        FastUse.updateRightClickDelay();
    }

    @Inject(method = "rightClickMouse", at = @At(value = "RETURN", ordinal = 3))
    public void rightClickMouseBlock$Inject$RETURN$3(CallbackInfo ci) {
        FastUse.updateRightClickDelay();
    }
}

