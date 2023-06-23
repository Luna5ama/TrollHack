package dev.luna5ama.trollhack.util

import dev.fastmc.common.floorToInt
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.util.inventory.id
import dev.luna5ama.trollhack.util.math.vector.distanceTo
import dev.luna5ama.trollhack.util.math.vector.toBlockPos
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityAgeable
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.EnumCreatureType
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntityIronGolem
import net.minecraft.entity.monster.EntityPigZombie
import net.minecraft.entity.passive.*
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

object EntityUtils {
    private val mc = Minecraft.getMinecraft()

    val SafeClientEvent.viewEntity get() = mc.renderViewEntity ?: player

    val viewEntity: Entity? get() = mc.renderViewEntity ?: mc.player

    val Entity.eyePosition get() = Vec3d(this.posX, this.posY + this.eyeHeight, this.posZ)
    val Entity.lastTickPos get() = Vec3d(this.lastTickPosX, this.lastTickPosY, this.lastTickPosZ)
    val Entity.flooredPosition get() = BlockPos(this.posX.floorToInt(), this.posY.floorToInt(), this.posZ.floorToInt())
    val Entity.betterPosition
        get() = BlockPos(
            this.posX.floorToInt(),
            (this.posY + 0.25).floorToInt(),
            this.posZ.floorToInt()
        )

    val Entity.isPassive
        get() = this is EntityAgeable
            || this is EntityAmbientCreature
            || this is EntitySquid

    val Entity.isNeutral get() = isNeutralMob(this) && !isMobAggressive(this)

    val Entity.isHostile get() = isMobAggressive(this)

    val Entity.isTamed
        get() = this is EntityTameable && this.isTamed || this is AbstractHorse && this.isTame

    val Entity.isInOrAboveLiquid
        get() = this.isInWater || this.isInLava || world.containsAnyLiquid(
            entityBoundingBox.expand(
                0.0,
                -1.0,
                0.0
            )
        )

    val EntityPlayer.isFriend get() = FriendManager.isFriend(this.name)

    val EntityPlayer.isSelf get() = this == mc.player || this == mc.renderViewEntity

    val EntityPlayer.isFakeOrSelf get() = this == mc.player || this == mc.renderViewEntity || this.entityId < 0

    val EntityPlayer.isFlying: Boolean
        get() = this.isElytraFlying || this.capabilities.isFlying

    private fun isNeutralMob(entity: Entity) = entity is EntityPigZombie
        || entity is EntityWolf
        || entity is EntityEnderman
        || entity is EntityIronGolem

    private fun isMobAggressive(entity: Entity) = when (entity) {
        is EntityPigZombie -> {
            // arms raised = aggressive, angry = either game or we have set the anger cooldown
            entity.isArmsRaised || entity.isAngry
        }
        is EntityWolf -> {
            entity.isAngry && mc.player != entity.owner
        }
        is EntityEnderman -> {
            entity.isScreaming
        }
        is EntityIronGolem -> {
            entity.revengeTarget != null
        }
        else -> {
            entity.isCreatureType(EnumCreatureType.MONSTER, false)
        }
    }

    fun mobTypeSettings(entity: Entity, mobs: Boolean, passive: Boolean, neutral: Boolean, hostile: Boolean): Boolean {
        return mobs && (passive && entity.isPassive || neutral && entity.isNeutral || hostile && entity.isHostile)
    }

    /**
     * Find the entities interpolated position
     */
    fun getInterpolatedPos(entity: Entity, ticks: Float): Vec3d =
        entity.lastTickPos.add(getInterpolatedAmount(entity, ticks))

    /**
     * Find the entities interpolated amount
     */
    fun getInterpolatedAmount(entity: Entity, ticks: Float): Vec3d =
        entity.positionVector.subtract(entity.lastTickPos).scale(ticks.toDouble())

    fun getTargetList(
        player: Array<Boolean>,
        mobs: Array<Boolean>,
        invisible: Boolean,
        range: Float,
        ignoreSelf: Boolean = true
    ): ArrayList<EntityLivingBase> {
        val entityList = ArrayList<EntityLivingBase>()
        for (entity in EntityManager.livingBase) {
            /* Entity type check */
            if (ignoreSelf && entity == player) continue
            if (entity == mc.renderViewEntity) continue
            if (entity is EntityPlayer) {
                if (!player[0]) continue
                if (!playerTypeCheck(entity, player[1], player[2])) continue
            } else if (!mobTypeSettings(entity, mobs[0], mobs[1], mobs[2], mobs[3])) continue

            if (mc.player.isRiding && entity == mc.player.ridingEntity) continue // Riding entity check
            if (mc.player.distanceTo(entity) > range) continue // Distance check
            if (entity.health <= 0) continue // HP check
            if (!invisible && entity.isInvisible) continue
            entityList.add(entity)
        }
        return entityList
    }

    fun playerTypeCheck(player: EntityPlayer, friend: Boolean, sleeping: Boolean) =
        (friend || !FriendManager.isFriend(player.name))
            && (sleeping || !player.isPlayerSleeping)

    fun SafeClientEvent.getDroppedItems(itemId: Int, range: Float): ArrayList<EntityItem> {
        val entityList = ArrayList<EntityItem>()
        for (entity in EntityManager.entity) {
            if (entity !is EntityItem) continue /* Entites that are dropped item */
            if (entity.item.item.id != itemId) continue /* Dropped items that are has give item id */
            if (entity.distanceTo(player) > range) continue /* Entities within specified  blocks radius */

            entityList.add(entity)
        }
        return entityList
    }

    fun SafeClientEvent.getDroppedItem(itemId: Int, range: Float) =
        getDroppedItems(itemId, range)
            .minByOrNull { player.distanceTo(it) }
            ?.positionVector
            ?.toBlockPos()

    inline fun EntityPlayerSP.spoofSneak(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        if (!this.isSneaking) {
            connection.sendPacket(CPacketEntityAction(this, CPacketEntityAction.Action.START_SNEAKING))
            block.invoke()
            connection.sendPacket(CPacketEntityAction(this, CPacketEntityAction.Action.STOP_SNEAKING))
        } else {
            block.invoke()
        }
    }

    inline fun EntityPlayerSP.spoofUnSneak(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        if (this.isSneaking) {
            connection.sendPacket(CPacketEntityAction(this, CPacketEntityAction.Action.STOP_SNEAKING))
            block.invoke()
            connection.sendPacket(CPacketEntityAction(this, CPacketEntityAction.Action.START_SNEAKING))
        } else {
            block.invoke()
        }
    }
}