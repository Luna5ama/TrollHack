package dev.luna5ama.trollhack.mixins.patch.chat;

import com.google.common.collect.Iterators;
import dev.luna5ama.trollhack.mixins.PatchedITextComponent;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;

@Mixin(ITextComponent.class)
public interface MixinITextComponent extends PatchedITextComponent, Iterable<ITextComponent> {
    @Shadow
    List<ITextComponent> getSiblings();

    @NotNull
    @Override
    default Iterator<ITextComponent> inplaceIterator() {
        return Iterators.concat(Iterators.forArray((ITextComponent) this), this.getSiblings().iterator());
    }
}
