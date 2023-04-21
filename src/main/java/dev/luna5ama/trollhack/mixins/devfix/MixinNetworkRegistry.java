package dev.luna5ama.trollhack.mixins.devfix;

import io.netty.channel.ChannelHandler;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.EnumMap;

// WTF Forge
@Mixin(value = NetworkRegistry.class, remap = false)
public class MixinNetworkRegistry {
    @Inject(method = "newChannel(Ljava/lang/String;[Lio/netty/channel/ChannelHandler;)Ljava/util/EnumMap;", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/network/FMLEmbeddedChannel;<init>(Ljava/lang/String;Lnet/minecraftforge/fml/relauncher/Side;[Lio/netty/channel/ChannelHandler;)V"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void Inject$newChannel1$INVOKE$FMLEmbeddedChannel$init(
        String name,
        ChannelHandler[] handlers,
        CallbackInfoReturnable<EnumMap<Side, FMLEmbeddedChannel>> cir,
        EnumMap<Side, FMLEmbeddedChannel> result,
        Side[] var4,
        int var5,
        int var6,
        Side side
    ) {
        if (side.ordinal() > 1) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "newChannel(Lnet/minecraftforge/fml/common/ModContainer;Ljava/lang/String;[Lio/netty/channel/ChannelHandler;)Ljava/util/EnumMap;", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/network/FMLEmbeddedChannel;<init>(Lnet/minecraftforge/fml/common/ModContainer;Ljava/lang/String;Lnet/minecraftforge/fml/relauncher/Side;[Lio/netty/channel/ChannelHandler;)V"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void Inject$newChannel2$INVOKE$FMLEmbeddedChannel$init(
        ModContainer container,
        String name,
        ChannelHandler[] handlers,
        CallbackInfoReturnable<EnumMap<Side, FMLEmbeddedChannel>> cir,
        EnumMap<Side, FMLEmbeddedChannel> result,
        Side[] var5,
        int var6,
        int var7,
        Side side
    ) {
        if (side.ordinal() > 1) {
            cir.setReturnValue(result);
        }
    }
}
