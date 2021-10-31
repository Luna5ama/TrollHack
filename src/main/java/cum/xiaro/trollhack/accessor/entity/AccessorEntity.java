package cum.xiaro.trollhack.accessor.entity;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface AccessorEntity {
    @Accessor("isInWeb")
    boolean troll$getIsInWeb();

    @Invoker("getFlag")
    boolean troll$getFlag(int flag);

    @Invoker("setFlag")
    void troll$setFlag(int flag, boolean value);
}
