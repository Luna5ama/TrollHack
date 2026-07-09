package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SignText.class)
public interface ISignTextAccessor {
    @Accessor("renderMessages")
    void languagereload_setOrderedMessages(FormattedCharSequence[] orderedMessages);
}
