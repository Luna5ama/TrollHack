package dev.luna5ama.trollhack.interfaces;

import org.jetbrains.annotations.Nullable;

public interface ITranslationStorage {
    @Nullable String languagereload_getTargetLanguage();

    void languagereload_setTargetLanguage(@Nullable String value);
}
