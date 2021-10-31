package cum.xiaro.trollhack.patch.player;

import cum.xiaro.trollhack.util.Wrapper;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerControllerMP.class)
public abstract class MixinPlayerControllerMP {
    @Shadow private int currentPlayerItem;

    /**
     * @author Xiaro
     * @reason Fuck
     */
    @Overwrite
    private void syncCurrentPlayItem() {
        EntityPlayerSP player = Wrapper.getPlayer();
        if (player != null) {
            int i = player.inventory.currentItem;

            if (i != this.currentPlayerItem) {
                this.currentPlayerItem = i;
                player.connection.sendPacket(new CPacketHeldItemChange(this.currentPlayerItem));
            }
        }
    }
}
