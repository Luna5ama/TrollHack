package cum.xiaro.trollhack.gui.hudgui.elements.misc

import cum.xiaro.trollhack.util.graphics.Easing
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.InputEvent
import cum.xiaro.trollhack.event.events.RunGameLoopEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.gui.hudgui.LabelHud
import cum.xiaro.trollhack.util.TickTimer

internal object CPS : LabelHud(
    name = "CPS",
    category = Category.MISC,
    description = "Display your clicks per second."
) {

    private val averageSpeedTime by setting("Average Speed Time", 2.0f, 1.0f..5.0f, 0.1f, description = "The period of time to measure, in seconds")

    private val timer = TickTimer()
    private val clicks = ArrayDeque<Long>()
    private var currentCps = 0.0f
    private var prevCps = 0.0f

    init {
        listener<InputEvent.Mouse> {
            if (it.state && it.button == 0) {
                clicks.add(System.currentTimeMillis())
            }
        }

        listener<RunGameLoopEvent.Render> {
            if ((currentCps == 0.0f && clicks.size > 0) || timer.tickAndReset(1000L)) {
                val removeTime = System.currentTimeMillis() - (averageSpeedTime * 1000.0f).toLong()
                while (clicks.isNotEmpty() && clicks.first() < removeTime) {
                    clicks.removeFirst()
                }

                prevCps = currentCps
                currentCps = clicks.size / averageSpeedTime
            }
        }
    }

    override fun SafeClientEvent.updateText() {
        val deltaTime = Easing.toDelta(timer.time, 1000.0f)
        val cps = (prevCps + (currentCps - prevCps) * deltaTime)

        displayText.add("%.2f".format(cps), primaryColor)
        displayText.add("CPS", secondaryColor)
    }
}