package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.manager.managers.FriendManager
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.accessor.textComponent
import cum.xiaro.trollhack.util.graphics.color.EnumTextColor
import cum.xiaro.trollhack.util.text.format
import cum.xiaro.trollhack.util.threads.onMainThread
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextFormatting
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

internal object FriendHighlight : Module(
    name = "FriendHighlight",
    description = "Highlights friends in GUI",
    category = Category.RENDER,
    visible = false
) {
    private val chat by setting("Chat", true)
    private val messageSound by setting("Message Sound", false)
    private val tabList by setting("Tab List", true)
    private val bold by setting("Bold", false)
    private val color by setting("Color", EnumTextColor.GREEN)

    private val regex1 = "<(.*?)>".toRegex()

    init {
        listener<PacketEvent.Receive>(-100) {
            if (!chat || !FriendManager.enabled || it.packet !is SPacketChat) return@listener
            if (replace(it.packet.textComponent) && messageSound) {
                onMainThread {
                    mc.soundHandler.playSound(PositionedSoundRecord.getRecord(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f))
                }
            }
        }
    }

    private fun replace(textComponent: ITextComponent): Boolean {
        var friend = false

        for ((index, sibling) in textComponent.siblings.withIndex()) {
            val playerName = regex1.find(sibling.unformattedComponentText)?.groupValues?.get(1)
            if (playerName != null && FriendManager.isFriend(playerName)) {
                val modified = TextComponentString(sibling.unformattedComponentText.replaceFirst(playerName, getReplacement(playerName)))
                textComponent.siblings[index] = modified
                friend = true
                continue
            }

            replace(sibling)
        }

        return friend
    }

    @JvmStatic
    fun getPlayerName(info: NetworkPlayerInfo, cir: CallbackInfoReturnable<String>) {
        if (isDisabled || !tabList || !FriendManager.enabled) return

        val name = info.gameProfile.name

        if (FriendManager.isFriend(name)) {
            cir.returnValue = getReplacement(name)
        }
    }

    private fun getReplacement(name: String): String {
        return buildString {
            append(color.textFormatting)
            if (bold) append(TextFormatting.BOLD)
            append(name)
            append(TextFormatting.RESET)
        }
    }
}