package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BookViewScreen.class)
public interface IBookScreenAccessor {
    @Accessor("cachedPage")
    void languagereload_setCachedPageIndex(int cachedPageIndex);
}
