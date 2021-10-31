package cum.xiaro.trollhack.mixin.gui;

import cum.xiaro.trollhack.util.graphics.Easing;
import cum.xiaro.trollhack.module.modules.render.GuiAnimation;
import cum.xiaro.trollhack.util.graphics.AnimationFlag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionSlider;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11.GL_QUADS;

@Mixin(GuiOptionSlider.class)
public class MixinGuiOptionSlider extends GuiButton {
    private final AnimationFlag animation = new AnimationFlag(Easing.OUT_QUAD, 100);
    @Shadow private float sliderValue;

    public MixinGuiOptionSlider(int buttonId, int x, int y, String buttonText) {
        super(buttonId, x, y, buttonText);
    }

    @Inject(method = "<init>(IIILnet/minecraft/client/settings/GameSettings$Options;FF)V", at = @At("RETURN"))
    public void init$INJECT$RETURN(int buttonId, int x, int y, GameSettings.Options optionIn, float minValueIn, float maxValue, CallbackInfo ci) {
        animation.forceUpdate(0.0f, 0.0f);
    }

    @Inject(method = "mouseDragged", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiOptionSlider;drawTexturedModalRect(IIIIII)V", ordinal = 0), cancellable = true)
    public void mouseDragged$INJECT$HEAD(Minecraft mc, int mouseX, int mouseY, CallbackInfo ci) {
        if (GuiAnimation.INSTANCE.isEnabled()) {
            ci.cancel();

            float renderSliderValue = animation.getAndUpdate(this.sliderValue) * (this.width - 8.0f);


            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            
            bufferbuilder.pos(this.x + renderSliderValue, this.y + 20.0, this.zLevel).tex(0.0, 0.3359375).endVertex();
            bufferbuilder.pos(this.x + renderSliderValue + 4.0, this.y + 20.0, this.zLevel).tex(0.015625, 0.3359375).endVertex();
            bufferbuilder.pos(this.x + renderSliderValue + 4.0, this.y + 0.0, this.zLevel).tex(0.015625, 0.2578125).endVertex();
            bufferbuilder.pos(this.x + renderSliderValue, this.y + 0.0, this.zLevel).tex(0.0, 0.2578125).endVertex();

            bufferbuilder.pos(this.x + renderSliderValue + 4, this.y + 20.0, this.zLevel).tex(0.765625, 0.3359375).endVertex();
            bufferbuilder.pos(this.x + renderSliderValue + 8, this.y + 20.0, this.zLevel).tex(0.78125, 0.3359375).endVertex();
            bufferbuilder.pos(this.x + renderSliderValue + 8, this.y + 0.0, this.zLevel).tex(0.78125, 0.2578125).endVertex();
            bufferbuilder.pos(this.x + renderSliderValue + 4, this.y + 0.0, this.zLevel).tex(0.765625, 0.2578125).endVertex();
            
            tessellator.draw();
        }
    }

}
