package dev.luna5ama.trollhack.mixins.core.render;

import dev.fastmc.common.MathUtilKt;
import dev.luna5ama.trollhack.TrollHackMod;
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer;
import dev.luna5ama.trollhack.module.modules.chat.Emoji;
import dev.luna5ama.trollhack.module.modules.client.CustomFont;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

@Mixin(FontRenderer.class)
public abstract class MixinFontRenderer {

    @Shadow
    public int FONT_HEIGHT;
    @Shadow
    protected float posX;
    @Shadow
    protected float posY;
    @Shadow
    private float alpha;
    @Shadow
    private float red;
    @Shadow
    private float green;
    @Shadow
    private float blue;

    @Shadow
    protected abstract void renderStringAtPos(String text, boolean shadow);

    @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), cancellable = true)
    private void drawString$Inject$HEAD(
        String text,
        float x,
        float y,
        int color,
        boolean dropShadow,
        CallbackInfoReturnable<Integer> cir
    ) {
        handleDrawString(text, x, y, color, dropShadow, cir);
    }

    @Inject(method = "renderString", at = @At("HEAD"), cancellable = true)
    private void renderString$Inject$HEAD(
        String text,
        float x,
        float y,
        int color,
        boolean shadow,
        CallbackInfoReturnable<Integer> cir
    ) {
        handleDrawString(text, x, y, color, false, cir);
    }

    private void handleDrawString(
        String text,
        float x,
        float y,
        int color,
        boolean drawShadow,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (TrollHackMod.isReady() && CustomFont.INSTANCE.getOverrideMinecraft()) {
            posX = x;
            posY = y;

            if (Emoji.INSTANCE.isEnabled() && text.contains(":")) {
                text = Emoji.renderText(text, FONT_HEIGHT, false, posX, posY, alpha);
                GlStateManager.color(red, blue, green, alpha); // Big Mojang meme :monkey:
            }

            GlStateManager.tryBlendFuncSeparate(
                GL_SRC_ALPHA,
                GL_ONE_MINUS_SRC_ALPHA,
                GL_SRC_ALPHA,
                GL_ONE_MINUS_SRC_ALPHA
            );
            MainFontRenderer.INSTANCE.drawStringJava(text, x, y, color, 1.0f, drawShadow);
            cir.setReturnValue(MathUtilKt.ceilToInt(x + MainFontRenderer.INSTANCE.getWidth(text)));
        }
    }

    @Inject(method = "renderString", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;renderStringAtPos(Ljava/lang/String;Z)V", shift = At.Shift.BEFORE), cancellable = true)
    private void renderString$Inject$INVOKE$renderStringAtPos(
        String text,
        float x,
        float y,
        int color,
        boolean shadow,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (TrollHackMod.isReady() && !CustomFont.INSTANCE.getOverrideMinecraft() && Emoji.INSTANCE.isEnabled() && text.contains(
            ":")) {
            text = Emoji.renderText(text, FONT_HEIGHT, shadow, posX, posY, alpha);
            GlStateManager.color(red, blue, green, alpha); // Big Mojang meme :monkey:
            renderStringAtPos(text, shadow);
            cir.setReturnValue((int) posX);
        }
    }

    @Inject(method = "getStringWidth", at = @At("HEAD"), cancellable = true)
    public void getStringWidth$Inject$HEAD(String text, CallbackInfoReturnable<Integer> cir) {
        if (TrollHackMod.isReady() && CustomFont.INSTANCE.getOverrideMinecraft()) {
            if (Emoji.INSTANCE.isEnabled() && text.contains(":")) {
                cir.setReturnValue(Emoji.getStringWidthCustomFont(text));
            } else {
                cir.setReturnValue(MathUtilKt.ceilToInt(MainFontRenderer.INSTANCE.getWidth(text)));
            }
        }
    }

    @Inject(method = "getStringWidth", at = @At("TAIL"), cancellable = true)
    public void getStringWidth$Inject$TAIL(String text, CallbackInfoReturnable<Integer> cir) {
        if (TrollHackMod.isReady() && cir.getReturnValue() != 0 && !CustomFont.INSTANCE.getOverrideMinecraft() && Emoji.INSTANCE.isEnabled() && text.contains(
            ":")) {
            cir.setReturnValue(Emoji.getStringWidth(cir.getReturnValue(), text, FONT_HEIGHT));
        }
    }

    @Inject(method = "getCharWidth", at = @At("HEAD"), cancellable = true)
    public void getCharWidth$Inject$HEAD(char character, CallbackInfoReturnable<Integer> cir) {
        if (TrollHackMod.isReady() && CustomFont.INSTANCE.getOverrideMinecraft()) {
            cir.setReturnValue(MathUtilKt.ceilToInt(MainFontRenderer.INSTANCE.getWidth(character)));
        }
    }
}
