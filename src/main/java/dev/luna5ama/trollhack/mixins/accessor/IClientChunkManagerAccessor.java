package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.client.multiplayer.ClientChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientChunkCache.class)
public interface IClientChunkManagerAccessor {
    @Accessor("storage")
    ClientChunkCache.Storage languagereload_getChunks();
}
