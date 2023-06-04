package dev.luna5ama.trollhack.module

import dev.luna5ama.trollhack.translation.TranslateType
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum

enum class Category(override val displayName: CharSequence) : DisplayEnum {
    CHAT(TranslateType.COMMON commonKey "Chat"),
    CLIENT(TranslateType.COMMON commonKey "Client"),
    EXPLOIT(TranslateType.COMMON commonKey "Exploit"),
    COMBAT(TranslateType.COMMON commonKey "Combat"),
    MISC(TranslateType.COMMON commonKey "Misc"),
    MOVEMENT(TranslateType.COMMON commonKey "Movement"),
    PLAYER(TranslateType.COMMON commonKey "Player"),
    RENDER(TranslateType.COMMON commonKey "Render");

    override fun toString() = displayString
}