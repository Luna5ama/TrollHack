package cum.xiaro.trollhack.gui.hudgui.elements.misc

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.gui.hudgui.LabelHud
import cum.xiaro.trollhack.util.extension.rootName
import cum.xiaro.trollhack.util.threads.BackgroundScope

internal object MemoryUsage : LabelHud(
    name = "MemoryUsage",
    category = Category.MISC,
    description = "Display the used, allocated and max memory"
) {
    private val showAllocated by setting("Show Allocated", false)
    private val showMax by setting("Show Max", false)
    private val showRealtime by setting("Show Realtime", false)

    private const val BYTE_TO_MB = 1048576L
    private const val BYTE_TO_MB_D = 1048576.0
    private var lastUsed = getUsed()
    private var lastUpdate = System.nanoTime()
    private var counter = 0L
    private var realtime = 0

    init {
        BackgroundScope.launchLooping(rootName, 5L) {
            if (visible && showRealtime) {
                val last = lastUsed
                val lastTime = lastUpdate
                val current = getUsed()
                val currentTime = System.nanoTime()
                lastUsed = current

                val deltaTime = currentTime - lastTime
                if (deltaTime > 1_000_000_000) {
                    val finalCounter = counter
                    counter = 0L
                    val adjustFactor = deltaTime.toDouble() / 1_000_000_000.0
                    realtime = (finalCounter * adjustFactor / BYTE_TO_MB_D).toInt()
                    lastUpdate = currentTime
                }

                val diff = current - last
                if (diff > 0) counter += diff
            }
        }
    }

    override fun SafeClientEvent.updateText() {
        displayText.add(getUsedMB().toString(), primaryColor)

        if (showRealtime) {
            displayText.add("(${realtime}MB/s)", primaryColor)
        }
        if (showAllocated) {
            val allocatedMemory = Runtime.getRuntime().totalMemory() / BYTE_TO_MB
            displayText.add(allocatedMemory.toString(), primaryColor)
        }
        if (showMax) {
            val maxMemory = Runtime.getRuntime().maxMemory() / BYTE_TO_MB
            displayText.add(maxMemory.toString(), primaryColor)
        }

        displayText.add("MB", secondaryColor)
    }

    private fun getUsedMB(): Int {
        return (getUsed() / 1048576L).toInt()
    }

    private fun getUsed(): Long {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    }
}