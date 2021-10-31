package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.BOOLEAN_SUPPLIER_FALSE
import cum.xiaro.trollhack.util.TickTimer
import kotlin.math.max
import kotlin.math.min

internal object FullBright : Module(
    name = "FullBright",
    description = "Makes everything brighter!",
    category = Category.RENDER,
    alwaysListening = true
) {
    private val gamma by setting("Gamma", 12.0f, 5.0f..15.0f, 0.5f)
    private val transitionLength by setting("Transition Length", 3.0f, 0.0f..10.0f, 0.5f)
    private var oldValue by setting("Old Value", 1.0f, 0.0f..1.0f, 0.1f, BOOLEAN_SUPPLIER_FALSE)

    private var gammaSetting: Float
        get() = mc.gameSettings.gammaSetting
        set(gammaIn) {
            mc.gameSettings.gammaSetting = gammaIn
        }
    private val disableTimer = TickTimer()

    override fun getHudInfo(): String {
        return "%.2f".format(gamma)
    }

    init {
        onEnable {
            oldValue = mc.gameSettings.gammaSetting
        }

        onDisable {
            disableTimer.reset()
        }

        safeListener<TickEvent.Post> {
            when {
                isEnabled -> {
                    transition(gamma)
                    alwaysListening = true
                }

                isDisabled && gammaSetting != oldValue
                    && !disableTimer.tick((transitionLength * 1000.0f).toLong()) -> {
                    transition(oldValue)
                }

                else -> {
                    alwaysListening = false
                    disable()
                }
            }
        }
    }

    private fun transition(target: Float) {
        gammaSetting = when {
            gammaSetting !in 0f..15f -> target

            gammaSetting == target -> return

            gammaSetting < target -> min(gammaSetting + getTransitionAmount(), target)

            else -> max(gammaSetting - getTransitionAmount(), target)
        }
    }

    private fun getTransitionAmount(): Float {
        if (transitionLength == 0f) return 15f
        return (1f / transitionLength / 20f) * (gamma - oldValue)
    }
}