package dev.luna5ama.trollhack.gui.hudgui.elements.misc

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.extension.rootName
import dev.luna5ama.trollhack.util.extension.synchronized
import dev.luna5ama.trollhack.util.threads.TimerScope
import dev.luna5ama.trollhack.util.threads.runSynchronized
import kotlin.math.roundToInt

internal object MemoryUsage : LabelHud(
    name = "MemoryUsage",
    category = Category.MISC,
    description = "Display the used, allocated and max memory"
) {
    private val showAllocated by setting("Show Allocated", false)
    private val showMax by setting("Show Max", false)
    private val showAllocations by setting("Show Allocations", false)

    private const val BYTE_TO_MB = 1048576L
    private const val BYTE_TO_MB_D = 1048576.0
    private val allocations = ArrayList<Pair<Long, Double>>().synchronized()
    private var lastUsed = getUsed()
    private var lastUpdate = System.nanoTime()

    init {
        TimerScope.launchLooping(rootName, 5L) {
            if (visible && showAllocations) {
                val last = lastUsed
                val lastTime = lastUpdate
                val current = getUsed()
                val currentTime = System.nanoTime()

                val diff = current - last
                if (diff > 0L) {
                    val adjustFactor = (currentTime - lastTime).toDouble() / 5_000_000.0
                    allocations.add(currentTime + 3_000_000_000L to (diff * adjustFactor))
                }

                lastUsed = current
                lastUpdate = currentTime
            }
        }
    }

    override fun SafeClientEvent.updateText() {
        displayText.add(getUsedMB().toString(), GuiSetting.text)

        if (showAllocations) {
            val current = System.nanoTime()
            val allocation = allocations.runSynchronized {
                removeIf {
                    it.first <= current
                }
                allocations.sumOf {
                    it.second / 3.0 / BYTE_TO_MB_D
                }
            }
            displayText.add("(${allocation.roundToInt()} MB/s)", GuiSetting.text)
        }
        if (showAllocated) {
            val allocatedMemory = Runtime.getRuntime().totalMemory() / BYTE_TO_MB
            displayText.add(allocatedMemory.toString(), GuiSetting.text)
        }
        if (showMax) {
            val maxMemory = Runtime.getRuntime().maxMemory() / BYTE_TO_MB
            displayText.add(maxMemory.toString(), GuiSetting.text)
        }

        displayText.add("MB", GuiSetting.primary)
    }

    private fun getUsedMB(): Int {
        return (getUsed() / 1048576L).toInt()
    }

    private fun getUsed(): Long {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    }
}