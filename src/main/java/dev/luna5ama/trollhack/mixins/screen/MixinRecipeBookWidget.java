package dev.luna5ama.trollhack.mixins.screen;

import dev.luna5ama.trollhack.TrollHackMod;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedList;

@Mixin(RecipeBookComponent.class)
public class MixinRecipeBookWidget {
    @Inject(method = "pirateSpeechForThePeople", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/language/LanguageManager;setSelected(Ljava/lang/String;)V"))
    void onLanguageSwitching$cancel(String search, CallbackInfo ci) {
        TrollHackMod.ClientLanguageReload.INSTANCE.setLanguage("en_pt", new LinkedList<>() {{
            add(Language.DEFAULT);
        }});
        ci.cancel();
    }
}
