package dev.luna5ama.trollhack.mixins.network;

import io.netty.channel.ChannelFuture;
import dev.luna5ama.trollhack.event.impl.PacketEvent;
import dev.luna5ama.trollhack.event.impl.world.ConnectionEvent;
import net.minecraft.network.ClientboundPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.network.EventLoopGroupHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetSocketAddress;
import java.util.Iterator;

@Mixin(Connection.class)
public class MixinClientConnection {
    @Inject(method = "genericsFtw", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            for (Iterator<Packet<? super ClientGamePacketListener>> it = bundle.subPackets().iterator(); it.hasNext(); ) {
                PacketEvent.Receive event = new PacketEvent.Receive(it.next());
                event.post();
                if (event.getCancelled()) it.remove();
            }
        } else {
            PacketEvent.Receive event = new PacketEvent.Receive(packet);
            event.post();
            if (event.getCancelled()) ci.cancel();
        }
    }

    @Inject(method = "genericsFtw", at = @At("RETURN"))
    private static <T extends PacketListener> void onHandlePacketReturn(Packet<T> packet, PacketListener listener, CallbackInfo info) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            for (Packet<? super ClientGamePacketListener> value : bundle.subPackets()) {
                PacketEvent.PostReceive event = new PacketEvent.PostReceive(value);
                event.post();
            }
        } else {
            PacketEvent.PostReceive event = new PacketEvent.PostReceive(packet);
            event.post();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacketHead(Packet<?> packet, CallbackInfo info) {
        PacketEvent.Send event = new PacketEvent.Send(packet);
        event.post();
        if (event.getCancelled()) info.cancel();
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("RETURN"))
    private void onSendPacketTail(Packet<?> packet, CallbackInfo info) {
        PacketEvent.PostSend event = new PacketEvent.PostSend(packet);
        event.post();
    }

    @Inject(method = "disconnect(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void disconnect(Component disconnectReason, CallbackInfo ci) {
        ConnectionEvent.Disconnect event = new ConnectionEvent.Disconnect(disconnectReason);
        event.post();
    }

    @Inject(method = "connect", at = @At("HEAD"))
    private static void onConnect(InetSocketAddress address, EventLoopGroupHolder eventLoopGroupHolder, Connection connection, CallbackInfoReturnable<ChannelFuture> cir) {
        ConnectionEvent.Connect.INSTANCE.post();
    }
}
