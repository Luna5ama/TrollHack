package me.luna.trollhack.module

import me.luna.trollhack.translation.TranslateType
import me.luna.trollhack.util.interfaces.DisplayEnum

enum class Category(override val displayName: CharSequence) : DisplayEnum {
    CHAT(TranslateType.COMMON commonKey "Chat"),
    CLIENT(TranslateType.COMMON commonKey "Client"),
    COMBAT(TranslateType.COMMON commonKey "Combat"),
    MISC(TranslateType.COMMON commonKey "Misc"),
    MOVEMENT(TranslateType.COMMON commonKey "Movement"),
    PLAYER(TranslateType.COMMON commonKey "Player"),
    RENDER(TranslateType.COMMON commonKey "Render");

    override fun toString() = displayString
}