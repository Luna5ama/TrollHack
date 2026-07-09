package dev.luna5ama.trollhack.i18n

import dev.luna5ama.trollhack.utils.Displayable


enum class Lang(override val displayName: String, val key: String, val minecraft: String) : Displayable {
    ENGLISH("English", "en_us", "en_us"),
    CHINESE_SIMPLIFIED("Chinese Simplified", "zh_cn", "zh_cn"),
    CHINESE_TRADITIONAL_TW("Chinese Traditional (TW)", "zh_tw", "zh_tw"),
    CHINESE_TRADITIONAL_HK("Chinese Traditional (HK)", "zh_hk", "zh_hk"),
    ESPERANTO("Esperanto", "esperanto", "*")
}