package dev.luna5ama.trollhack.utils.combat

import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.manager.managers.RotationManager
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.extension.block
import dev.luna5ama.trollhack.utils.math.MathUtils
import dev.luna5ama.trollhack.utils.math.RotationUtils
import dev.luna5ama.trollhack.utils.rotation.Priority
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

    context(ctx: NonNullContext)
    fun getEnemies(range: Double): List<Player> = ctx.run {
        val list: MutableList<Player> = ArrayList()
        for (player in world.players) {
            if (!isValid(player, range)) continue
            list.add(player)
        }
        return list
    }

    context(ctx: NonNullContext)
    fun isValid(entity: Entity, range: Double): Boolean = ctx.run {
        val invalid = !entity.isAlive || entity == player || entity is Player && FriendManager.isFriend(
            entity.getName().string
        ) || player.distanceToSqr(entity) > MathUtils.square(range)

        return !invalid
    }

    context(ctx: NonNullContext)
    fun getClosestEnemy(distance: Double): Player? = ctx.run {
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

    context(ctx: NonNullContext)
    fun attackCrystal(pos: BlockPos, rotate: Boolean, eatingPause: Boolean): Unit = ctx.run {
        for (entity in world.getEntitiesOfClass(EndCrystal::class.java, AABB(pos))) {
            attackCrystal(entity, rotate, eatingPause)
            break
        }
    }

    context(ctx: NonNullContext)
    fun attackCrystal(box: AABB, rotate: Boolean, eatingPause: Boolean): Unit = ctx.run {
        for (entity in world.getEntitiesOfClass(EndCrystal::class.java, box)) {
            attackCrystal(entity, rotate, eatingPause)
            break
        }
    }

    context(ctx: NonNullContext)
    fun attackCrystal(crystal: Entity, rotate: Boolean, usingPause: Boolean): Unit = ctx.run {
        if (!breakTimer.tickAndReset((ClientSettings.attackDelay * 1000).toLong())) return
        if (usingPause && player.isUsingItem) return
        breakTimer.reset()
        if (rotate && ClientSettings.attackRotate) {
            RotationManager.setRotations(
                RotationUtils.getRotationTo(Vec3(
                    crystal.x,
                    crystal.y + 0.25,
                    crystal.z
                )),
                priority = Priority.High
            )
        }
        netHandler.send(ServerboundInteractPacket.createAttackPacket(crystal, player.isShiftKeyDown))
        player.resetAttackStrengthTicker()
        player.swing(InteractionHand.MAIN_HAND)
    }

    context(ctx: NonNullContext)
    fun isHard(pos: BlockPos): Boolean = ctx.run {
        val block: Block = pos.block
        return block === Blocks.OBSIDIAN || block === Blocks.NETHERITE_BLOCK || block === Blocks.ENDER_CHEST || block === Blocks.BEDROCK
    }

}
