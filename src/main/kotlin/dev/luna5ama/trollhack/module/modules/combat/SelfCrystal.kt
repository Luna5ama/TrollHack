package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.EntityEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSpawnEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.gui.hudgui.elements.client.Notification
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.accessor.id
import dev.luna5ama.trollhack.util.accessor.packetAction
import dev.luna5ama.trollhack.util.combat.CrystalUtils.canPlaceCrystal
import dev.luna5ama.trollhack.util.inventory.slot.allSlots
import dev.luna5ama.trollhack.util.inventory.slot.firstItem
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.pause.MainHandPause
import dev.luna5ama.trollhack.util.pause.OffhandPause
import dev.luna5ama.trollhack.util.pause.withPause
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

internal object SelfCrystal : Module(
    name = "Self Crystal",
    description = "Suicide using crystal",
    category = Category.COMBAT,
    modulePriority = Int.MAX_VALUE
) {
    private val breakTimer = TickTimer()
    private var crystalPos: Vec3d? = null

    init {
        safeListener<CrystalSpawnEvent> {
            breakCrystal(it.entityID)
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            crystalPos?.let {
                sendPlayerPacket {
                    rotate(getRotationTo(it))
                }
            }
        }

        safeListener<EntityEvent.Death> {
            if (it.entity == player) {
                Notification.send(this@SelfCrystal, "$chatName Disabled on death.")
                disable()
            }
        }

        safeParallelListener<TickEvent.Post> {
            OffhandPause.withPause(this@SelfCrystal, 1000) {
                MainHandPause.withPause(this@SelfCrystal, 1000) {
                    val crystalSlot = player.allSlots.firstItem(Items.END_CRYSTAL)
                    if (crystalSlot == null) {
                        Notification.send(this@SelfCrystal, "$chatName No crystal found in inventory, disabling.")
                        disable()
                        return@safeParallelListener
                    }

                    if (breakTimer.tick(500L)) {
                        CombatManager.crystalList
                            .maxByOrNull { it.second.selfDamage }
                            ?.let {
                                breakCrystal(it.first.entityId)
                                breakTimer.reset()
                            }
                    }

                    val mutableBlockPos = BlockPos.MutableBlockPos()

                    CombatManager.placeList.asSequence()
                        .filter { player.distanceSqTo(it.crystalPos) < 9.0 }
                        .filter { canPlaceCrystal(it.blockPos, player, mutableBlockPos) }
                        .maxByOrNull { it.selfDamage }
                        ?.let {
                            crystalPos = it.crystalPos

                            MainHandPause.withPause(this@SelfCrystal) {
                                ghostSwitch(crystalSlot) {
                                    connection.sendPacket(
                                        CPacketPlayerTryUseItemOnBlock(
                                            it.blockPos,
                                            EnumFacing.UP,
                                            EnumHand.MAIN_HAND,
                                            0.5f,
                                            1.0f,
                                            0.5f
                                        )
                                    )
                                }
                            }
                        }
                }
            }
        }
    }

    private fun SafeClientEvent.breakCrystal(entityID: Int) {
        connection.sendPacket(attackPacket(entityID))
        player.swingArm(EnumHand.MAIN_HAND)
    }

    private fun attackPacket(entityID: Int): CPacketUseEntity {
        val packet = CPacketUseEntity()
        packet.packetAction = CPacketUseEntity.Action.ATTACK
        packet.id = entityID
        return packet
    }

}