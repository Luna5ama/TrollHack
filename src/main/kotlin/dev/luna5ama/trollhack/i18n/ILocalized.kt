package dev.luna5ama.trollhack.i18n

interface ILocalized {
    val i18N: I18N
    val translateKey: String

    fun resolve(subKey: String): String
}