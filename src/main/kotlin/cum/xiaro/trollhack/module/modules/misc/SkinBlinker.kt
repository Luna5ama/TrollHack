package cum.xiaro.trollhack.module.modules.misc

import cum.xiaro.trollhack.util.interfaces.DisplayEnum
import cum.xiaro.trollhack.event.events.RunGameLoopEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.TickTimer
import net.minecraft.entity.player.EnumPlayerModelParts
import java.util.*

internal object SkinBlinker : Module(
    name = "SkinBlinker",
    description = "Toggle your skin layers rapidly for a cool skin effect",
    category = Category.MISC
) {
    private val cape = setting("Cape", false)
    private val jacket = setting("Jacket", true)
    private val leftSleeve = setting("Left Sleeve", true)
    private val rightSleeve = setting("Right Sleeve", true)
    private val leftPantsLeg = setting("Left Pants Leg", true)
    private val rightPantsLeg = setting("Right Pants Leg", true)
    private val hat = setting("Hat", true)
    private val mode by setting("Mode", FlickerMode.HORIZONTAL)
    private val delay by setting("Delay", 10, 0..500, 5, description = "Skin layer toggle delay, in milliseconds")

    private enum class FlickerMode(override val displayName: CharSequence) : DisplayEnum {
        HORIZONTAL("Horizontal"),
        VERTICAL("Vertical"),
        RANDOM("Random")
    }

    private val enabledParts = EnumSet.of(
        EnumPlayerModelParts.JACKET,
        EnumPlayerModelParts.LEFT_SLEEVE,
        EnumPlayerModelParts.RIGHT_SLEEVE,
        EnumPlayerModelParts.LEFT_PANTS_LEG,
        EnumPlayerModelParts.RIGHT_PANTS_LEG,
        EnumPlayerModelParts.HAT
    )

    private val horizontalParts = arrayOf(
        EnumPlayerModelParts.LEFT_SLEEVE,
        EnumPlayerModelParts.LEFT_PANTS_LEG,
        EnumPlayerModelParts.JACKET,
        EnumPlayerModelParts.HAT,
        EnumPlayerModelParts.CAPE,
        EnumPlayerModelParts.RIGHT_PANTS_LEG,
        EnumPlayerModelParts.RIGHT_SLEEVE
    )
    private val verticalParts = arrayOf(
        EnumPlayerModelParts.HAT,
        EnumPlayerModelParts.JACKET,
        EnumPlayerModelParts.CAPE,
        EnumPlayerModelParts.LEFT_SLEEVE,
        EnumPlayerModelParts.RIGHT_SLEEVE,
        EnumPlayerModelParts.LEFT_PANTS_LEG,
        EnumPlayerModelParts.RIGHT_PANTS_LEG
    )

    private val timer = TickTimer()
    private var lastIndex = 0

    override fun getHudInfo(): String {
        return mode.displayString
    }

    init {
        safeListener<RunGameLoopEvent.Tick> {
            if (!timer.tickAndReset(delay.toLong())) return@safeListener

            val part = when (mode) {
                FlickerMode.RANDOM -> EnumPlayerModelParts.values().random()
                FlickerMode.VERTICAL -> verticalParts[lastIndex]
                FlickerMode.HORIZONTAL -> horizontalParts[lastIndex]
            }

            if (enabledParts.contains(part)) {
                mc.gameSettings.switchModelPartEnabled(part)
            }
            lastIndex = (lastIndex + 1) % 7
        }

        onDisable {
            for (model in EnumPlayerModelParts.values()) {
                mc.gameSettings.setModelPartEnabled(model, true)
            }
        }

        cape.valueListeners.add { _, it ->
            if (it) enabledParts.add(EnumPlayerModelParts.CAPE)
            else enabledParts.remove(EnumPlayerModelParts.CAPE)
        }

        jacket.valueListeners.add { _, it ->
            if (it) enabledParts.add(EnumPlayerModelParts.JACKET)
            else enabledParts.remove(EnumPlayerModelParts.JACKET)
        }

        leftSleeve.valueListeners.add { _, it ->
            if (it) enabledParts.add(EnumPlayerModelParts.LEFT_SLEEVE)
            else enabledParts.remove(EnumPlayerModelParts.LEFT_SLEEVE)
        }

        rightSleeve.valueListeners.add { _, it ->
            if (it) enabledParts.add(EnumPlayerModelParts.RIGHT_SLEEVE)
            else enabledParts.remove(EnumPlayerModelParts.RIGHT_SLEEVE)
        }

        leftPantsLeg.valueListeners.add { _, it ->
            if (it) enabledParts.add(EnumPlayerModelParts.LEFT_PANTS_LEG)
            else enabledParts.remove(EnumPlayerModelParts.LEFT_PANTS_LEG)
        }

        rightPantsLeg.valueListeners.add { _, it ->
            if (it) enabledParts.add(EnumPlayerModelParts.RIGHT_PANTS_LEG)
            else enabledParts.remove(EnumPlayerModelParts.RIGHT_PANTS_LEG)
        }

        hat.valueListeners.add { _, it ->
            if (it) enabledParts.add(EnumPlayerModelParts.HAT)
            else enabledParts.remove(EnumPlayerModelParts.HAT)
        }
    }
}