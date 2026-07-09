package dev.luna5ama.trollhack.mixins.network;

import dev.luna5ama.trollhack.command.CommandManager;
import dev.luna5ama.trollhack.event.impl.client.SendMessageEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    @Unique
    private boolean ignoreChatMessage;

    @Shadow
    public void sendChat(String content) {
    }

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (message.charAt(0) == CommandManager.INSTANCE.getPrefix().charAt(0)) {
            CommandManager.INSTANCE.runCommand(message.substring(1));
            ci.cancel();
            return;
        }

        if (ignoreChatMessage) {
            return;
        }

        SendMessageEvent event = new SendMessageEvent(message);
        event.post();

        if (!event.getCancelled()) {
            ignoreChatMessage = true;
            sendChat(event.getString());
            ignoreChatMessage = false;
        }
        ci.cancel();
    }
}

