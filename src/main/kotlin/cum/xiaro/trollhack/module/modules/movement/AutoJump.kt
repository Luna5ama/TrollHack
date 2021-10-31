package cum.xiaro.trollhack.module.modules.movement

import cum.xiaro.trollhack.event.events.player.PlayerMoveEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.TimeUnit

internal object AutoJump : Module(
    name = "AutoJump",
    category = Category.MOVEMENT,
    description = "Automatically jumps if possible"
) {
    private val delay = setting("Tick Delay", 10, 0..40, 1)

    private val timer = TickTimer(TimeUnit.TICKS)

    init {
        safeListener<PlayerMoveEvent.Pre> {
            if (player.isInWater || player.isInLava) player.motionY = 0.1
            else if (player.onGround && timer.tickAndReset(delay.value)) player.jump()
        }
    }
}