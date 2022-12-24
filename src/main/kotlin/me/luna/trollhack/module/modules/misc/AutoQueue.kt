package me.luna.trollhack.module.modules.misc

import me.luna.trollhack.event.events.ConnectionEvent
import me.luna.trollhack.event.events.PacketEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.text.MessageDetection
import me.luna.trollhack.util.text.MessageSendUtils
import me.luna.trollhack.util.text.MessageSendUtils.sendServerMessage
import net.minecraft.network.play.server.SPacketChat

internal object AutoQueue : Module(
    name = "AutoQueue",
    description = "Automatically answer questions at 2b2t.xin queue",
    category = Category.MISC
) {
    private val questions = arrayOf<String>("本服务:2020","苦力怕:爬行者","钻石的:264","大箱子:54","小箱子:27","羊驼会:不会","南瓜的:不需要","无限水:3","猪被闪:僵尸猪人","红石火:15","定位末:0","凋零死:下界之星","挖掘速:金镐")

    init {

        onEnable {
            if (!mc.currentServerData?.serverIP.equals("2b2t.xin")) {
                MessageSendUtils.sendNoSpamChatMessage("This feature is only support 2b2t.xin")
                disable()
            }
        }

        listener<PacketEvent.Receive> {
            if (it.packet !is SPacketChat || MessageDetection.Direct.RECEIVE detect it.packet.chatComponent.unformattedText) return@listener
            if(it.packet.chatComponent.unformattedText.contains("A.")){
                var i = -1
                for (index in questions) {
                    if (it.packet.chatComponent.unformattedText.contains(index.substring(0,3))) {
                        i = it.packet.chatComponent.unformattedText.indexOf(index.substring(4), 0, false)
                        break
                    }
                }
                if (i != -1) {
                    sendServerMessage(it.packet.chatComponent.unformattedText.substring(i - 2, i - 1))
                }else{
                    MessageSendUtils.sendChatMessage("This is an unknown question, please add")
                }
            }
        }

        listener<ConnectionEvent.Disconnect>{
            disable()
        }
    }
}
