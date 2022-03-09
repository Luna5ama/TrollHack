package me.luna.trollhack.gui.hudgui.elements.client

import me.luna.trollhack.TrollHackMod
import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.TickEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.gui.hudgui.LabelHud
import me.luna.trollhack.util.TickTimer
import org.lwjgl.opengl.Display

internal object Watermark : LabelHud(
    name = "Watermark",
    category = Category.CLIENT,
    description = "Troll Hack watermark",
    enabledByDefault = true
) {
    private val overrideName by setting("Override Name", false)
    private val clientName0 by setting("Client Name", TrollHackMod.NAME, { overrideName })
    private val overrideVersion by setting("Override Version", false)
    private val clientVersion0 by setting("Client Version", TrollHackMod.VERSION, { overrideVersion })
    private val windowTitle by setting("Window Title", true)

    private val clientName get() = if (overrideName) clientName0 else TrollHackMod.NAME
    private val clientVersion get() = if (overrideVersion) clientVersion0 else TrollHackMod.VERSION
    private val timer = TickTimer()

    init {
        listener<TickEvent.Pre>(true) {
            if (timer.tickAndReset(5000L)) {
                if (windowTitle) {
                    Display.setTitle("$clientName $clientVersion")
                } else {
                    Display.setTitle(TrollHackMod.title)
                }
            }
        }

        settingList.forEach {
            it.listeners.add {
                timer.reset(-69420L)
            }
        }
    }

    override fun SafeClientEvent.updateText() {
        displayText.add(clientName, secondaryColor)
        displayText.add(clientVersion, primaryColor)
    }
}