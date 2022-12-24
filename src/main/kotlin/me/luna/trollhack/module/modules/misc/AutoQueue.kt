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
    private val questions: HashMap<String,String> = hashMapOf(
        "本服务" to "2020",
        "苦力怕" to "爬行者",
        "钻石的" to "264",
        "大箱子" to "54",
        "小箱子" to "27",
        "羊驼会" to "不会",
        "南瓜的" to "不需要",
        "无限水" to "3",
        "猪被闪" to "僵尸猪人",
        "红石火" to "15",
        "定位末" to "0",
        "凋零死" to "下界之星",
        "挖掘速" to "金镐"
    )

    private var is2B2T:Boolean = false

    init {
        listener<ConnectionEvent.Connect>{
            if (!mc.currentServerData?.serverIP.equals("2b2t.xin")) {
                is2B2T = true
            }
            is2B2T = false
        }

        listener<PacketEvent.Receive> {
            if(is2B2T){
                if (it.packet !is SPacketChat || MessageDetection.Direct.RECEIVE detect it.packet.chatComponent.unformattedText) return@listener
                if(it.packet.chatComponent.unformattedText.contains("?丨选项：A.")){
                    var i = -1
                    for ((question, answer) in questions) {
                        if (it.packet.chatComponent.unformattedText.contains(question)) {
                            i = it.packet.chatComponent.unformattedText.indexOf(answer, 0, false)
                            if(i != -1){
                                sendServerMessage(String.format("%s",it.packet.chatComponent.unformattedText.substring(i - 2, i - 1)))
                            }
                            break
                        }
                    }
                }
            }
        }
    }
}
