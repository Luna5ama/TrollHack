package dev.luna5ama.trollhack.i18n

open class LocalizedNameable(
    final override val translateKey: String, final override val i18N: I18N, private val defaultName: String = translateKey
) : ILocalizedNameable {
    @Suppress("LeakingThis")
    override val localizedName by i18N[translateKey, defaultName]
    override val name: CharSequence by i18N[translateKey, defaultName]

    override fun resolve(subKey: String) = "$translateKey.${subKey.lowercase().replace(" ", "")}"
}