package dev.luna5ama.trollhack.modules.impl.visual.valkyrie

import dev.luna5ama.trollhack.RS
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.CoreRender2DEvent
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.matrix.scope
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.components.*
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.compat.armorStacksCompat
import net.minecraft.world.item.Items
import kotlin.math.absoluteValue

object Valkyrie : Module("Valkyrie", category = Category.VISUAL) {
    private val onElytraOnly by setting("Elytra Only", false)
    val reverseRoll by setting("Reverse Roll", false)
    val degreesPerBar by setting("Degrees Per Bar", 15)
    private val rollTurningForce0 by setting("Roll Turning Force", 1.25f)
    val rollTurningForce get() = rollTurningForce0.absoluteValue
    private val rollSmoothing0 by setting("Roll Smoothing", 0.85f)
    val rollSmoothing get() = rollSmoothing0.absoluteValue
    val optimumGlideAngle by setting("Optimum Glide Angle", -2)
    val optimumClimbAngle by setting("Optimum Climb Angle", 55)
//    private val pitchLength0 by setting("Pitch Length", 30.0)
//    private val pitchLength get() = pitchLength0.absoluteValue
//    private val pitchDashedGap0 by setting("Pitch Dashed Gap", 3.0)
//    private val pitchDashedGap get() = pitchDashedGap0.absoluteValue
//    private val halfGap0 by setting("Half Gap", 20.0)
//    private val halfGap get() = halfGap0.absoluteValue
//    private val inflectionLength0 by setting("Infl Length", 5.0)
//    private val inflectionLength get() = inflectionLength0.absoluteValue
//    private val linearPitchLabel by setting("Linear Pitch Label", true)
//    private val angleMultiplier by setting("Angle Multiplier", 6.0, { linearPitchLabel })
    private val lineWidth by setting("Line Width", 3.0f)
    val halfThickness get() = lineWidth / 2
    val color by setting("Color", ColorRGBA.GREEN)

//    private val pitchLabels = (-90..90 step 15).toList()
    private val dim = Dimensions()
    private val computer = FlightComputer()

    private val components = arrayOf(
        FlightPathIndicator(computer, dim), LocationIndicator(dim),
        HeadingIndicator(computer, dim), SpeedIndicator(computer, dim),
        AltitudeIndicator(computer, dim), PitchIndicator(computer, dim),
        ElytraHealthIndicator(computer, dim)
    )

    init {
        nonNullHandler<CoreRender2DEvent> {
            if (onElytraOnly && (player.onGround() || !player.isFallFlying || player.armorStacksCompat.none { it.item == Items.ELYTRA }))
                return@nonNullHandler
            computer.update(it.ticksDelta)
            dim.update(mc)

            RS.matrixLayer.scope {
                for (component in components) {
                    component.render(it.ticksDelta)
                }
            }
//            drawPitchLabel()
//            UnicodeFontManager.CURRENT_FONT.drawStringWithShadow("${player.getSpeedKpH()}", ColorRGBA.Companion.WHITE)
        }
    }

    context(ctx: NonNullContext)
    private fun drawPitchLabel(): Unit = ctx.run {
//        val font = UnicodeFontManager.MSYAHEI_12
//        val centerOfScreen = Vec2d(RS.scaledWidth / 2, RS.scaledHeight / 2)
//        val currentPitch = mc.gameRenderer.camera.pitch
//        val length = RS.scaledHeight / 1.5
//        pitchLabels.forEach { angle ->
//            val yDelta: Double
//            if (!linearPitchLabel) {
//                if ((angle - currentPitch).absoluteValue > 90) return@forEach
//                yDelta = sin((angle - currentPitch).toRadian()).toDouble() * RS.scaledHeight / 2
//                if (yDelta.absoluteValue > length) return@forEach
//            } else {
//                yDelta = -(currentPitch + angle) * angleMultiplier
//                if (yDelta.absoluteValue > centerOfScreen.y) {
//                    return@forEach
//                }
//            }
//
//            if (angle == 0) {
//                Render2DUtils.drawLine(
//                    Vec2d(0.0, centerOfScreen.y + yDelta), Vec2d(centerOfScreen.x - 10.0, centerOfScreen.y + yDelta),
//                    width = lineWidth, color1 = ColorRGBA.Companion.GREEN
//                )
//                Render2DUtils.drawLine(
//                    Vec2d(centerOfScreen.x + 10.0, centerOfScreen.y + yDelta),
//                    Vec2d(RS.scaledWidth, centerOfScreen.y + yDelta),
//                    width = lineWidth, color1 = ColorRGBA.Companion.GREEN
//                )
//            } else if (angle > 0) {
//                Render2DUtils.drawLinesStrip(
//                    arrayOf(
//                        centerOfScreen.plus(-halfGap, yDelta),
//                        centerOfScreen.plus(-(halfGap + pitchLength), yDelta),
//                        centerOfScreen.plus(-(halfGap + pitchLength), yDelta + inflectionLength)
//                    ), width = lineWidth, color = ColorRGBA.Companion.GREEN
//                )
//
//                Render2DUtils.drawLinesStrip(
//                    arrayOf(
//                        centerOfScreen.plus((halfGap + pitchLength), yDelta + inflectionLength),
//                        centerOfScreen.plus((halfGap + pitchLength), yDelta),
//                        centerOfScreen.plus(halfGap, yDelta)
//                    ), width = lineWidth, color = ColorRGBA.Companion.GREEN
//                )
//
//                val labelText = angle.toString()
//                val labelWidth = font.getWidth(labelText)
//                val labelHeight = font.height
//                font.drawText(
//                    labelText,
//                    centerOfScreen.x - (halfGap + pitchLength) - labelWidth - 3,
//                    centerOfScreen.y + yDelta - labelHeight / 2,
//                    ColorRGBA.Companion.GREEN
//                )
//                font.drawText(
//                    labelText,
//                    centerOfScreen.x + (halfGap + pitchLength) + 3,
//                    centerOfScreen.y + yDelta - labelHeight / 2,
//                    ColorRGBA.Companion.GREEN
//                )
//            } else {
//                val oneThirdOfLength = (pitchLength - pitchDashedGap * 2) / 3
//
//                Render2DUtils.drawLinesStrip(
//                    arrayOf(
//                        centerOfScreen.plus(-(halfGap + 3 * oneThirdOfLength + 2 * pitchDashedGap), yDelta - inflectionLength),
//                        centerOfScreen.plus(-(halfGap + 3 * oneThirdOfLength + 2 * pitchDashedGap), yDelta),
//                        centerOfScreen.plus(-(halfGap + 2 * oneThirdOfLength + 2 * pitchDashedGap), yDelta)
//                    ), width = lineWidth, color = ColorRGBA.Companion.GREEN
//                )
//                Render2DUtils.drawLine(
//                    centerOfScreen.plus(-(halfGap + 2 * oneThirdOfLength + pitchDashedGap), yDelta),
//                    centerOfScreen.plus(-(halfGap + oneThirdOfLength + pitchDashedGap), yDelta),
//                    width = lineWidth, color1 = ColorRGBA.Companion.GREEN
//                )
//                Render2DUtils.drawLine(
//                    centerOfScreen.plus(-(halfGap + oneThirdOfLength), yDelta),
//                    centerOfScreen.plus(-halfGap, yDelta),
//                    width = lineWidth, color1 = ColorRGBA.Companion.GREEN
//                )
//
//                Render2DUtils.drawLine(
//                    centerOfScreen.plus(halfGap, yDelta),
//                    centerOfScreen.plus(halfGap + oneThirdOfLength, yDelta),
//                    width = lineWidth, color1 = ColorRGBA.Companion.GREEN
//                )
//                Render2DUtils.drawLine(
//                    centerOfScreen.plus(halfGap + oneThirdOfLength + pitchDashedGap, yDelta),
//                    centerOfScreen.plus(halfGap + 2 * oneThirdOfLength + pitchDashedGap, yDelta),
//                    width = lineWidth, color1 = ColorRGBA.Companion.GREEN
//                )
//                Render2DUtils.drawLinesStrip(
//                    arrayOf(
//                        centerOfScreen.plus(halfGap + 2 * oneThirdOfLength + 2 * pitchDashedGap, yDelta),
//                        centerOfScreen.plus(halfGap + 3 * oneThirdOfLength + 2 * pitchDashedGap, yDelta),
//                        centerOfScreen.plus(halfGap + 3 * oneThirdOfLength + 2 * pitchDashedGap, yDelta - inflectionLength)
//                    ), width = lineWidth, color = ColorRGBA.Companion.GREEN
//                )
//
//                val labelText = angle.toString()
//                val labelWidth = font.getWidth(labelText)
//                val labelHeight = font.height
//                font.drawText(
//                    labelText,
//                    centerOfScreen.x - (halfGap + pitchLength) - labelWidth - 3,
//                    centerOfScreen.y + yDelta - labelHeight / 2,
//                    ColorRGBA.Companion.GREEN
//                )
//                font.drawText(
//                    labelText,
//                    centerOfScreen.x + (halfGap + pitchLength) + 3,
//                    centerOfScreen.y + yDelta - labelHeight / 2,
//                    ColorRGBA.Companion.GREEN
//                )
//            }
//        }
    }
}
