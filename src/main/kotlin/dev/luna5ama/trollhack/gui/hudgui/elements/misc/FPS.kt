package dev.luna5ama.trollhack.gui.hudgui.elements.misc

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import kotlin.math.max
import kotlin.math.min

internal object FPS : LabelHud(
    name = "FPS",
    category = Category.MISC,
    description = "Frame per second in game"
) {

    private val showAverage = setting("Show Average", true)
    private val showRenderTime by setting("Show Render Time", false)
    private val showMin = setting("Show Min", false)
    private val showMax = setting("Show Max", false)

    private var lastRender = System.nanoTime()

    private val shortFps = ArrayList<Pair<Long, Int>>()
    private val longFps = ArrayList<Pair<Long, Int>>()

    init {
        listener<RunGameLoopEvent.End> {
            val current = System.nanoTime()

            shortFps.removeIf {
                it.first <= current
            }

            shortFps.add(current + 1_000_000_000 to (current - lastRender).toInt())

            lastRender = current
        }
    }

    override fun SafeClientEvent.updateText() {
        val current = System.nanoTime()

        val millisSum = shortFps.sumOf {
            it.second.toDouble() / 1_000_000.0
        }
        val renderTime = millisSum / shortFps.size
        val fps = (1000.0 / renderTime).toInt()

        longFps.removeIf {
            it.first <= current
        }
        longFps.add(current + 5_000_000_000 to fps)

        var min = 6969
        var max = 0
        var avg = 0

        for ((_, value) in longFps) {
            if (value != 0) min = min(value, min)
            max = max(value, max)
            avg += value
        }

        avg /= longFps.size

        displayText.add("FPS", secondaryColor)
        displayText.add(fps.toString(), primaryColor)

        if (showRenderTime) {
            displayText.add("(%.2f ms)".format(renderTime), primaryColor)
        }

        if (showAverage.value) {
            displayText.add("AVG", secondaryColor)
            displayText.add(avg.toString(), primaryColor)
        }

        if (showMin.value) {
            displayText.add("MIN", secondaryColor)
            displayText.add(min.toString(), primaryColor)
        }

        if (showMax.value) {
            displayText.add("MAX", secondaryColor)
            displayText.add(max.toString(), primaryColor)
        }
    }

}