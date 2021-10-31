package cum.xiaro.trollhack.manager.managers

import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import cum.xiaro.trollhack.event.events.render.RenderEntityEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.Manager
import cum.xiaro.trollhack.module.AbstractModule
import cum.xiaro.trollhack.util.Wrapper
import cum.xiaro.trollhack.util.accessor.*
import cum.xiaro.trollhack.util.math.vector.Vec2f
import cum.xiaro.trollhack.util.threads.runSafe
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import java.util.concurrent.atomic.AtomicReference

object PlayerPacketManager : Manager() {
    private val pendingPacket = AtomicReference<Packet?>()

    var position: Vec3d = Vec3d.ZERO; private set
    var prevPosition: Vec3d = Vec3d.ZERO; private set

    var eyePosition: Vec3d = Vec3d.ZERO; private set
    var boundingBox: AxisAlignedBB = AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0); private set

    var rotation = Vec2f.ZERO; private set
    var prevRotation = Vec2f.ZERO; private set

    val rotationX: Float
        get() = rotation.x

    val rotationY: Float
        get() = rotation.y

    private var clientSidePitch = Vec2f.ZERO

    init {
        listener<PacketEvent.PostSend>(-6969) {
            if (it.packet !is CPacketPlayer) return@listener

            runSafe {
                if (it.packet.moving) {
                    position = Vec3d(it.packet.x, it.packet.y, it.packet.z)
                    eyePosition = Vec3d(it.packet.x, it.packet.y + player.getEyeHeight(), it.packet.z)

                    val halfWidth = player.width / 2.0
                    boundingBox = AxisAlignedBB(
                        it.packet.x - halfWidth, it.packet.y, it.packet.z - halfWidth,
                        it.packet.x + halfWidth, it.packet.y + player.height, it.packet.z + halfWidth,
                    )
                }

                if (it.packet.rotating) {
                    rotation = Vec2f(it.packet.yaw, it.packet.pitch)
                }
            }
        }

        safeListener<TickEvent.Pre>(Int.MAX_VALUE) {
            prevPosition = position
            prevRotation = rotation
        }

        listener<RenderEntityEvent.All.Pre> {
            if (it.entity != Wrapper.player || it.entity.isRiding) return@listener

            clientSidePitch = Vec2f(it.entity.prevRotationPitch, it.entity.rotationPitch)
            it.entity.prevRotationPitch = prevRotation.y
            it.entity.rotationPitch = rotation.y
        }

        listener<RenderEntityEvent.All.Post> {
            if (it.entity != mc.player || it.entity.isRiding) return@listener

            it.entity.prevRotationPitch = clientSidePitch.x
            it.entity.rotationPitch = clientSidePitch.y
        }
    }

    fun applyPacket(event: OnUpdateWalkingPlayerEvent.Pre) {
        val packet = pendingPacket.getAndSet(null)
        if (packet != null) {
            event.apply(packet)
        }
    }

    inline fun AbstractModule.sendPlayerPacket(block: Packet.Builder.() -> Unit) {
        sendPlayerPacket(this.modulePriority, block)
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

    class Packet private constructor(
        val priority: Int,
        val position: Vec3d?,
        val rotation: Vec2f?,
        val onGround: Boolean?,
        val cancelMove: Boolean,
        val cancelRotate: Boolean,
        val cancelAll: Boolean
    ) {
        class Builder(private val priority: Int) {
            private var position: Vec3d? = null
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

            fun move(position: Vec3d) {
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
                if (!empty) Packet(priority, position, rotation, onGround, cancelMove, cancelRotate, cancelAll) else null
        }
    }
}