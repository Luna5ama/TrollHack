package dev.luna5ama.trollhack.modules.impl.visual.valkyrie

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.Skia2DEvent
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.components.AltitudeIndicator
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.components.ElytraHealthIndicator
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.components.FlightPathIndicator
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.components.HeadingIndicator
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.components.LocationIndicator
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.components.PitchIndicator
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.components.SpeedIndicator
import dev.luna5ama.trollhack.utils.compat.armorStacksCompat
import net.minecraft.world.item.Items
import kotlin.math.absoluteValue

object Valkyrie : Module("Valkyrie", category = Category.RENDER) {
    private val onElytraOnly by setting("Elytra Only", false)
    val reverseRoll by setting("Reverse Roll", false)
    val degreesPerBar by setting("Degrees Per Bar", 15)
    private val rollTurningForce0 by setting("Roll Turning Force", 1.25f)
    val rollTurningForce get() = rollTurningForce0.absoluteValue
    private val rollSmoothing0 by setting("Roll Smoothing", 0.85f)
    val rollSmoothing get() = rollSmoothing0.absoluteValue
    val optimumGlideAngle by setting("Optimum Glide Angle", -2)
    val optimumClimbAngle by setting("Optimum Climb Angle", 55)
    private val lineWidth by setting("Line Width", 3.0f)
    val halfThickness get() = lineWidth / 2
    val color by setting("Color", ColorRGBA.GREEN)

    private val dimensions = Dimensions()
    private val computer = FlightComputer()
    private val components = arrayOf(
        FlightPathIndicator(computer, dimensions),
        LocationIndicator(dimensions),
        HeadingIndicator(computer, dimensions),
        SpeedIndicator(computer, dimensions),
        AltitudeIndicator(computer, dimensions),
        PitchIndicator(computer, dimensions),
        ElytraHealthIndicator(computer, dimensions)
    )

    init {
        nonNullHandler<Skia2DEvent> { event ->
            if (onElytraOnly && (
                    player.onGround() ||
                        !player.isFallFlying ||
                        player.armorStacksCompat.none { it.item == Items.ELYTRA }
                    )
            ) return@nonNullHandler

            computer.update(event.ticksDelta)
            dimensions.update(mc)
            components.forEach { it.renderWith(event.draw, event.ticksDelta) }
        }
    }
}
