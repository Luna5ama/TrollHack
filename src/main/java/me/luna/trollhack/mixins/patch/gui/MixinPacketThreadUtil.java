package me.luna.trollhack.mixins.patch.gui;

import me.luna.trollhack.util.threads.MainThreadExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.util.IThreadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketThreadUtil.class)
public class MixinPacketThreadUtil {

    /**
     * @author Luna
     * @reason Fuck
     */
    @Overwrite
    public static <T extends INetHandler> void checkThreadAndEnqueue(final Packet<T> packetIn, final T processor, IThreadListener scheduler) throws ThreadQuickExitException {
        if (!scheduler.isCallingFromMinecraftThread()) {
            if (scheduler == Minecraft.getMinecraft()) {
                MainThreadExecutor.INSTANCE.addProcessingPacket(packetIn, processor);
            } else {
                scheduler.addScheduledTask(() -> packetIn.processPacket(processor));
            }
            throw ThreadQuickExitException.INSTANCE;
        }
    }
}
