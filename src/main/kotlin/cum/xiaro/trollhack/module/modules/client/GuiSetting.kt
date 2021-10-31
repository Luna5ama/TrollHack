package cum.xiaro.trollhack.module.modules.client

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeParallelListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.delegate.FrameValue
import kotlin.math.round

internal object GuiSetting : Module(
    name = "GuiSetting",
    description = "GUI ",
    visible = false,
    category = Category.CLIENT,
    alwaysEnabled = true
) {
    private val scaleSetting = setting("Scale", 100, 50..400, 5)
    val particle by setting("Particle", true)
    val blur by setting("Blur", 0.25f, 0.0f..1.0f, 0.05f)
    val windowOutline by setting("Window Outline", false)
    val titleBar by setting("Title Bar", true)
    private val windowBlur0 = setting("Window Blur", true)
    val windowBlur by windowBlur0
    val darkness by setting("Darkness", 0.25f, 0.0f..1.0f, 0.05f)
    val fadeInTime by setting("Fade In Time", 0.3f, 0.0f..1.0f, 0.05f)
    val fadeOutTime by setting("Fade Out Time", 0.2f, 0.0f..1.0f, 0.05f)
    private val primarySetting by setting("Primary Color", ColorRGB(255, 160, 240, 220))
    private val outlineSetting by setting("Outline Color", ColorRGB(240, 250, 255, 48))
    private val backgroundSetting by setting("Background Color", ColorRGB(36, 40, 48, 100))
    private val textSetting by setting("Text Color", ColorRGB(255, 250, 253, 255))
    private val aHover by setting("Hover Alpha", 32, 0..255, 1)

    val primary get() = primarySetting
    val idle get() = if (primary.lightness < 0.9f) ColorRGB(255, 255, 255, 0) else ColorRGB(0, 0, 0, 0)
    val hover get() = idle.alpha(aHover)
    val click get() = idle.alpha(aHover * 2)
    val backGround get() = backgroundSetting
    val outline get() = outlineSetting
    val text get() = textSetting

    private var prevScale = scaleSetting.value / 100.0f
    private var scale = prevScale
    private val settingTimer = TickTimer()

    fun resetScale() {
        scaleSetting.value = 100
        prevScale = 1.0f
        scale = 1.0f
    }

    val scaleFactorFloat by FrameValue { (prevScale + (scale - prevScale) * mc.renderPartialTicks) * 2.0f }
    val scaleFactor by FrameValue { (prevScale + (scale - prevScale) * mc.renderPartialTicks) * 2.0 }

    private fun getRoundedScale(): Float {
        return round((scaleSetting.value / 100.0f) / 0.1f) * 0.1f
    }

    init {
        safeParallelListener<TickEvent.Post> {
            prevScale = scale
            if (settingTimer.tick(500L)) {
                val diff = scale - getRoundedScale()
                when {
                    diff < -0.025 -> scale += 0.025f
                    diff > 0.025 -> scale -= 0.025f
                    else -> scale = getRoundedScale()
                }
            }
        }

        scaleSetting.listeners.add {
            settingTimer.reset()
        }
    }
}