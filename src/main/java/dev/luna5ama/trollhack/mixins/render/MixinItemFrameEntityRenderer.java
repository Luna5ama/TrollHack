package dev.luna5ama.trollhack.mixins.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings;
import dev.luna5ama.trollhack.modules.impl.visual.NameTags;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemFrameRenderer.class)
public class MixinItemFrameEntityRenderer {
    @ModifyReturnValue(method = "shouldShowName*", at = @At(value = "RETURN"))
    public boolean hasLabel$Tweaker(boolean original) {
        return original && (!NameTags.INSTANCE.isEnabled());
    }
}
