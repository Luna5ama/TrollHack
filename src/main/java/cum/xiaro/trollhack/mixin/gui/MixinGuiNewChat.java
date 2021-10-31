package cum.xiaro.trollhack.mixin.gui;

import cum.xiaro.trollhack.module.modules.chat.ExtraChatHistory;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {
    @Shadow @Final private List<ChatLine> chatLines;
    @Shadow @Final private List<ChatLine> drawnChatLines;

    @Inject(method = "setChatLine", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0, remap = false), cancellable = true)
    public void setChatLineInvokeSize(ITextComponent chatComponent, int chatLineId, int updateCounter, boolean displayOnly, CallbackInfo ci) {
        ExtraChatHistory.handleSetChatLine(drawnChatLines, chatLines, chatComponent, chatLineId, updateCounter, displayOnly, ci);
    }
}