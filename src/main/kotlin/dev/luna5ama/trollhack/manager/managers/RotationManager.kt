package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.event.impl.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import dev.luna5ama.trollhack.manager.AbstractManager
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.rotation.Priority
import dev.luna5ama.trollhack.utils.rotation.RotationMath
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket
import net.minecraft.util.Mth
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

object RotationManager : AbstractManager(), AlwaysListening {
    private var rotations = Vec2f.ZERO
    private var lastRotations = Vec2f.ZERO
    private var targetRotations: Vec2f? = null
    private var offset = Vec2f.ZERO
    private var initialized = false
    private var smoothed = false
    private var rotationSpeed = 0.0
    private var raytrace: ((Vec2f) -> Boolean)? = null
    private var randomAngle = 0.0f
    private var priority = Priority.Lowest.priority

    var isActive = false
        private set

    val rotation: Vec2f
        get() = if (isActive) rotations else playerRotation()

    val yaw: Float
        get() = rotation.x

    val pitch: Float
        get() = rotation.y

    val lastRotation: Vec2f
        get() = if (initialized) lastRotations else playerRotation()

    val defaultSpeed: Double
        get() = ClientSettings.yawSpeedLimit.toDouble()

    init {
        handler<PacketEvent.PostReceive> {
            if (it.packet is ClientboundPlayerPositionPacket || it.packet is ClientboundPlayerRotationPacket) {
                reset(playerRotation())
            }
        }

        handler<WorldEvent.Unload> {
            reset(Vec2f.ZERO)
        }
    }

    @JvmOverloads
    fun setRotations(
        rotations: Vec2f?,
        rotationSpeed: Double = defaultSpeed,
        raytrace: ((Vec2f) -> Boolean)? = null,
        priority: Priority = Priority.Medium
    ): Boolean {
        if (rotations == null || !rotations.isFinite()) return false
        if (isActive && priority.priority < this.priority) return false

        initialize()
        this.targetRotations = Vec2f(rotations.x, rotations.y.coerceIn(-90.0f, 90.0f))
        this.rotationSpeed = rotationSpeed.coerceAtLeast(0.0)
        this.raytrace = raytrace
        this.priority = priority.priority
        this.isActive = true
        this.smoothed = false

        smooth()
        return true
    }

    fun applyRotation(event: OnUpdateWalkingPlayerEvent.Pre) {
        initialize()
        val target = targetRotations
        if (!isActive || target == null) {
            rotations = PlayerPacketManager.rotation
            lastRotations = rotations
            return
        }

        smooth()
        if (!event.applyRotation(rotations)) {
            smoothed = false
            return
        }

        val playerRotation = playerRotation()
        if (abs(Mth.wrapDegrees(rotations.x - playerRotation.x)) < 1.0f &&
            abs(rotations.y - playerRotation.y) < 1.0f
        ) {
            isActive = false
            priority = Priority.Lowest.priority
            correctDisabledRotations()
        }

        lastRotations = rotations
        targetRotations = playerRotation
        raytrace = null
        smoothed = false
    }

    fun reset() {
        reset(playerRotation())
    }

    private fun initialize() {
        if (initialized) return

        val rotation = playerRotation()
        rotations = rotation
        lastRotations = rotation
        targetRotations = rotation
        initialized = true
    }

    private fun reset(rotation: Vec2f) {
        rotations = rotation
        lastRotations = rotation
        targetRotations = rotation
        offset = Vec2f.ZERO
        smoothed = false
        rotationSpeed = 0.0
        raytrace = null
        randomAngle = 0.0f
        priority = Priority.Lowest.priority
        isActive = false
        initialized = rotation !== Vec2f.ZERO
    }

    private fun smooth() {
        if (smoothed) return

        val target = targetRotations ?: return
        var targetYaw = target.x
        var targetPitch = target.y
        val raytrace = raytrace

        if (raytrace != null &&
            (abs(Mth.wrapDegrees(targetYaw - rotations.x)) > 5.0f || abs(targetPitch - rotations.y) > 5.0f)
        ) {
            val trueTarget = target
            val speed = Math.random() * Math.random() * Math.random() * 20.0
            val direction = if (((mc.player?.tickCount ?: 0) / 10) % 2 == 0) -1.0 else 1.0
            randomAngle += ((20.0 + (Math.random() - 0.5) * Math.random() * Math.random() * Math.random() * 360.0) * direction).toFloat()

            offset = Vec2f(
                offset.x + (-sin(Math.toRadians(randomAngle.toDouble())) * speed).toFloat(),
                offset.y + (cos(Math.toRadians(randomAngle.toDouble())) * speed).toFloat()
            )
            targetYaw += offset.x
            targetPitch += offset.y

            if (!raytrace(Vec2f(targetYaw, targetPitch))) {
                randomAngle = Math.toDegrees(
                    kotlin.math.atan2(
                        (trueTarget.x - targetYaw).toDouble(),
                        (targetPitch - trueTarget.y).toDouble()
                    )
                ).toFloat() - 180.0f

                targetYaw -= offset.x
                targetPitch -= offset.y
                offset = Vec2f(
                    offset.x + (-sin(Math.toRadians(randomAngle.toDouble())) * speed).toFloat(),
                    offset.y + (cos(Math.toRadians(randomAngle.toDouble())) * speed).toFloat()
                )
                targetYaw += offset.x
                targetPitch += offset.y
            }

            if (!raytrace(Vec2f(targetYaw, targetPitch))) {
                offset = Vec2f.ZERO
                targetYaw = target.x + (Math.random() * 2.0).toFloat()
                targetPitch = target.y + (Math.random() * 2.0).toFloat()
            }
        }

        val stepped = RotationMath.step(
            lastRotations.x,
            lastRotations.y,
            targetYaw,
            targetPitch.coerceIn(-90.0f, 90.0f),
            rotationSpeed.toFloat(),
            sensitivityStep()
        )
        rotations = Vec2f(stepped[0], stepped[1])
        smoothed = true
    }

    private fun applySensitivityPatch(rotation: Vec2f, previous: Vec2f): Vec2f {
        val multiplier = sensitivityStep()
        if (multiplier <= 0.0f) return rotation

        val yaw = previous.x + round((rotation.x - previous.x) / multiplier) * multiplier
        val pitch = previous.y + round((rotation.y - previous.y) / multiplier) * multiplier
        return Vec2f(yaw, pitch.coerceIn(-90.0f, 90.0f))
    }

    private fun sensitivityStep(): Float {
        val sensitivity = (mc.options.sensitivity().get() * 0.6 + 0.2).toFloat()
        return sensitivity * sensitivity * sensitivity * 8.0f * 0.15f
    }

    private fun correctDisabledRotations() {
        val player = mc.player ?: return
        val current = Vec2f(player)
        val fixed = applySensitivityPatch(current, lastRotations)
        player.yRot = fixed.x + Mth.wrapDegrees(player.yRot - fixed.x)
        player.xRot = player.xRot.coerceIn(-90.0f, 90.0f)
    }

    private fun playerRotation(): Vec2f {
        return mc.player?.let(::Vec2f) ?: PlayerPacketManager.rotation
    }

    private fun Vec2f.isFinite(): Boolean {
        return x.isFinite() && y.isFinite()
    }
}
