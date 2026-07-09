package dev.luna5ama.trollhack.mixins.language;

import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.luna5ama.trollhack.TrollHackMod;
import dev.luna5ama.trollhack.language.Config;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(LanguageManager.class)
abstract class MixinLanguageManager {
    @Shadow
    private Map<String, LanguageInfo> languages;

    @Shadow public abstract LanguageInfo getLanguage(String code);

    @Redirect(method = "onResourceManagerReload", at = @At(value = "INVOKE", ordinal = 0, remap = false,
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    boolean onReload$addFallbacks(List<String> list, Object enUsCode) {
        Lists.reverse(Config.getInstance().fallbacks).stream()
                .filter(code -> Objects.nonNull(getLanguage(code)))
                .forEach(list::add);
        return true;
    }

    @ModifyExpressionValue(method = "onResourceManagerReload", at = @At(value = "INVOKE", remap = false,
            target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"))
    boolean onReload$ignoreNoLanguage(boolean original) {
        return Config.getInstance().language.equals(TrollHackMod.NO_LANGUAGE);
    }

    @Inject(method = "onResourceManagerReload", at = @At(value = "INVOKE", ordinal = 0, remap = false,
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    void onReload$setSystemLanguage(ResourceManager resourceManager, CallbackInfo ci) {
        if (TrollHackMod.INSTANCE.getShouldSetSystemLanguage()) {
            TrollHackMod.INSTANCE.setShouldSetSystemLanguage(false);
            TrollHackMod.LOGGER.info("Language is not set. Setting it to system language");

            var locale = Locale.getDefault();
            var matchingLanguages = languages.keySet().stream()
                    .filter(code -> code.split("_")[0].equalsIgnoreCase(locale.getLanguage()))
                    .toList();
            var count = matchingLanguages.size();
            if (count > 1) matchingLanguages.stream()
                    .filter(code -> {
                        var split = code.split("_");
                        if (split.length < 2) return false;
                        return split[1].equalsIgnoreCase(locale.getCountry());
                    })
                    .findFirst()
                    .ifPresent(lang -> setSystemLanguage(lang, locale));
            else if (count == 1) setSystemLanguage(matchingLanguages.getFirst(), locale);
        }
    }

    @Unique
    private static void setSystemLanguage(String lang, Locale locale) {
        TrollHackMod.LOGGER.info("Set language to {} (mapped from {})", lang, locale.toLanguageTag());
        TrollHackMod.ClientLanguageReload.INSTANCE.setLanguage(lang, new LinkedList<>() {{
            if (!lang.equals(Language.DEFAULT))
                add(Language.DEFAULT);
        }});
    }
}
