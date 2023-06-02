package dev.luna5ama.trollhack.module.modules.render

import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.mixins.PatchedITextComponent
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.accessor.textComponent
import dev.luna5ama.trollhack.util.text.EnumTextColor
import dev.luna5ama.trollhack.util.text.unformatted
import dev.luna5ama.trollhack.util.threads.onMainThread
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextFormatting
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

internal object FriendHighlight : Module(
    name = "Friend Highlight",
    description = "Highlights friends in GUI",
    category = Category.RENDER,
    visible = false
) {
    private val chat by setting("Chat", true)
    private val messageSound by setting("Message Sound", false)
    private val tabList by setting("Tab List", true)
    private val bold by setting("Bold", false)
    private val color by setting("Color", EnumTextColor.GREEN)

    private val playerNameRegex = "<(.+?)>".toRegex()

    init {
        listener<PacketEvent.Receive>(-100) {
            if (!chat) return@listener
            if (!FriendManager.enabled) return@listener
            if (it.packet !is SPacketChat) return@listener

            if (replace(it.packet) && messageSound) {
                onMainThread {
                    mc.soundHandler.playSound(
                        PositionedSoundRecord.getRecord(
                            SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                            1.0f,
                            1.0f
                        )
                    )
                }
            }
        }
    }

    private fun replace(packet: SPacketChat): Boolean {
        val playerName = playerNameRegex.findAll(packet.textComponent.unformatted).map {
            it.groupValues[1]
        }.find {
            FriendManager.isFriend(it)
        }

        return if (playerName != null) {
            val list = (packet.textComponent as PatchedITextComponent).inplaceIterator().asSequence().toList()
            if (list.size == 1) {
                // Whole message in 1 component
                replaceComponent(packet.textComponent, playerName)?.let {
                    packet.textComponent = it
                }
            } else {
                val nameComponent = list.find { it.unformatted == playerName }
                if (nameComponent != null) {
                    nameComponent.style.color = color.textFormatting
                } else {
                    replaceSibling(packet.textComponent, playerName)
                }
            }
            true
        } else {
            false
        }
    }

    fun replaceSibling(component: ITextComponent, playerName: String): Boolean {
        val list = component.siblings
        for (i in list.indices) {
            replaceComponent(list[i], playerName)?.let {
                list[i] = it
                return true
            }
        }

        list.forEach {
            if (replaceSibling(it, playerName)) {
                return true
            }
        }

        return false
    }

    @JvmStatic
    fun getPlayerName(info: NetworkPlayerInfo, cir: CallbackInfoReturnable<String>) {
        if (isDisabled || !tabList || !FriendManager.enabled) return

        val name = info.gameProfile.name

        if (FriendManager.isFriend(name)) {
            cir.returnValue = getReplacement(name)
        }
    }

    private fun replaceComponent(component: ITextComponent, playerName: String): TextComponentString? {
        var found = false
        val newText = component.formattedText.replace(playerNameRegex) {
            if (it.groupValues[1].contains(playerName)) {
                found = true
                it.value.replace(playerName, getReplacement(playerName))
            } else {
                it.value
            }
        }
        return if (found) TextComponentString(newText).apply { style = component.style } else null
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