package cum.xiaro.trollhack.mixin.network;

import cum.xiaro.trollhack.event.events.PacketEvent;
import cum.xiaro.trollhack.module.modules.player.NoPacketKick;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {
    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void sendPacketPre(Packet<?> packet, CallbackInfo callbackInfo) {
        if (packet != null) {
            PacketEvent.Send event = new PacketEvent.Send(packet);
            event.post();

            if (event.getCancelled()) {
                callbackInfo.cancel();
            }
        }
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("RETURN"), cancellable = true)
    private void sendPacketPost(Packet<?> packet, CallbackInfo callbackInfo) {
        if (packet != null) {
            PacketEvent.PostSend event = new PacketEvent.PostSend(packet);
            event.post();
        }
    }

    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    private void channelReadPre(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callbackInfo) {
        if (packet != null) {
            PacketEvent.Receive event = new PacketEvent.Receive(packet);
            event.post();

            if (event.getCancelled()) {
                callbackInfo.cancel();
            }
        }
    }

    @Inject(method = "channelRead0", at = @At("RETURN"))
    private void channelReadPost(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callbackInfo) {
        if (packet != null) {
            PacketEvent.PostReceive event = new PacketEvent.PostReceive(packet);
            event.post();
        }
    }

    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    private void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable, CallbackInfo ci) {
        if (NoPacketKick.INSTANCE.isEnabled()) {
            NoPacketKick.sendWarning(throwable);
            ci.cancel();
        }
    }
}
