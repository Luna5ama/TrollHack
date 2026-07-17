package dev.luna5ama.trollhack.gui

import dev.luna5ama.trollhack.modules.impl.client.ClickGui

internal fun clickGuiText(key: String, defaultText: String): String {
    return ClickGui.i18N["${ClickGui.translateKey}.${key.lowercase()}", defaultText][ClickGui.i18N.currentLang]
}
