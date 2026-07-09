package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Display.TextDisplay.class)
public interface ITextDisplayEntityAccessor {
    @Accessor("clientDisplayCache")
    void languagereload_setTextLines(Display.TextDisplay.CachedInfo textLines);
}
