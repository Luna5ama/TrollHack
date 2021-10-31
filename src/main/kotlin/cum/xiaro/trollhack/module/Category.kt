package cum.xiaro.trollhack.module

import cum.xiaro.trollhack.util.interfaces.DisplayEnum
import cum.xiaro.trollhack.util.translation.I18nType

enum class Category(override val displayName: CharSequence) : DisplayEnum {
    CHAT(I18nType.COMMON commonKey "Chat"),
    CLIENT(I18nType.COMMON commonKey "Client"),
    COMBAT(I18nType.COMMON commonKey "Combat"),
    MISC(I18nType.COMMON commonKey "Misc"),
    MOVEMENT(I18nType.COMMON commonKey "Movement"),
    PLAYER(I18nType.COMMON commonKey "Player"),
    RENDER(I18nType.COMMON commonKey "Render");

    override fun toString() = displayString
}