package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.mixins.core.network.MixinNetworkManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendNoSpamWarningMessage

/**
 * @see MixinNetworkManager
 */
internal object NoPacketKick : Module(
    name = "NoPacketKick",
    category = Category.PLAYER,
    description = "Suppress network exceptions and prevent getting kicked",
    visible = false
) {
    @JvmStatic
    fun sendWarning(throwable: Throwable) {
        sendNoSpamWarningMessage("$chatName Caught exception - \"$throwable\" check log for more info.")
        throwable.printStackTrace()
    }
}
