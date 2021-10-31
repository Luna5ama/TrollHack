package cum.xiaro.trollhack.mixin.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractClientPlayer.class, priority = Integer.MAX_VALUE)
public abstract class MixinAbstractClientPlayer extends EntityPlayer {
    public MixinAbstractClientPlayer(World worldIn, GameProfile gameProfileIn) {
        super(worldIn, gameProfileIn);
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "isSpectator", at = @At("HEAD"), cancellable = true)
    public void isSpectator$Inject$HEAD(CallbackInfoReturnable<Boolean> cir) {
        NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection();
        if (connection != null) {
            NetworkPlayerInfo networkplayerinfo = connection.getPlayerInfo(this.getGameProfile().getId());
            cir.setReturnValue(networkplayerinfo != null && networkplayerinfo.getGameType() == GameType.SPECTATOR);
        } else {
            cir.setReturnValue(false);
        }
    }
}