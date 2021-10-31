package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.mixin.network.MixinNetworkManager
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.text.MessageSendUtils.sendNoSpamWarningMessage

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
