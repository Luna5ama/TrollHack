package dev.luna5ama.trollhack.i18n

import dev.luna5ama.trollhack.utils.Nameable

interface ILocalizedNameable : ILocalized, Nameable {
    /**
     * Localized name, should only be used for rendering purposes
     */
    val localizedName: String
}