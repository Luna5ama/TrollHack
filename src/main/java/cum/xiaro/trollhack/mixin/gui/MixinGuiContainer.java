package cum.xiaro.trollhack.mixin.gui;

import cum.xiaro.trollhack.module.modules.player.ChestStealer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public class MixinGuiContainer extends GuiScreen {
    private final ChestStealer.StealButton stealButton = new ChestStealer.StealButton();
    private final ChestStealer.StoreButton storeButton = new ChestStealer.StoreButton();
    @Shadow protected int guiLeft;
    @Shadow protected int guiTop;
    @Shadow protected int xSize;

    @Inject(method = "initGui", at = @At("RETURN"))
    public void initGui(CallbackInfo ci) {
        if (ChestStealer.INSTANCE.isValidGui()) {
            this.buttonList.add(stealButton);
            this.buttonList.add(storeButton);
            stealButton.update(this.guiLeft, this.guiTop, this.xSize);
            storeButton.update(this.guiLeft, this.guiTop, this.xSize);
        }
    }

    @Inject(method = "updateScreen", at = @At("HEAD"))
    public void updateScreen(CallbackInfo ci) {
        stealButton.update(this.guiLeft, this.guiTop, this.xSize);
        storeButton.update(this.guiLeft, this.guiTop, this.xSize);
    }
}
