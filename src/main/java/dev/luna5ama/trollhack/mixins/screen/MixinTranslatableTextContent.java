package dev.luna5ama.trollhack.mixins.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import dev.luna5ama.trollhack.language.Config;
import dev.luna5ama.trollhack.interfaces.ILanguage;
import dev.luna5ama.trollhack.interfaces.ITranslationStorage;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.TranslatableFormatException;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@Mixin(TranslatableContents.class)
abstract class MixinTranslatableTextContent implements ComponentContents {
    @Unique private @Nullable String previousTargetLanguage;
    @Unique private final Map<String, List<FormattedText>> separateTranslationsCache = Maps.newHashMap();
    @Unique private @Nullable List<FormattedText> savedTranslations;

    @Shadow
    @Final
    private String key;
    @Shadow private @Nullable Language decomposedWith;
    @Shadow private List<FormattedText> decomposedParts;

    @Inject(method = "decompose", at = @At("RETURN"))
    void onUpdateTranslations(CallbackInfo ci) {
        if (Config.getInstance() == null) return;
        if (!Config.getInstance().multilingualItemSearch) return;
        if (decomposedWith == null) return;

        var translationStorage = ((ILanguage) decomposedWith).languagereload_getTranslationStorage();
        if (translationStorage == null) return;

        var targetLanguage = ((ITranslationStorage) translationStorage).languagereload_getTargetLanguage();
        if (Objects.equals(previousTargetLanguage, targetLanguage)) return;

        if (targetLanguage == null) {
            previousTargetLanguage = null;
            decomposedParts = savedTranslations;
            savedTranslations = null;
            return;
        }

        if (previousTargetLanguage == null) {
            savedTranslations = decomposedParts;
        }
        previousTargetLanguage = targetLanguage;
        decomposedParts = separateTranslationsCache.computeIfAbsent(targetLanguage, k -> {
            var string = decomposedWith.getOrDefault(key);
            try {
                var builder = new ImmutableList.Builder<FormattedText>();
                this.decomposeTemplate(string, builder::add);
                return builder.build();
            } catch (TranslatableFormatException e) {
                return ImmutableList.of(FormattedText.of(string));
            }
        });
    }

    @Inject(method = "decompose", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/locale/Language;getOrDefault(Ljava/lang/String;)Ljava/lang/String;"))
    void onUpdateTranslations$clearCache(CallbackInfo ci) {
        previousTargetLanguage = null;
        separateTranslationsCache.clear();
        savedTranslations = null;
    }

    @Shadow protected abstract void decomposeTemplate(String translation, Consumer<FormattedText> partsConsumer);
}
