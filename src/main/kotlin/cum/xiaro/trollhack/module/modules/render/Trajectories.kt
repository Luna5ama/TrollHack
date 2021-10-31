package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.util.extension.toRadian
import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.render.Render3DEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.module.modules.combat.MidClickPearl
import cum.xiaro.trollhack.module.modules.player.FastUse
import cum.xiaro.trollhack.util.EntityUtils
import cum.xiaro.trollhack.util.graphics.ESPRenderer
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.graphics.RenderUtils3D
import cum.xiaro.trollhack.util.graphics.mask.EnumFacingMask
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.ActiveRenderInfo
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHandSide
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.GL_LINE_STRIP
import org.lwjgl.opengl.GL11.glLineWidth
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal object Trajectories : Module(
    name = "Trajectories",
    category = Category.RENDER,
    description = "Draws lines to where trajectories are going to fall"
) {
    private val showEntity = setting("Show Entity", true)
    private val showBlock = setting("Show Block", true)
    private val r = setting("Red", 255, 0..255, 1)
    private val g = setting("Green", 255, 0..255, 1)
    private val b = setting("Blue", 255, 0..255, 1)
    private val aFilled = setting("Filled Alpha", 127, 0..255, 1)
    private val aOutline = setting("Outline Alpha", 255, 0..255, 1)
    private val width = setting("Width", 2f, 0.25f..5f, 0.25f)

    private var prevItemUseCount = 0

    init {
        safeListener<TickEvent.Pre> {
            prevItemUseCount = player.itemInUseCount
        }

        safeListener<Render3DEvent> {
            val type = getMidClickPearl()
                ?: getThrowingType(player.heldItemMainhand)
                ?: getThrowingType(player.heldItemOffhand)
                ?: return@safeListener

            val path = ArrayList<Vec3d>()
            val flightPath = FlightPath(type)
            path.add(flightPath.position)
            while (flightPath.collision == null && path.size < 500) {
                flightPath.simulateTick()
                path.add(flightPath.position)
            }

            val offset = getPathOffset()
            val color = ColorRGB(r.value, g.value, b.value, aOutline.value)

            glLineWidth(width.value)
            GlStateUtils.depth(false)

            for ((index, pos) in path.withIndex()) {
                val scale = ((path.size - 1) - index) * (1.0 / (path.size - 1))
                val offsetPos = pos.add(offset.scale(scale))
                RenderUtils3D.putVertex(offsetPos.x, offsetPos.y, offsetPos.z, color)
            }

            RenderUtils3D.draw(GL_LINE_STRIP)

            flightPath.collision?.let {
                val box = (when (it.sideHit.axis) {
                    EnumFacing.Axis.X -> AxisAlignedBB(0.0, -0.25, -0.25, 0.0, 0.25, 0.25)
                    EnumFacing.Axis.Y -> AxisAlignedBB(-0.25, 0.0, -0.25, 0.25, 0.0, 0.25)
                    else -> AxisAlignedBB(-0.25, -0.25, 0.0, 0.25, 0.25, 0.0)
                }).offset(it.hitVec)

                val colorBox = ColorRGB(r.value, g.value, b.value)
                val quadSide = EnumFacingMask.getMaskForSide(it.sideHit)
                val renderer = ESPRenderer()
                renderer.aFilled = aFilled.value
                renderer.aOutline = aOutline.value
                renderer.thickness = width.value
                renderer.add(box, colorBox, quadSide)
                renderer.render(true)

                renderer.aFilled = 0
                if (showEntity.value && it.entityHit != null) renderer.add(it.entityHit, colorBox)
                else if (showBlock.value) renderer.add(it.blockPos, colorBox)
                renderer.render(true)
            }

            glLineWidth(1f)
            GlStateUtils.depth(true)
        }
    }

    private fun SafeClientEvent.getPathOffset(): Vec3d {
        if (mc.gameSettings.thirdPersonView != 0) return Vec3d.ZERO
        var multiplier = if (getThrowingType(player.heldItemOffhand) != null) -1.0 else 1.0
        if (mc.gameSettings.mainHand != EnumHandSide.RIGHT) multiplier *= -1.0

        val eyePos = player.getPositionEyes(RenderUtils3D.partialTicks)
        val camPos = EntityUtils.getInterpolatedPos(player, RenderUtils3D.partialTicks).add(ActiveRenderInfo.getCameraPosition())

        val yawRad = player.rotationYaw.toRadian()
        val pitchRad = player.rotationPitch.toRadian()
        val offset = Vec3d(cos(yawRad) * 0.2 + sin(pitchRad) * -sin(yawRad) * 0.15, 0.0, sin(yawRad) * 0.2 + sin(pitchRad) * cos(yawRad) * 0.15)
        return camPos.subtract(offset.scale(multiplier).add(0.0, cos(pitchRad) * 0.1, 0.0)).subtract(eyePos)
    }

    private class FlightPath(val throwingType: ThrowingType) {
        private val halfSize = if (throwingType == ThrowingType.BOW) 0.25 else 0.125

        var position: Vec3d = mc.player.getPositionEyes(RenderUtils3D.partialTicks)
            private set
        private var motion: Vec3d
        private var boundingBox: AxisAlignedBB = AxisAlignedBB(position.x - halfSize, position.y - halfSize, position.z - halfSize, position.x + halfSize, position.y + halfSize, position.z + halfSize)
        var collision: RayTraceResult? = null
            private set

        fun simulateTick() {
            if (position.y <= -9.11) { // Sanity check to see if we've gone below the world (if we have we will never collide)
                collision = RayTraceResult(position, EnumFacing.UP)
                return
            }

            val nextPos = position.add(motion) // Get the next positions in the world
            collision = mc.world.rayTraceBlocks(position, nextPos, false, true, false) // Check if we've collided with a block

            if (collision == null) {
                val resultList = ArrayList<RayTraceResult>()
                for (entity in mc.world.loadedEntityList) {
                    if (!entity.canBeCollidedWith()) continue
                    if (entity == mc.player) continue
                    val box = entity.entityBoundingBox.grow(0.30000001192092896) ?: continue
                    val rayTraceResult = box.calculateIntercept(position, nextPos) ?: continue
                    rayTraceResult.entityHit = entity
                    resultList.add(rayTraceResult)
                }
                collision = resultList.minByOrNull { it.hitVec.distanceTo(position) }
            }

            collision?.let {
                setPosition(it.hitVec) // Update position
                return
            }

            val motionModifier = if (mc.player.entityWorld.isMaterialInBB(boundingBox, Material.WATER)) if (throwingType == ThrowingType.BOW) 0.6 else 0.8
            else 0.99

            setPosition(nextPos) // Update the position and bounding box

            motion = motion.scale(motionModifier) // Slowly decay the velocity of the path
            motion = motion.subtract(0.0, throwingType.gravity, 0.0) // Drop the motionY by the constant gravity

        }

        private fun setPosition(posIn: Vec3d) {
            boundingBox = boundingBox.offset(posIn.subtract(position))
            position = posIn
        }

        private fun getInterpolatedCharge() = prevItemUseCount.toDouble() + (mc.player.itemInUseCount.toDouble() - prevItemUseCount.toDouble()) * RenderUtils3D.partialTicks.toDouble()

        init {
            var pitch = mc.player.rotationPitch.toDouble()
            if (throwingType == ThrowingType.EXPERIENCE || throwingType == ThrowingType.POTION) pitch -= 20.0
            val yawRad = Math.toRadians(mc.player.rotationYaw.toDouble())
            val pitchRad = Math.toRadians(pitch)
            val cosPitch = cos(pitchRad)

            val initVelocity = if (throwingType == ThrowingType.BOW) {
                val itemUseCount = FastUse.bowCharge ?: if (mc.player.isHandActive) getInterpolatedCharge() else 0.0
                val useDuration = (72000 - itemUseCount) / 20.0
                val velocity = (useDuration.pow(2) + useDuration * 2.0) / 3.0
                min(velocity, 1.0) * throwingType.velocity
            } else throwingType.velocity

            motion = Vec3d(-sin(yawRad) * cosPitch, -sin(pitchRad), cos(yawRad) * cosPitch).scale(initVelocity)
        }
    }

    private fun getMidClickPearl(): ThrowingType? {
        return if (MidClickPearl.isActive() && Mouse.isButtonDown(2)) {
            ThrowingType.OTHER
        } else {
            null
        }
    }

    private fun getThrowingType(itemStack: ItemStack): ThrowingType? = when (itemStack.item) {
        Items.BOW -> ThrowingType.BOW
        Items.EXPERIENCE_BOTTLE -> ThrowingType.EXPERIENCE
        Items.SPLASH_POTION, Items.LINGERING_POTION -> ThrowingType.POTION
        Items.SNOWBALL, Items.EGG, Items.ENDER_PEARL -> ThrowingType.OTHER
        else -> null
    }

    enum class ThrowingType(val gravity: Double, val velocity: Double) {
        BOW(0.05000000074505806, 3.0),
        EXPERIENCE(0.07, 0.7),
        POTION(0.05, 0.5),
        OTHER(0.03, 1.5)
    }
}