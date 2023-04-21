package dev.luna5ama.trollhack.mixins.core.gui;

import dev.luna5ama.trollhack.module.modules.player.ChestStealer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public class MixinGuiContainer extends GuiScreen {
    private final ChestStealer.StealButton trollHackStealButton = new ChestStealer.StealButton();
    private final ChestStealer.StoreButton trollHackStoreButton = new ChestStealer.StoreButton();
    @Shadow
    protected int guiLeft;
    @Shadow
    protected int guiTop;
    @Shadow
    protected int xSize;

    @Inject(method = "initGui", at = @At("RETURN"))
    public void initGui(CallbackInfo ci) {
        if (ChestStealer.INSTANCE.isValidGui()) {
            this.buttonList.add(trollHackStealButton);
            this.buttonList.add(trollHackStoreButton);
            trollHackStealButton.update(this.guiLeft, this.guiTop, this.xSize);
            trollHackStoreButton.update(this.guiLeft, this.guiTop, this.xSize);
        }
    }

    @Inject(method = "updateScreen", at = @At("HEAD"))
    public void updateScreen(CallbackInfo ci) {
        trollHackStealButton.update(this.guiLeft, this.guiTop, this.xSize);
        trollHackStoreButton.update(this.guiLeft, this.guiTop, this.xSize);
    }
}
