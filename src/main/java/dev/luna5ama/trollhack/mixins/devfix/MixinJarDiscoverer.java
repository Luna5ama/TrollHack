package dev.luna5ama.trollhack.mixins.devfix;

import net.minecraftforge.fml.common.discovery.JarDiscoverer;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// No junk logs
@Mixin(value = JarDiscoverer.class, remap = false)
public class MixinJarDiscoverer {
    @Redirect(method = "findClassesASM", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private void Redirect$findClassesASM$INVOKELogger$error(
        Logger instance,
        String message,
        Object p0,
        Object p1,
        Object p2
    ) {

    }

    @Redirect(method = "discover", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private void Redirect$discover$INVOKELogger$warn(Logger instance, String message, Object p0, Object p1) {

    }
}
