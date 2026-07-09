package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface IMinecraftClientAccessor {
    @Mutable
    @Accessor("user")
    void setUser(User user);

    @Mutable
    @Accessor("user")
    User getUser();

    @Mutable
    @Accessor("rightClickDelay")
    void setRightClickDelay(int i);
}
