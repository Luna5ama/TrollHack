package dev.luna5ama.trollhack.manager.managers

import com.google.common.collect.MapMaker
import com.google.common.util.concurrent.AtomicDouble
import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.event.impl.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.impl.render.RenderEntityEvent
import dev.luna5ama.trollhack.manager.AbstractManager
import dev.luna5ama.trollhack.modules.AbstractModule
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.extension.getValue
import dev.luna5ama.trollhack.utils.extension.setValue
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.runSafe
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.util.Mth
import net.minecraft.world.entity.Relative
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.*

object PlayerPacketManager : AbstractManager(), AlwaysListening {
    private val ignoreUpdateSet = Collections.newSetFromMap<ServerboundMovePlayerPacket>(MapMaker().weakKeys().makeMap())
    private val pendingPacket = AtomicReference<Packet?>()
    private var lastMilli = System.currentTimeMillis()
    var clientTps by AtomicDouble(); private set

    var position: Vec3 = Vec3.ZERO; private set
    var prevPosition: Vec3 = Vec3.ZERO; private set

    var eyePosition: Vec3 = Vec3.ZERO; private set
    var boundingBox: AABB = AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0); private set

    var rotation = Vec2f.ZERO; private set
    var prevRotation = Vec2f.ZERO; private set

    val rotationX: Float
        get() = rotation.x

    val rotationY: Float
        get() = rotation.y

    val x get() = position.x
    val y get() = position.y
    val z get() = position.z
    val yawSpeed get() = ClientSettings.yawSpeedLimit

    private var clientSidePitch = Vec2f.ZERO

    init {
        handler<PacketEvent.Send>(Int.MIN_VALUE) {
            if (it.packet !is ServerboundMovePlayerPacket) return@handler
        }

        handler<PacketEvent.PostSend>(-6969) {
            if (it.packet !is ServerboundMovePlayerPacket) return@handler
            if (ignoreUpdateSet.remove(it.packet)) return@handler

            runSafe {
                if (it.packet.hasPosition()) {
                    position = Vec3(it.packet.getX(x), it.packet.getY(y), it.packet.getZ(z))
                    eyePosition = Vec3(
                        it.packet.getX(x),
                        it.packet.getY(y) + player.getEyeHeight(player.pose),
                        it.packet.getZ(z)
                    )

                    val halfWidth = player.bbWidth / 2.0
                    boundingBox = AABB(
                        it.packet.getX(x) - halfWidth, it.packet.getY(y), it.packet.getZ(z) - halfWidth,
                        it.packet.getX(x) + halfWidth, it.packet.getY(y) + player.bbHeight, it.packet.getZ(z) + halfWidth,
                    )
                }
                rotation = Vec2f(it.packet.getYRot(rotationX), it.packet.getXRot(rotationY))
            }
        }

        handler<PacketEvent.PostReceive>() { event ->
            var (lastYaw, lastPitch) = prevRotation
            if (event.packet is ClientboundPlayerPositionPacket) {
                if (event.packet.relatives.contains(Relative.X_ROT)) {
                    lastYaw += event.packet.change.yRot
                } else {
                    lastYaw = event.packet.change.yRot
                }

                if (event.packet.relatives.contains(Relative.Y_ROT)) {
                    lastPitch += event.packet.change.xRot
                } else {
                    lastPitch = event.packet.change.xRot
                }
                prevRotation = Vec2f(lastYaw, lastPitch)
            }
        }

        nonNullHandler<TickEvent.Pre>(Int.MAX_VALUE) {
            prevPosition = position
            prevRotation = rotation
        }

        handler<RenderEntityEvent.All.Pre> {
//            ChatUtils.sendMessage("Render Pre")
            if (it.entity != mc.player || it.entity.isPassenger) return@handler

            clientSidePitch = Vec2f(it.entity.xRotO, it.entity.xRot)
            it.entity.xRotO = prevRotation.y
            it.entity.xRot = rotation.y
        }

        handler<RenderEntityEvent.All.Post> {
//            ChatUtils.sendMessage("Render Post")
            if (it.entity != mc.player || it.entity.isPassenger) return@handler

            it.entity.xRotO = clientSidePitch.x
            it.entity.xRot = clientSidePitch.y
        }

        nonNullHandler<TickEvent.Pre> {
            val current = System.currentTimeMillis()
            val tps = 1000.0 / (current - lastMilli)
            clientTps = tps
            lastMilli = current
        }
    }

    fun applyPacket(event: OnUpdateWalkingPlayerEvent.Pre) {
        runSafe {
            val packet = pendingPacket.getAndSet(null)?.interpolateRotation()
            if (packet != null) {
                packet.rotation?.let {
//                    ChatUtils.sendMessage("Fixed rotation $it with step $yawSpeed")
                    rotation = it
                }
                event.apply(packet)
            }
        }
    }

    fun ignoreUpdate(packet: ServerboundMovePlayerPacket) {
        ignoreUpdateSet.add(packet)
    }

    inline fun AbstractModule.sendPlayerPacket(block: Packet.Builder.() -> Unit) {
        sendPlayerPacket(this.priority, block)
    }

    inline fun sendPlayerPacket(priority: Int, block: Packet.Builder.() -> Unit) {
        Packet.Builder(priority).apply(block).build()?.let {
            sendPlayerPacket(it)
        }
    }

    fun sendPlayerPacket(packet: Packet) {
        pendingPacket.updateAndGet {
            if (it == null || it.priority < packet.priority) {
                packet
            } else {
                it
            }
        }
    }

    fun injectStep(angle: Vec2f, steps: Float): Vec2f {
        return injectStep(rotation, angle, steps)
    }

    fun injectStep(rotation: Vec2f, targetRotation: Vec2f, steps0: Float): Vec2f {
        val angle = floatArrayOf(targetRotation.x, targetRotation.y)
        val steps = steps0.coerceIn(0.01f, 1f)
        if (steps < 1) {
            val packetYaw = rotation.x
            var diff = Mth.degreesDifferenceAbs(angle[0], packetYaw)
            if (abs(diff.toDouble()) > 180 * steps) {
                angle[0] = (packetYaw + (diff * ((180 * steps) / abs(diff.toDouble())))).toFloat()
            }
            val packetPitch = rotation.y
            diff = angle[1] - packetPitch
            if (abs(diff.toDouble()) > 90 * steps) {
                angle[1] = (packetPitch + (diff * ((90 * steps) / abs(diff.toDouble())))).toFloat()
            }
        }
        return Vec2f(angle[0], angle[1])
    }

    class Packet private constructor(
        val priority: Int,
        val position: Vec3?,
        val rotation: Vec2f?,
        val onGround: Boolean?,
        val cancelMove: Boolean,
        val cancelRotate: Boolean,
        val cancelAll: Boolean
    ) {
        class Builder(private val priority: Int) {
            private var position: Vec3? = null
            private var rotation: Vec2f? = null
            private var onGround: Boolean? = null

            private var cancelMove = false
            private var cancelRotate = false
            private var cancelAll = false
            private var empty = true

            fun onGround(onGround: Boolean) {
                this.onGround = onGround
                this.empty = false
            }

            fun move(position: Vec3) {
                this.position = position
                this.cancelMove = false
                this.empty = false
            }

            fun rotate(rotation: Vec2f) {
                this.rotation = rotation
                this.cancelRotate = false
                this.empty = false
            }

            fun cancelAll() {
                this.cancelMove = true
                this.cancelRotate = true
                this.cancelAll = true
                this.empty = false
            }

            fun cancelMove() {
                this.position = null
                this.cancelMove = true
                this.empty = false
            }

            fun cancelRotate() {
                this.rotation = null
                this.cancelRotate = true
                this.empty = false
            }

            fun build() =
                if (!empty) Packet(
                    priority,
                    position,
                    rotation,
                    onGround,
                    cancelMove,
                    cancelRotate,
                    cancelAll
                ) else null
        }

        context(NonNullContext)
        fun interpolateRotation(): Packet {
            val currentRotation = PlayerPacketManager.rotation
            return Packet(
                priority, position,
                rotation?.let {
//                    Vec2f(currentRotation.x + RotationUtils.normalizeAngle(((it.x + 360) - (currentRotation.x + 360)))
//                        .coerceIn(-yawSpeed..yawSpeed), it.y)
                    injectStep(currentRotation, it, yawSpeed / 180f)
                },
                onGround, cancelMove, cancelRotate, cancelAll
            )
        }
    }
}