package dev.luna5ama.trollhack.manager.managers

import kotlinx.coroutines.runBlocking
import dev.luna5ama.trollhack.TrollHackMod.i18N
import dev.luna5ama.trollhack.TrollHackMod.resolve
import dev.luna5ama.trollhack.config.Configurable
import dev.luna5ama.trollhack.i18n.ILocalizedNameable
import dev.luna5ama.trollhack.i18n.LocalizedNameable
import dev.luna5ama.trollhack.manager.AbstractManager
import dev.luna5ama.trollhack.utils.ChatUtils
import dev.luna5ama.trollhack.config.ConfigCategories

object FriendManager : AbstractManager(), ILocalizedNameable by LocalizedNameable(resolve("friends"), i18N),
    Configurable by Configurable.NamedConfigurable("Friends", ConfigCategories.FRIENDS) {
    var friends by setting("Friend Names", listOf()); private set

    fun isFriend(name: CharSequence) = name.toString() in friends

    fun clear() {
        friends = emptyList()
        runBlocking {
            ConfigManager.save()
        }
    }

    fun add(name: CharSequence) {
        ChatUtils.sendMessage("Added ${ChatUtils.GREEN}$name${ChatUtils.RESET} to friend.")
        friends += (name.toString())
        runBlocking {
            ConfigManager.save()
        }
    }

    fun remove(name: CharSequence) {
        ChatUtils.sendMessage("Removed ${ChatUtils.RED}$name{ChatUtils.RESET} from friends.")
        friends -= (name.toString())
        runBlocking {
            ConfigManager.save()
        }
    }
}