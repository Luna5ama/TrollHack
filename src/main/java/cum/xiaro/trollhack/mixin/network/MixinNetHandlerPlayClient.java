package cum.xiaro.trollhack.mixin.network;

import cum.xiaro.trollhack.module.modules.player.NoRotate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {
    @Shadow @Final private NetworkManager netManager;
    @Shadow private Minecraft client;
    @Shadow private boolean doneLoadingTerrain;

    @Inject(method = "handlePlayerPosLook", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;setPositionAndRotation(DDDFF)V", shift = At.Shift.BEFORE), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
    public void writePacketData(SPacketPlayerPosLook packetIn, CallbackInfo ci, EntityPlayer entityplayer, double d0, double d1, double d2, float f, float f1) {
        if (NoRotate.INSTANCE.isEnabled()) {
            ci.cancel();

            entityplayer.setPosition(d0, d1, d2);
            this.netManager.sendPacket(new CPacketConfirmTeleport(packetIn.getTeleportId()));
            this.netManager.sendPacket(new CPacketPlayer.PositionRotation(entityplayer.posX, entityplayer.getEntityBoundingBox().minY, entityplayer.posZ, f, f1, false));

            if (!this.doneLoadingTerrain) {
                this.client.player.prevPosX = this.client.player.posX;
                this.client.player.prevPosY = this.client.player.posY;
                this.client.player.prevPosZ = this.client.player.posZ;
                this.doneLoadingTerrain = true;
                this.client.displayGuiScreen(null);
            }
        }
    }
}
