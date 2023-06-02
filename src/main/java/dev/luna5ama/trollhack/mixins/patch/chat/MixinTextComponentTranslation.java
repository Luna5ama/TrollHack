package dev.luna5ama.trollhack.mixins.patch.chat;

import com.google.common.collect.Iterators;
import dev.luna5ama.trollhack.mixins.PatchedITextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentBase;
import net.minecraft.util.text.TextComponentTranslation;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;

@Mixin(TextComponentTranslation.class)
public abstract class MixinTextComponentTranslation extends TextComponentBase implements PatchedITextComponent {
    @Shadow
    abstract void ensureInitialized();

    @Shadow
    List<ITextComponent> children;

    @NotNull
    @Override
    public Iterator<ITextComponent> inplaceIterator() {
        this.ensureInitialized();
        return Iterators.concat(this.children.iterator(), this.siblings.iterator());
    }
}
