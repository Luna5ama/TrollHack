package dev.luna5ama.trollhack.gui.hudgui.elements.misc

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import java.util.*
import kotlin.math.max

internal object MemoryUsage : LabelHud(
    name = "Memory Usage",
    category = Category.MISC,
    description = "Display the used, allocated and max memory"
) {
    private val showAllocated by setting("Show Allocated", false)
    private val showMax by setting("Show Max", false)
    private val showAllocations by setting("Show Allocations", false)

    private const val BYTE_TO_MB = 1048576L
    private val allocations = ArrayDeque<AllocationRecord>()
    private var lastUsed = getUsed()

    init {
        listener<RunGameLoopEvent.Start> {
            if (!showAllocations) {
                allocations.clear()
            }

            val current = getUsed()

            if (lastUsed != 0L) {
                val diff = current - lastUsed
                if (diff > 0L) {
                    allocations.add(AllocationRecord(System.nanoTime() + 3_000_000_000L, diff.toFloat() / 1024.0f))
                }
            }

            lastUsed = current
        }
    }

    override fun SafeClientEvent.updateText() {
        displayText.add(getUsedMB().toString(), GuiSetting.text)

        if (showAllocations) {
            displayText.add(getAllocationText(), GuiSetting.text)
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

    private fun getAllocationText(): String {
        val current = System.nanoTime()
        while (allocations.isNotEmpty() && allocations.peek().time < current) {
            allocations.poll()
        }

        if (allocations.isEmpty()) {
            return "(0.0 MB/s)"
        }

        var allocation = 0.0f
        for (entry in allocations) {
            allocation += entry.allocation
        }
        val timeLength = (allocations.last.time - allocations.first.time) / 1_000_000_000.0f

        return "(%.2f MB/s)".format(allocation / max(timeLength, 0.1f) / 1024.0f)
    }


    private fun getUsedMB(): Int {
        return (getUsed() / 1048576L).toInt()
    }

    private fun getUsed(): Long {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    }

    private class AllocationRecord(val time: Long, val allocation: Float)
}