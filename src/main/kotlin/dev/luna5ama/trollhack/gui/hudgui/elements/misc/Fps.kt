package dev.luna5ama.trollhack.gui.hudgui.elements.misc

import dev.fastmc.common.sort.IntComparator
import dev.fastmc.common.sort.IntIntrosort
import dev.fastmc.common.sort.LongComparator
import dev.fastmc.common.sort.LongIntrosort
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.longs.LongArrayList

internal object Fps : LabelHud(
    name = "Fps",
    category = Category.MISC,
    description = "Frame per second in game"
) {
    private val showAverage by setting("Show Average", true)
    private val showFrameTime by setting("Show Frame Time", false)
    private val showMin by setting("Show Min", false)
    private val show1Low by setting("Show 1% Low", false)
    private val show5Low by setting("Show 5% Low", false)
    private val shortWindow by setting("Short Window", 1.0f, 0.1f..3.0f, 0.1f)
    private val longWindow by setting("Long Window", 5.0f, 1.0f..15.0f, 0.1f)

    private var lastRender = System.nanoTime()

    private val shortFrameTimeList = ArrayList<FrameTimeRecord>()
    private val longFrameTimeList = ArrayList<FrameTimeRecord>()
    private val sorted = LongArrayList()

    init {
        listener<RunGameLoopEvent.End> {
            val current = System.nanoTime()

            shortFrameTimeList.removeIf {
                it.timeout <= current
            }

            longFrameTimeList.removeIf {
                it.timeout <= current
            }

            val frameTime = current - lastRender
            shortFrameTimeList.add(FrameTimeRecord(current + (shortWindow * 1_000_000_000L).toLong(), frameTime))
            longFrameTimeList.add(FrameTimeRecord(current + (longWindow * 1_000_000_000L).toLong(), frameTime))
            lastRender = current
        }
    }

    override fun SafeClientEvent.updateText() {
        val shortFrameTime = shortFrameTimeList.average()

        displayText.add("Fps", GuiSetting.primary)
        addFps(shortFrameTime)

        if (showAverage) {
            displayText.add("Avg.", GuiSetting.primary)
            addFps(longFrameTimeList.average())
        }

        if (showMin) {
            val minFrameTime = longFrameTimeList.maxByOrNull { it.frameTime }?.frameTime ?: 0
            displayText.add("Min", GuiSetting.primary)
            addFps(minFrameTime)
        }

        if (show1Low || show5Low) {
            if (longFrameTimeList.isNotEmpty()) {
                sorted.clear()
                sorted.ensureCapacity(longFrameTimeList.size)
                longFrameTimeList.forEach {
                    sorted.add(it.frameTime)
                }
                LongIntrosort.sort(sorted.elements(), 0, sorted.size, LongComparator.REVERSE_ORDER)
            }
            if (show1Low) {
                val time = if (sorted.isNotEmpty()) sorted.getLong(sorted.size / 100) else 0
                displayText.add("1% Low", GuiSetting.primary)
                addFps(time)
            }
            if (show5Low) {
                val time = if (sorted.isNotEmpty()) sorted.getLong(sorted.size / 20) else 0
                displayText.add("5% Low", GuiSetting.primary)
                addFps(time)
            }
        }
    }

    private fun addFps(time: Long) {
        displayText.add((1000_000_000.0 / time).toInt().toString(), GuiSetting.text)
        if (showFrameTime) {
            displayText.add("(%.2f ms)".format(time / 1_000_000.0), GuiSetting.text)
        }
    }

    private fun ArrayList<FrameTimeRecord>.average(): Long {
        if (isEmpty()) {
            return 0
        }
        val mul = 1.0 / size
        return sumOf {
            it.frameTime * mul
        }.toLong()
    }

    private class FrameTimeRecord(val timeout: Long, val frameTime: Long)
}