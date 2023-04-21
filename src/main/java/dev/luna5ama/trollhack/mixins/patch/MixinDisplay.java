package dev.luna5ama.trollhack.mixins.patch;

import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = Display.class, remap = false)
public class MixinDisplay {
    /**
     * @author Lol
     * @reason Trolled
     */
    @Overwrite
    public static void setTitle(String newTitle) {

    }
}
