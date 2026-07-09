package dev.luna5ama.trollhack.mixins.screen;

import com.google.common.collect.Maps;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.luna5ama.trollhack.language.Config;
import dev.luna5ama.trollhack.interfaces.ITranslationStorage;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Mixin(value = ClientLanguage.class)
abstract class MixinClientLanguage extends Language implements ITranslationStorage {
    @Unique private @Nullable String targetLanguage;
    @Unique private static Map<String, Map<String, String>> separateTranslationsOnLoad;
    @Unique private Map<String, Map<String, String>> separateTranslations;

    @Inject(method = "<init>", at = @At("RETURN"))
    void onConstructed(Map<String, String> translations, boolean rightToLeft, CallbackInfo ci) {
        separateTranslations = separateTranslationsOnLoad;
        separateTranslationsOnLoad = null;
    }

    @Inject(method = "loadFrom",
            at = @At("HEAD"))
    private static void onLoad(ResourceManager resourceManager, List<String> filenames, boolean defaultRightToLeft, CallbackInfoReturnable<ClientLanguage> cir) {
        separateTranslationsOnLoad = Maps.newHashMap();
    }

//    @ModifyReturnValue(method = "appendFrom", at = @At(value = "INVOKE",
//            target = "Lnet/minecraft/locale/Language;loadFromJson(Ljava/io/InputStream;Ljava/util/function/BiConsumer;)V"))
//    private static void onInternalLoad$saveSeparately(InputStream inputStream, BiConsumer<String, String> entryConsumer, String langCode) {
//        if (Config.getInstance().multilingualItemSearch) {
//            Language.loadFromJson(inputStream, entryConsumer.andThen((key, value) ->
//                    separateTranslationsOnLoad.computeIfAbsent(langCode, k -> Maps.newHashMap()).put(key, value)));
//        } else Language.loadFromJson(inputStream, entryConsumer);
//    }

    @Inject(method = "getOrDefault", at = @At(value = "HEAD"), cancellable = true)
    void onGet(String key, String fallback, CallbackInfoReturnable<String> cir) {
        if (targetLanguage != null) {
            var targetTranslations = separateTranslations.get(targetLanguage);
            cir.setReturnValue(targetTranslations == null ? "" : targetTranslations.getOrDefault(key, ""));
        }
    }

    @Override
    public @Nullable String languagereload_getTargetLanguage() {
        return targetLanguage;
    }

    @Override
    public void languagereload_setTargetLanguage(@Nullable String value) {
        targetLanguage = value;
    }
}
