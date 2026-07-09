package dev.luna5ama.trollhack.interfaces;

import dev.luna5ama.trollhack.language.LanguageEntry;
import dev.luna5ama.trollhack.language.LanguageListWidget;

public interface ILanguageOptionsScreen {
    void languagereload_focusList(LanguageListWidget list);

    void languagereload_focusEntry(LanguageEntry entry);
}
