package dev.luna5ama.trollhack.modules

import dev.luna5ama.trollhack.i18n.LocalizedNameable
import dev.luna5ama.trollhack.manager.managers.ModuleManager
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.IEnumEntriesProvider

sealed class Category(
    override val displayName: CharSequence
) : LocalizedNameable(ModuleManager.resolve(displayName.toString()), ModuleManager.i18N, displayName.toString()), Displayable {
    data object COMBAT : Category("Fight")
    data object PLAYER : Category("Player")
    data object MOVEMENT : Category("Movement")
    data object VISUAL : Category("Visual")
    data object MISC : Category("Miscellaneous")
    data object CLIENT : Category("Client")
    data object HUD : Category("Hud")

    companion object : IEnumEntriesProvider<Category> {
        override val entries get() = listOf(COMBAT, PLAYER, MOVEMENT, VISUAL, MISC, CLIENT, HUD)
    }
}
