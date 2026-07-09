package dev.luna5ama.trollhack.utils.combat

import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.extension.block
import dev.luna5ama.trollhack.utils.math.MathUtils
import dev.luna5ama.trollhack.utils.math.RotationUtils
import dev.luna5ama.trollhack.utils.timing.TickTimer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.entity.Entity
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object CombatUtil {

    private val breakTimer = TickTimer()

    context(NonNullContext)
    fun getEnemies(range: Double): List<Player> {
        val list: MutableList<Player> = ArrayList()
        for (player in world.players) {
            if (!isValid(player, range)) continue
            list.add(player)
        }
        return list
    }

    context(NonNullContext)
    fun isValid(entity: Entity, range: Double): Boolean {
        val invalid = !entity.isAlive || entity == player || entity is Player && FriendManager.isFriend(
            entity.getName().string
        ) || player.distanceToSqr(entity) > MathUtils.square(range)

        return !invalid
    }

    context(NonNullContext)
    fun getClosestEnemy(distance: Double): Player? {
        var closest: Player? = null

        for (player in getEnemies(distance)) {
            if (closest == null) {
                closest = player
                continue
            }

            if (player.eyePosition.distanceToSqr(player.position()) >= player.distanceToSqr(closest)) continue

            closest = player
        }
        return closest
    }

    context(NonNullContext)
    fun attackCrystal(pos: BlockPos, rotate: Boolean, eatingPause: Boolean) {
        for (entity in world.getEntitiesOfClass(EndCrystal::class.java, AABB(pos))) {
            attackCrystal(entity, rotate, eatingPause)
            break
        }
    }

    context(NonNullContext)
    fun attackCrystal(box: AABB, rotate: Boolean, eatingPause: Boolean) {
        for (entity in world.getEntitiesOfClass(EndCrystal::class.java, box)) {
            attackCrystal(entity, rotate, eatingPause)
            break
        }
    }

    context(NonNullContext)
    fun attackCrystal(crystal: Entity, rotate: Boolean, usingPause: Boolean) {
        if (!breakTimer.tickAndReset((ClientSettings.attackDelay * 1000).toLong())) return
        if (usingPause && player.isUsingItem) return
        breakTimer.reset()
        if (rotate && ClientSettings.attackRotate) {
            PlayerPacketManager.sendPlayerPacket(114514) {
                cancelRotate()
                rotate(RotationUtils.getRotationTo(Vec3(
                    crystal.x,
                    crystal.y + 0.25,
                    crystal.z
                )))
            }
        }
        netHandler.send(ServerboundInteractPacket.createAttackPacket(crystal, player.isShiftKeyDown))
        player.resetAttackStrengthTicker()
        player.swing(InteractionHand.MAIN_HAND)
    }

    context(NonNullContext)
    fun isHard(pos: BlockPos): Boolean {
        val block: Block = pos.block
        return block === Blocks.OBSIDIAN || block === Blocks.NETHERITE_BLOCK || block === Blocks.ENDER_CHEST || block === Blocks.BEDROCK
    }

}