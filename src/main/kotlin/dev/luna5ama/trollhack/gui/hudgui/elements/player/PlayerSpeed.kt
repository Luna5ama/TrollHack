package dev.luna5ama.trollhack.gui.hudgui.elements.player

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.manager.managers.TimerManager
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.MovementUtils.realSpeed
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.math.MathUtils
import java.util.*

internal object PlayerSpeed : LabelHud(
    name = "Player Speed",
    category = Category.PLAYER,
    description = "Player movement speed"
) {
    private val speedUnit by setting("Speed Unit", SpeedUnit.MPS)
    private val averageSpeedTime by setting("Average Speed Ticks", 10, 1..50, 1)
    private val applyTimer by setting("Apply Timer", true)

    @Suppress("UNUSED")
    private enum class SpeedUnit(override val displayName: CharSequence, val multiplier: Double) : DisplayEnum {
        MPS("m/s", 1.0),
        KMH("km/h", 3.6)
    }

    private val speedList = ArrayDeque<Double>()

    override fun SafeClientEvent.updateText() {
        updateSpeedList()

        var averageSpeed = if (speedList.isEmpty()) 0.0 else speedList.sum() / speedList.size

        averageSpeed *= speedUnit.multiplier
        averageSpeed = MathUtils.round(averageSpeed, 2)

        displayText.add("%.2f".format(averageSpeed), GuiSetting.text)
        displayText.add(speedUnit.displayString, GuiSetting.primary)
    }

    private fun SafeClientEvent.updateSpeedList() {
        val tps = if (applyTimer) 1000.0 / TimerManager.tickLength else 20.0
        val speed = player.realSpeed * tps

        if (speed > 0.0 || player.ticksExisted % 4 == 0) {
            speedList.add(speed) // Only adding it every 4 ticks if speed is 0
        } else {
            speedList.pollFirst()
        }

        while (speedList.size > averageSpeedTime) speedList.pollFirst()
    }

}