package dev.luna5ama.trollhack.module.modules.combat

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.player.PlayerAttackEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.translation.TranslateType
import dev.luna5ama.trollhack.util.EntityUtils.isInOrAboveLiquid
import dev.luna5ama.trollhack.util.accessor.isInWeb
import dev.luna5ama.trollhack.util.atValue
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.notAtValue
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.MobEffects
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.util.EnumHand
import net.minecraft.world.GameType

internal object Criticals : Module(
    name = "Criticals",
    category = Category.COMBAT,
    description = "Always do critical attacks"
) {
    private val mode0 = setting("Mode", Mode.PACKET)
    private val mode by mode0
    private val jumpMotion by setting(
        "Jump Motion",
        0.25,
        0.1..0.5,
        0.01,
        mode0.atValue(Mode.MINI_JUMP),
        fineStep = 0.001
    )
    private val attackFallDistance by setting(
        "Attack Fall Distance",
        0.1,
        0.05..1.0,
        0.05,
        mode0.notAtValue(Mode.PACKET)
    )

    private enum class Mode(override val displayName: CharSequence) : DisplayEnum {
        PACKET(TranslateType.COMMON commonKey "Packet"),
        JUMP(TranslateType.COMMON commonKey "Jump"),
        MINI_JUMP(TranslateType.COMMON commonKey "Mini Jump")
    }

    private var delayTick = -1
    private var target: Entity? = null
    private var attacking = false

    override fun isActive(): Boolean {
        return isEnabled && !delaying()
    }

    override fun getHudInfo(): String {
        return mode.displayString
    }

    init {
        onDisable {
            reset()
        }

        listener<PacketEvent.Send> {
            if (it.packet is CPacketAnimation && mode != Mode.PACKET && delayTick > -1) {
                it.cancel()
            }
        }

        safeListener<PlayerAttackEvent>(0) {
            if (it.cancelled || attacking || it.entity !is EntityLivingBase || !canDoCriticals(true)) return@safeListener

            val cooldownReady = player.onGround && player.getCooledAttackStrength(0.5f) > 0.9f

            when (mode) {
                Mode.PACKET -> {
                    if (cooldownReady) {
                        connection.sendPacket(
                            CPacketPlayer.Position(
                                player.posX,
                                player.posY + 0.1,
                                player.posZ,
                                false
                            )
                        )
                        connection.sendPacket(CPacketPlayer.Position(player.posX, player.posY, player.posZ, false))
                    }
                }
                Mode.JUMP -> {
                    jumpAndCancel(it, cooldownReady, null)
                }
                Mode.MINI_JUMP -> {
                    jumpAndCancel(it, cooldownReady, jumpMotion)
                }
            }
        }

        safeParallelListener<TickEvent.Post> {
            if (mode == Mode.PACKET || delayTick <= -1) return@safeParallelListener

            delayTick--

            if (target != null && player.fallDistance >= attackFallDistance && canDoCriticals(!player.onGround)) {
                val target = target
                reset()

                if (target != null) {
                    attacking = true
                    connection.sendPacket(CPacketUseEntity(target))
                    connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
                    attacking = false
                }
            }
        }
    }

    private fun reset() {
        delayTick = -1
        target = null
    }

    private fun SafeClientEvent.jumpAndCancel(event: PlayerAttackEvent, cooldownReady: Boolean, motion: Double?) {
        if (cooldownReady && !delaying()) {
            player.jump()
            if (motion != null) player.motionY = motion
            target = event.entity

            if (playerController.currentGameType != GameType.SPECTATOR) {
                player.attackTargetEntityWithCurrentItem(event.entity)
                player.resetCooldown()
            }

            delayTick = 20
        }

        event.cancel()
    }

    private fun delaying() =
        mode != Mode.PACKET && delayTick > -1 && target != null

    private fun SafeClientEvent.canDoCriticals(onGround: Boolean) =
        onGround
            && !player.isInWeb
            && !player.isOnLadder
            && !player.isRiding
            && !player.isPotionActive(MobEffects.BLINDNESS)
            && !player.isInOrAboveLiquid
}