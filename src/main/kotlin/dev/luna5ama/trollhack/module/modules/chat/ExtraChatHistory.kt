package dev.luna5ama.trollhack.module.modules.chat

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import net.minecraft.client.gui.ChatLine
import net.minecraft.util.text.ITextComponent
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

internal object ExtraChatHistory : Module(
    name = "Extra Chat History",
    alias = arrayOf("InfiniteChat", "InfiniteChatHistory"),
    category = Category.CHAT,
    description = "Show more messages in the chat history",
    visible = false
) {
    private val maxMessages by setting("Max Message", 1000, 100..5000, 100)

    @JvmStatic
    fun handleSetChatLine(
        drawnChatLines: MutableList<ChatLine>,
        chatLines: MutableList<ChatLine>,
        chatComponent: ITextComponent,
        chatLineId: Int,
        updateCounter: Int,
        displayOnly: Boolean,
        ci: CallbackInfo
    ) {
        if (isDisabled) return

        while (drawnChatLines.isNotEmpty() && drawnChatLines.size > maxMessages) {
            drawnChatLines.removeLast()
        }

        if (!displayOnly) {
            chatLines.add(0, ChatLine(updateCounter, chatComponent, chatLineId))

            while (chatLines.isNotEmpty() && chatLines.size > maxMessages) {
                chatLines.removeLast()
            }
        }

        ci.cancel()
    }
}