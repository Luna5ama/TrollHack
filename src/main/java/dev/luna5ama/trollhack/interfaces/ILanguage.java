package dev.luna5ama.trollhack.interfaces;

import net.minecraft.client.resources.language.ClientLanguage;

public interface ILanguage {
    void languagereload_setTranslationStorage(ClientLanguage translationStorage);

    ClientLanguage languagereload_getTranslationStorage();
}
