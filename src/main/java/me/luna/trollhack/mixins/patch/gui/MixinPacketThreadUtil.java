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

@Mixin(PacketThreadUtil.class)
public class MixinPacketThreadUtil {

    /**
     * @author Luna
     * @reason Fuck
     */
    @Overwrite
    public static <T extends INetHandler> void checkThreadAndEnqueue(final Packet<T> packetIn, final T processor, IThreadListener scheduler) throws ThreadQuickExitException {
        if (scheduler == Minecraft.getMinecraft() && !scheduler.isCallingFromMinecraftThread()) {
            MainThreadExecutor.INSTANCE.addProcessingPacket(packetIn, processor);
            throw ThreadQuickExitException.INSTANCE;
        }
    }
}
