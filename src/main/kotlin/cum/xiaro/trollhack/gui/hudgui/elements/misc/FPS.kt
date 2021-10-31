package cum.xiaro.trollhack.gui.hudgui.elements.misc

import cum.xiaro.trollhack.util.collections.CircularArray
import cum.xiaro.trollhack.util.graphics.Easing
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.gui.hudgui.LabelHud
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal object FPS : LabelHud(
    name = "FPS",
    category = Category.MISC,
    description = "Frame per second in game"
) {

    private val showAverage = setting("Show Average", true)
    private val showMin = setting("Show Min", false)
    private val showMax = setting("Show Max", false)

    private var updateTime = 0L
    private var prevFps = 0
    private var currentFps = 0

    private val longFps = CircularArray(10, 0)

    private var prevAvgFps = 0
    private var currentAvgFps = 0

    @JvmStatic
    fun updateFps(fps: Int) {
        prevFps = currentFps
        currentFps = fps

        longFps.add(fps)

        prevAvgFps = currentAvgFps
        currentAvgFps = longFps.average().roundToInt()
        updateTime = System.currentTimeMillis()
    }

    override fun SafeClientEvent.updateText() {
        val deltaTime = Easing.toDelta(updateTime, 1000.0f)
        val fps = (prevFps + (currentFps - prevFps) * deltaTime).roundToInt()
        val avg = (prevAvgFps + (currentAvgFps - prevAvgFps) * deltaTime).roundToInt()

        var min = 6969
        var max = 0
        for (value in longFps) {
            if (value != 0) min = min(value, min)
            max = max(value, max)
        }

        displayText.add("FPS", secondaryColor)
        displayText.add(fps.toString(), primaryColor)

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