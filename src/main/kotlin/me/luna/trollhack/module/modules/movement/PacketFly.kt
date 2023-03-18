package me.luna.trollhack.module.modules.movement

import io.netty.util.internal.ConcurrentSet
import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.PacketEvent
import me.luna.trollhack.event.events.player.PlayerMoveEvent
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.MovementUtils
import me.luna.trollhack.util.MovementUtils.calcMoveYaw
import net.minecraft.network.play.client.CPacketConfirmTeleport
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.server.SPacketCloseWindow
import net.minecraft.network.play.server.SPacketPlayerPosLook
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

internal object PacketFly : Module(
    name = "PacketFly",
    description = "Experimental!",
    category = Category.MOVEMENT,
    modulePriority = 9999
) {
    private val page by setting("Page", Page.MOVEMENT)

    private val upSpeed by setting("Up Speed", 0.04, 0.0..1.0, 0.01, { page == Page.MOVEMENT })
    private val downSpeed by setting("Down Speed", 0.062, 0.0..1.0, 0.01, { page == Page.MOVEMENT })
    private val speed by setting("Speed", 0.062, 0.0..1.0, 0.01, { page == Page.MOVEMENT })
    private val confirmTeleportMove by setting("Confirm Teleport Move", true, { page == Page.MOVEMENT })

    private val spoofX = SpoofSetting("X").apply {
        mode = SpoofMode.RANDOM
        randomMin = -10.0
        randomMax = 10.0
    }
    private val spoofY = SpoofSetting("Y").apply {
        mode = SpoofMode.JITTER
        randomMin = -100.0
        randomMax = -80.0
        jitterMin = 80.0
        jitterMax = 100.0
    }
    private val spoofZ = SpoofSetting("Z").apply {
        mode = SpoofMode.RANDOM
        randomMin = -10.0
        randomMax = 10.0
    }

    private val maxServerIgnore by setting("Max Server Ignores", 2, 0..10, 1, { page == Page.SERVER_PACKET })

    private enum class Page {
        MOVEMENT, SPOOF, SERVER_PACKET
    }

    private enum class SpoofMode {
        CONSTANT,
        RANDOM,
        JITTER,
    }

    private class SpoofSetting(axis: String) {
        var mode by setting("$axis Mode", SpoofMode.CONSTANT, { page == Page.SPOOF })

        var constant by setting(
            "$axis Constant",
            0.0,
            -100000.0..100000.0,
            1.0,
            { page == Page.SPOOF && mode == SpoofMode.CONSTANT }
        )

        var randomMin: Double by setting(
            "$axis Random Min",
            0.0,
            -100000.0..100000.0,
            1.0,
            { page == Page.SPOOF && (mode == SpoofMode.RANDOM || mode == SpoofMode.JITTER) },
            consumer = { _, input -> min(input, randomMax) }
        )

        var randomMax: Double by setting(
            "$axis Random Max",
            0.0,
            -100000.0..100000.0,
            1.0,
            { page == Page.SPOOF && (mode == SpoofMode.RANDOM || mode == SpoofMode.JITTER) },
            consumer = { _, input -> max(input, randomMin) }
        )
        var jitterMin: Double by setting(
            "$axis Jitter Min",
            0.0,
            -100000.0..100000.0,
            1.0,
            { page == Page.SPOOF && mode == SpoofMode.JITTER },
            consumer = { _, input -> min(input, jitterMax) })

        var jitterMax: Double by setting(
            "$axis Jitter Max",
            0.0,
            -100000.0..100000.0,
            1.0,
            { page == Page.SPOOF && mode == SpoofMode.JITTER },
            consumer = { _, input -> max(input, jitterMin) }
        )

        fun offset(event: SafeClientEvent): Double {
            event {
                when (mode) {
                    SpoofMode.CONSTANT -> {
                        return constant
                    }
                    SpoofMode.RANDOM -> {
                        return Random.nextDouble(randomMin, randomMax)
                    }
                    SpoofMode.JITTER -> {
                        return if (Random.nextBoolean()) {
                            Random.nextDouble(randomMin, randomMax)
                        } else {
                            Random.nextDouble(jitterMin, jitterMax)
                        }
                    }
                }
            }
        }
    }

    private val packetSet = ConcurrentSet<CPacketPlayer>()
    private var teleportID = 0
    private var serverIgnores = 0

    init {
        safeListener<PacketEvent.Send> {
            if (player.ticksExisted < 10) return@safeListener

            when (it.packet) {
                is CPacketPlayer -> {
                    if (!packetSet.remove(it.packet)) {
                        it.cancel()
                    }
                }
            }
        }

        safeListener<PacketEvent.Receive> {
            if (player.ticksExisted < 10) return@safeListener

            when (it.packet) {
                is SPacketCloseWindow -> {
                    it.cancel()
                }
                is SPacketPlayerPosLook -> {
                    it.cancel()
                    var x = it.packet.x
                    var y = it.packet.y
                    var z = it.packet.z
                    var yaw = it.packet.yaw
                    var pitch = it.packet.pitch

                    if (it.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.X)) {
                        x += player.posX
                    }

                    if (it.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.Y)) {
                        y += player.posY
                    }

                    if (it.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.Z)) {
                        z += player.posZ
                    }

                    if (it.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.X_ROT)) {
                        pitch += player.rotationPitch
                    }

                    if (it.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.Y_ROT)) {
                        yaw += player.rotationYaw
                    }

                    if (++serverIgnores > maxServerIgnore) {
                        player.setPosition(x, y, z)
                        serverIgnores = 0
                    }

                    teleportID = it.packet.teleportId
                    connection.sendPacket(CPacketConfirmTeleport(it.packet.teleportId))
                    sendPlayerPacket(
                        CPacketPlayer.PositionRotation(
                            player.posX,
                            player.entityBoundingBox.minY,
                            player.posZ,
                            player.rotationYaw,
                            player.rotationPitch,
                            false
                        )
                    )
                }
            }
        }

        safeListener<PlayerMoveEvent.Pre> {
            if (player.ticksExisted < 10) return@safeListener

            it.x = 0.0
            it.y = 0.0
            it.z = 0.0
            it.cancel()

            player.motionY = -0.01

            var motionX = 0.0
            var motionY = 0.0
            var motionZ = 0.0

            if (player.movementInput.jump != player.movementInput.sneak) {
                motionY = if (player.movementInput.jump) upSpeed else -downSpeed
            }

            if (MovementUtils.isInputting) {
                val yaw = calcMoveYaw()
                motionX -= sin(yaw) * speed
                motionZ += cos(yaw) * speed
            }

            player.setPosition(
                player.posX + motionX,
                player.posY + motionY,
                player.posZ + motionZ
            )

            sendPlayerPacket(
                CPacketPlayer.PositionRotation(
                    player.posX,
                    player.posY,
                    player.posZ,
                    player.rotationYaw,
                    player.rotationPitch,
                    player.onGround
                )
            )

            sendPlayerPacket(
                CPacketPlayer.Position(
                    player.posX + spoofX.offset(this),
                    player.posY + spoofY.offset(this),
                    player.posZ + spoofZ.offset(this),
                    player.onGround
                )
            )

            if (confirmTeleportMove) {
                connection.sendPacket(CPacketConfirmTeleport(++teleportID))
            }
        }
    }

    private fun SafeClientEvent.sendPlayerPacket(packet: CPacketPlayer) {
        packetSet.add(packet)
        connection.sendPacket(packet)
    }
}