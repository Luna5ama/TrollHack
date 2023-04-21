package dev.luna5ama.trollhack.mixins.devfix;

import net.minecraftforge.fml.common.discovery.asm.ASMModParser;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// WTF Forge
@Mixin(value = ASMModParser.class, remap = false)
public class MixinASMModParser {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V"))
    private void Redirect$init$INVOKELogger$error(Logger instance, String s, Throwable throwable) {

    }
}
