package dev.luna5ama.trollhack.gui.hudgui.elements.client

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import org.lwjgl.opengl.Display
import java.lang.reflect.Field
import java.lang.reflect.Method

internal object Watermark : LabelHud(
    name = "Watermark",
    category = Category.CLIENT,
    description = "Troll Hack watermark",
    enabledByDefault = true
) {
    private const val title = "${TrollHackMod.NAME} ${TrollHackMod.VERSION}"
    private val timer = TickTimer()
    private val titleField: Field
    private val displayImpl: Any
    private val setTitleMethod: Method

    init {
        val displayClazz = Display::class.java
        titleField = displayClazz.getDeclaredField("title")

        val displayImplField = displayClazz.getDeclaredField("display_impl")
        displayImplField.isAccessible = true
        displayImpl = displayImplField.get(null)
        displayImplField.isAccessible = false

        val displayImplClass = Class.forName("org.lwjgl.opengl.DisplayImplementation")
        setTitleMethod = displayImplClass.getDeclaredMethod("setTitle", String::class.java)

        listener<TickEvent.Pre>(true) {
            if (timer.tickAndReset(3000L)) {
                try {
                    titleField.isAccessible = true
                    titleField.set(null, title)
                    titleField.isAccessible = false

                    setTitleMethod.isAccessible = true
                    setTitleMethod.invoke(displayImpl, title)
                    setTitleMethod.isAccessible = false
                } catch (t: Throwable) {
                    //
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
        displayText.add(TrollHackMod.NAME, GuiSetting.primary)
        displayText.add(TrollHackMod.VERSION, GuiSetting.text)
    }
}