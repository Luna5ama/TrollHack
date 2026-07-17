package dev.luna5ama.trollhack.mixins.screen;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.luna5ama.trollhack.interfaces.ILanguage;
import dev.luna5ama.trollhack.interfaces.ITranslationStorage;
import dev.luna5ama.trollhack.language.Config;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SessionSearchTrees.class, priority = 990)
abstract class MixinSearchManager {
    @ModifyExpressionValue(
            method = "lambda$getTooltipLines$1(Lnet/minecraft/network/chat/Component;)Ljava/lang/String;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;")
    )
    private static String addFallbackTranslationsToSearchTooltips(String original, Component tooltip) {
        if (Config.getInstance() == null) return original;
        if (!Config.getInstance().multilingualItemSearch) return original;

        var translationStorage = ((ILanguage) Language.getInstance()).languagereload_getTranslationStorage();
        if (translationStorage == null) return original;

        var stringBuilder = new StringBuilder(original);
        for (String fallbackCode : Config.getInstance().fallbacks) {
            ((ITranslationStorage) translationStorage).languagereload_setTargetLanguage(fallbackCode);
            stringBuilder.append('\n').append(tooltip.getString());
        }

        ((ITranslationStorage) translationStorage).languagereload_setTargetLanguage(null);
        return stringBuilder.toString();
    }
}
