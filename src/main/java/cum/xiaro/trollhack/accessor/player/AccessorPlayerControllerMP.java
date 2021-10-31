package cum.xiaro.trollhack.accessor.player;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerControllerMP.class)
public interface AccessorPlayerControllerMP {

    @Accessor("blockHitDelay")
    int getBlockHitDelay();

    @Accessor("blockHitDelay")
    void setBlockHitDelay(int value);

    @Accessor("isHittingBlock")
    void trollSetIsHittingBlock(boolean value);

    @Accessor("currentPlayerItem")
    int getCurrentPlayerItem();

    @Invoker("syncCurrentPlayItem")
    void trollInvokeSyncCurrentPlayItem(); // Mixin bug #430 https://github.com/SpongePowered/Mixin/issues/430

}
