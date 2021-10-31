package cum.xiaro.trollhack.module.modules.movement

import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.player.PlayerPushOutOfBlockEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.mixin.entity.MixinEntity
import cum.xiaro.trollhack.mixin.world.MixinBlockLiquid
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.accessor.packetMotionX
import cum.xiaro.trollhack.util.accessor.packetMotionY
import cum.xiaro.trollhack.util.accessor.packetMotionZ
import cum.xiaro.trollhack.util.atTrue
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.network.play.server.SPacketEntityVelocity
import net.minecraft.network.play.server.SPacketExplosion
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.sqrt

/**
 * @see MixinEntity.applyEntityCollisionHead
 * @see MixinBlockLiquid.modifyAcceleration
 */
internal object Velocity : Module(
    name = "Velocity",
    alias = arrayOf("AntiKnockBack", "NoPush"),
    category = Category.MOVEMENT,
    description = "Modify player velocity",
) {
    private val horizontal by setting("Horizontal", 0.0f, -5.0f..5.0f, 0.05f)
    private val vertical by setting("Vertical", 0.0f, -5.0f..5.0f, 0.05f)
    private val noPush0 = setting("No Push", true)
    private val noPush by noPush0
    private val entity by setting("Entity", true, noPush0.atTrue())
    private val liquid by setting("Liquid", true, noPush0.atTrue())
    private val block by setting("Block", true, noPush0.atTrue())
    private val pushable by setting("Pushable", true, noPush0.atTrue())

    override fun getHudInfo(): String {
        return "$horizontal/$vertical"
    }

    init {
        safeListener<PacketEvent.Receive>(-1000) {
            if (it.packet is SPacketEntityVelocity) {
                with(it.packet) {
                    if (entityID != player.entityId) return@safeListener
                    if (isZero()) {
                        it.cancel()
                    } else {
                        packetMotionX = (packetMotionX * horizontal).toInt()
                        packetMotionY = (packetMotionY * vertical).toInt()
                        packetMotionZ = (packetMotionZ * horizontal).toInt()
                    }
                }
            } else if (it.packet is SPacketExplosion) {
                with(it.packet) {
                    if (isZero()) {
                        it.cancel()
                    } else {
                        packetMotionX *= horizontal
                        packetMotionY *= vertical
                        packetMotionZ *= horizontal
                    }
                }
            }
        }

        listener<PlayerPushOutOfBlockEvent> {
            if (block) {
                it.cancel()
            }
        }
    }

    private fun isZero(): Boolean {
        return horizontal == 0.0f && vertical == 0.0f
    }

    // Junky but not no compatibility issues
    @JvmStatic
    fun handleApplyEntityCollision(entity1: Entity, entity2: Entity, ci: CallbackInfo) {
        if (isDisabled || !noPush || !entity) return
        if (entity1.isRidingSameEntity(entity2) || entity1.noClip || entity2.noClip) return

        val player = mc.player ?: return
        if (entity1 != player && entity2 != player) return

        var x = entity2.posX - entity1.posX
        var z = entity2.posZ - entity1.posZ
        var dist = max(x.absoluteValue, z.absoluteValue)

        if (dist < 0.01) return

        dist = sqrt(dist)
        x /= dist
        z /= dist

        val multiplier = (1.0 / dist).coerceAtMost(1.0)
        val collisionReduction = 1.0f - entity1.entityCollisionReduction

        x *= multiplier * 0.05 * collisionReduction
        z *= multiplier * 0.05 * collisionReduction

        entity1.addCollisionVelocity(player, -x, -z)
        entity2.addCollisionVelocity(player, x, z)

        ci.cancel()
    }

    private fun Entity.addCollisionVelocity(player: EntityPlayerSP, x: Double, z: Double) {
        if (this != player && !isBeingRidden) {
            motionX += x
            motionZ += z
            isAirBorne = true
        }
    }

    @JvmStatic
    fun shouldCancelLiquidVelocity(): Boolean {
        return isEnabled && noPush && liquid
    }

    @JvmStatic
    fun shouldCancelMove(): Boolean {
        return isEnabled && pushable
    }
}
