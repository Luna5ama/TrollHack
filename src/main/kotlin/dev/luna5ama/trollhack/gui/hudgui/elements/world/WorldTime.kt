package dev.luna5ama.trollhack.gui.hudgui.elements.world

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import org.apache.commons.lang3.time.DurationFormatUtils

internal object WorldTime : LabelHud(
    name = "World Time",
    category = Category.WORLD,
    description = "Time in the Minecraft world"
) {

    private val displayMode by setting("Display Mode", DisplayMode.H24)
    private val fromMidNight by setting(
        "From Midnight",
        true,
        { displayMode == DisplayMode.REAL_TIME || displayMode == DisplayMode.TICKS })

    private enum class DisplayMode(override val displayName: CharSequence) : DisplayEnum {
        H12("12-Hours"),
        H24("24-Hours"),
        REAL_TIME("Real Time"),
        TICKS("Ticks"), // Dummy format
    }

    override fun SafeClientEvent.updateText() {
        displayText.add("World Time ", GuiSetting.primary)

        val ticks = getWorldTimeTicks()

        when (displayMode) {
            DisplayMode.H12 -> {
                var ticksHalf = ticks % 12000L
                if (ticksHalf < 1000L) ticksHalf += 12000L // Hacky way to display 12:00 instead of 00:00

                val millisHalf = ticksHalf * 3600L
                val timeString = DurationFormatUtils.formatDuration(millisHalf, "HH:mm")

                val period = if (ticks < 12000L) "AM" else "PM"

                displayText.add(timeString, GuiSetting.text)
                displayText.add(period, GuiSetting.primary)
            }
            DisplayMode.H24 -> {
                val millis = ticks * 3600L
                val timeString = DurationFormatUtils.formatDuration(millis, "HH:mm")

                displayText.add(timeString, GuiSetting.text)
            }
            DisplayMode.REAL_TIME -> {
                val realTimeMillis = ticks * 50L
                val timeString = DurationFormatUtils.formatDuration(realTimeMillis, "mm:ss")

                displayText.add(timeString, GuiSetting.text)
            }
            DisplayMode.TICKS -> {
                displayText.add("$ticks", GuiSetting.text)
                displayText.add("ticks", GuiSetting.primary)
            }
        }
    }

    private fun SafeClientEvent.getWorldTimeTicks() =
        if (fromMidNight && (displayMode == DisplayMode.H12 || displayMode == DisplayMode.H24)) {
            Math.floorMod(world.worldTime - 18000L, 24000L)
        } else {
            world.worldTime
        }

}