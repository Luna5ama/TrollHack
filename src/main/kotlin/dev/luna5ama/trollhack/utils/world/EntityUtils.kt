package dev.luna5ama.trollhack.utils.world

import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.math.vectors.toBlockPos
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.entity.Entity
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket
import net.minecraft.util.Mth
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.ambient.AmbientCreature
import net.minecraft.world.entity.animal.golem.IronGolem
import net.minecraft.world.entity.animal.squid.Squid
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin
import net.minecraft.world.entity.monster.piglin.Piglin
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Input
import net.minecraft.world.entity.projectile.arrow.Arrow
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.atan2
import kotlin.math.sqrt

object EntityUtils {

    context(ctx: NonNullContext)
    fun facePosSide(pos: BlockPos, side: Direction): Unit = ctx.run {
        val hitVec = pos.center.add(Vec3(side.unitVec3.x * 0.5, side.unitVec3.y * 0.5, side.unitVec3.z * 0.5))
        faceVector(hitVec)
    }

    fun getInterpolatedAmount(entity: Entity, ticks: Double): Vec3 {
        return getInterpolatedAmount(entity, ticks, ticks, ticks)
    }

    fun isTargetHere(pos: BlockPos, target: Entity): Boolean {
        return AABB(pos).intersects(target.boundingBox)
    }

    fun Player.isInWeb(): Boolean {
        val playerPos: Vec3 = this.position()
        for (x in floatArrayOf(0f, 0.3f, -0.3f)) {
            for (z in floatArrayOf(0f, 0.3f, -0.3f)) {
                for (y in floatArrayOf(0f, 1f, -1f)) {
                    val pos = Vec3(playerPos.x + x, playerPos.y + y, playerPos.z + z).toBlockPos()
                    if (isTargetHere(
                            pos,
                            this
                        ) && level().getBlockState(pos).block == Blocks.COBWEB
                    ) {
                        return true
                    }
                }
            }
        }
        return false
    }

    context(ctx: NonNullContext)
    fun Player.isInsideBlock(): Boolean {
        if (ctx.world.getBlockState(position().toBlockPos()).block == Blocks.ENDER_CHEST) return true
        return ctx.world.collidesWithSuffocatingBlock(this, this.boundingBox)
    }

    context(ctx: NonNullContext)
    fun hasEntity(pos: AABB, breakCrystal: Boolean = true): Boolean = ctx.run {
        for (entity in world.getEntitiesOfClass(Entity::class.java, pos)) {
            if (entity == player) continue
            if (!entity.isAlive || entity is ItemEntity || entity is ExperienceOrb
                || entity is ThrownExperienceBottle || entity is Arrow
                || entity is EndCrystal && breakCrystal || entity is ArmorStand
            ) continue
            return true
        }
        return false
    }

    context(ctx: NonNullContext)
    fun hasEntity(pos: BlockPos, ignoreCrystal: Boolean): Boolean = ctx.run {
        for (entity in world.getEntitiesOfClass(Entity::class.java, AABB(pos))) {
            if (!entity.isAlive || entity is ItemEntity || entity is ExperienceOrb || entity is ThrownExperienceBottle || entity is Arrow || ignoreCrystal && entity is EndCrystal) continue
            return true
        }
        return false
    }

    fun getInterpolatedAmount(entity: Entity, x: Double, y: Double, z: Double): Vec3 {
        return Vec3(
            (entity.x - entity.xo) * x,
            (entity.y - entity.yo) * y,
            (entity.z - entity.zo) * z
        )
    }

    context(ctx: NonNullContext)
    inline fun Player.spoofSneak(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        if (!this.isShiftKeyDown) {
            ctx.netHandler.send(ServerboundPlayerInputPacket(Input(false, false, false, false, false, true, ctx.player.isSprinting)))
            block.invoke()
            ctx.netHandler.send(ServerboundPlayerInputPacket(Input.EMPTY))

        } else {
            block.invoke()
        }
    }

    fun getInterpolatedPos(entity: Entity, ticks: Float): Vec3 {
        return Vec3(entity.xo, entity.yo, entity.zo).add(
            getInterpolatedAmount(entity, ticks.toDouble())
        )
    }

    context(ctx: NonNullContext)
    fun sendLook(lookAndOnGround: ServerboundMovePlayerPacket.Rot): Unit = ctx.run {
        netHandler.send(lookAndOnGround)
    }

    context(ctx: NonNullContext)
    fun faceVector(directionVec: Vec3): Unit = ctx.run {

        val angle: FloatArray = getLegitRotations(directionVec)
        sendLook(ServerboundMovePlayerPacket.Rot(angle[0], angle[1], player.onGround(), player.horizontalCollision))
    }

    context(ctx: NonNullContext)
    fun canSee(pos: BlockPos, side: Direction): Boolean = ctx.run {
        val testVec = pos.center.add(side.unitVec3.x * 0.5, side.unitVec3.y * 0.5, side.unitVec3.z * 0.5)
        val result = world.clip(
            ClipContext(
                getEyesPos(),
                testVec,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
            )
        )

        return result.type == HitResult.Type.MISS
    }

    context(ctx: NonNullContext)
    fun getEyesPos(): Vec3 = ctx.run {
        return player.eyePosition
    }

    context(ctx: NonNullContext)
    fun getLegitRotations(vec: Vec3): FloatArray = ctx.run {
        val eyesPos: Vec3 = getEyesPos()
        val diffX = vec.x - eyesPos.x
        val diffY = vec.y - eyesPos.y
        val diffZ = vec.z - eyesPos.z
        val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)
        val yaw = Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90.0f
        val pitch = (-Math.toDegrees(atan2(diffY, diffXZ))).toFloat()
        return floatArrayOf(
            player.yRot + Mth.wrapDegrees(yaw - player.yRot),
            player.xRot + Mth.wrapDegrees(pitch - player.xRot)
        )
    }

    private fun isNeutralMob(entity: Entity) = entity is Piglin
            || entity is Wolf
            || entity is EnderMan
            || entity is IronGolem


    private fun isMobAggressive(entity: Entity) = when (entity) {
        is ZombifiedPiglin -> {
            // arms raised = aggressive, angry = either game or we have set the anger cooldown
            entity.persistentAngerTarget != null
        }

        is Wolf -> {
            entity.persistentAngerTarget != null && entity.owner != mc.player
        }

        is EnderMan -> {
            entity.isAngry
        }

        is IronGolem -> {
            entity.persistentAngerTarget != null
        }

        else -> {
            entity is Monster
        }
    }


    fun isPassiveMob(e: Entity): Boolean {
        return e is AmbientCreature || e is Squid
    }

    fun isCurrentlyNeutral(entity: Entity): Boolean {
        return isNeutralMob(entity) && !isMobAggressive(entity)
    }

    fun mobTypeSettings(e: Entity, mobs: Boolean, passive: Boolean, neutral: Boolean, hostile: Boolean): Boolean {
        return mobs && (passive && isPassiveMob(e) || neutral && isCurrentlyNeutral(e) || hostile && isMobAggressive(e))
    }

    fun playerTypeCheck(player: Player, friend: Boolean, sleeping: Boolean) =
        (friend || !FriendManager.isFriend(player.name.string)) && (sleeping || !player.isSleeping)

    fun getEntityCLionPos(entity: Entity): BlockPos {
        return CLionPos(entity.position())
    }

    context(ctx: NonNullContext)
    fun getPlayerCLionPos(fix: Boolean): BlockPos = ctx.run {
        return CLionPos(player.position(), fix)
    }

    fun getEntityCLionPos(entity: Entity, fix: Boolean): BlockPos {
        return CLionPos(entity.position(), fix)
    }

    context(ctx: NonNullContext)
    fun isInsideBlock(): Boolean = ctx.run {
        if (BlockUtils.getBlock(getPlayerCLionPos(true)) === Blocks.ENDER_CHEST) return true
        return world.collidesWithSuffocatingBlock(player, player.boundingBox)
    }
}
