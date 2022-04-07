package me.luna.trollhack.mixins.core;

import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;

@Mixin(value = Display.class, remap = false)
public class MixinDisplay {
    @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
    private static void setTitle$Inject$HEAD(String newTitle, CallbackInfo ci) {
        ci.cancel();
    }
}
