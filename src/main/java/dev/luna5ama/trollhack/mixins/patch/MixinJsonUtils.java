package dev.luna5ama.trollhack.mixins.patch;

import com.google.gson.Gson;
import net.minecraft.util.JsonUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Pseudo
@Mixin(JsonUtils.class)
public abstract class MixinJsonUtils {
    @Shadow
    @Nullable
    public static <T> T gsonDeserialize(Gson gsonIn, String json, Class<T> adapter, boolean lenient) {
        return null;
    }

    /**
     * @author Luna
     * @reason Development launch fix
     */
    @Overwrite
    @Nullable
    public static <T> T gsonDeserialize(Gson gsonIn, String json, Class<T> adapter) {
        return gsonDeserialize(gsonIn, json, adapter, true);
    }
}
