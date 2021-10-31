package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.TpsCalculator
import net.minecraft.network.play.client.CPacketAnimation

internal object SwingLimiter : Module(
    name = "SwingLimiter",
    category = Category.PLAYER,
    description = "Limits swing packet",
    modulePriority = 2000
) {
    private val ticks by setting("Ticks", 10, 0..40, 1)
    private val tpsSync by setting("Tps Sync", true)

    private val swingTimer = TickTimer()

    init {
        listener<PacketEvent.Send>(-9999) {
            if (it.packet is CPacketAnimation) {
                if (checkSwingDelay()) {
                    swingTimer.reset()
                } else {
                    it.cancel()
                }
            }
        }
    }

    @JvmStatic
    fun checkSwingDelay(): Boolean {
        return swingTimer.tick(
            if (tpsSync) (ticks * TpsCalculator.multiplier * 50.0f).toLong()
            else ticks * 50L
        )
    }
}