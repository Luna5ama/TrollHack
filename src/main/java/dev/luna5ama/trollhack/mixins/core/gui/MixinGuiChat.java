package dev.luna5ama.trollhack.mixins.core.gui;

import dev.luna5ama.trollhack.command.CommandManager;
import dev.luna5ama.trollhack.gui.mc.TrollGuiChat;
import dev.luna5ama.trollhack.util.Wrapper;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends GuiScreen {

    @Shadow
    protected GuiTextField inputField;
    @Shadow
    private String historyBuffer;
    @Shadow
    private int sentHistoryCursor;

    @Inject(method = "keyTyped(CI)V", at = @At("RETURN"))
    public void returnKeyTyped(char typedChar, int keyCode, CallbackInfo info) {
        GuiScreen currentScreen = Wrapper.getMinecraft().currentScreen;
        if (currentScreen instanceof GuiChat && !(currentScreen instanceof TrollGuiChat)
            && inputField.getText().startsWith(CommandManager.INSTANCE.getPrefix())) {
            Wrapper.getMinecraft().displayGuiScreen(new TrollGuiChat(
                inputField.getText(),
                historyBuffer,
                sentHistoryCursor
            ));
        }
    }

}
