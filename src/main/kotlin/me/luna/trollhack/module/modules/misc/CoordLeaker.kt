package me.luna.trollhack.module.modules.misc

import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.text.MessageSendUtils.sendServerMessage

internal object CoordLeaker : Module(
    name = "CoordLeaker",
    description = "Leak your coord at chat box",
    category = Category.MISC
) {

    private val customText = setting("Text", "[AutoLeak] My coord in the (world) are (coord)")

    init{
        onEnable {

            var s:String = customText.value

            when (mc.player?.dimension) {
                -1 -> { // Nether
                    s = s.replace("(world)","Nether")
                }
                0 -> { // Overworld
                    s = s.replace("(world)","Overworld")
                }
                1 -> { // End
                    s = s.replace("(world)","End")
                }
            }
            s = s.replace("(coord)",mc.player.posX.toInt().toString() + " " + mc.player.posY.toInt().toString() + " " + mc.player.posZ.toInt().toString() )

            sendServerMessage(s)

            disable()
        }
    }
}
