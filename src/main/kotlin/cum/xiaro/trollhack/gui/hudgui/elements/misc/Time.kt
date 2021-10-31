package cum.xiaro.trollhack.gui.hudgui.elements.misc

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.gui.hudgui.LabelHud
import cum.xiaro.trollhack.util.TimeUtils

internal object Time : LabelHud(
    name = "Time",
    category = Category.MISC,
    description = "System date and time"
) {

    private val showDate = setting("Show Date", true)
    private val showTime = setting("Show Time", true)
    private val dateFormat = setting("Date Format", TimeUtils.DateFormat.DDMMYY, { showDate.value })
    private val timeFormat = setting("Time Format", TimeUtils.TimeFormat.HHMM, { showTime.value })
    private val timeUnit = setting("Time Unit", TimeUtils.TimeUnit.H12, { showTime.value })

    override fun SafeClientEvent.updateText() {
        if (showDate.value) displayText.addLine(TimeUtils.getDate(dateFormat.value))
        if (showTime.value) displayText.addLine(TimeUtils.getTime(timeFormat.value, timeUnit.value))
    }

}