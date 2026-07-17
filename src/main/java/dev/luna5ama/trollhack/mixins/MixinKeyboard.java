package dev.luna5ama.trollhack.mixins;

import com.mojang.blaze3d.platform.InputConstants;
import dev.luna5ama.trollhack.TrollHackMod;
import dev.luna5ama.trollhack.language.Config;
import dev.luna5ama.trollhack.utils.input.KeyBind;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Objects;

@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboard {

    @Inject(method = "keyPress", at = @At("HEAD"))
    public void onKey(long window, int action, KeyEvent event, CallbackInfo info) {
        Minecraft mc = Minecraft.getInstance();
        boolean whitelist = mc.screen == null;
        int key = event.key();
        if (key != GLFW.GLFW_KEY_UNKNOWN) {
            if (action == 1 && whitelist) {
                TrollHackMod.INSTANCE.onKeyPressed(new KeyBind(KeyBind.Category.KEYBOARD, key, event.scancode()));
            } else if (action == 0) {
                TrollHackMod.INSTANCE.onKeyReleased(new KeyBind(KeyBind.Category.KEYBOARD, key, event.scancode()));
            }
        }
    }

    @Shadow @Final private Minecraft minecraft;

    @Shadow protected abstract void debugFeedbackTranslated(String message, Object... args);

    @Unique
    private void processLanguageReloadKeys() {
        if (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            var config = Config.getInstance();
            var languageManager = minecraft.getLanguageManager();

            var language = languageManager.getLanguage(config.previousLanguage);
            var noLanguage = config.previousLanguage.equals(TrollHackMod.NO_LANGUAGE);
            if (language == null && !noLanguage) {
                minecraft.gui.getChat().addClientSystemMessage(Component.translatable("debug.reload_languages.switch.failure"));
            } else {
                TrollHackMod.ClientLanguageReload.INSTANCE.setLanguage(config.previousLanguage, config.previousFallbacks);
                var languages = new ArrayList<Component>() {{
                    if (noLanguage)
                        add(Component.literal("No language"));
                    if (language != null)
                        add(language.toComponent());
                    addAll(config.fallbacks.stream()
                            .map(languageManager::getLanguage)
                            .filter(Objects::nonNull)
                            .map(LanguageInfo::toComponent)
                            .toList());
                }};
                debugFeedbackTranslated("debug.reload_languages.switch.success", ComponentUtils.formatList(languages, Component.literal(", ")));
            }
        } else {
            TrollHackMod.ClientLanguageReload.INSTANCE.reloadLanguages();
            debugFeedbackTranslated("debug.reload_languages.message");
        }
    }

    @Inject(method = "handleDebugKeys", at = @At("RETURN"), cancellable = true)
    private void onProcessF3(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.key() == GLFW.GLFW_KEY_Q) {
            minecraft.gui.getChat().addClientSystemMessage(Component.translatable("debug.reload_languages.help"));
        }
        if (event.key() == GLFW.GLFW_KEY_J) {
            processLanguageReloadKeys();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onOnKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_F3) && event.key() == GLFW.GLFW_KEY_J) {
            if (action != 0)
                processLanguageReloadKeys();
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onOnChar(long window, CharacterEvent event, CallbackInfo ci) {
        if (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_F3) && InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_J)) {
            ci.cancel();
        }
    }
}
