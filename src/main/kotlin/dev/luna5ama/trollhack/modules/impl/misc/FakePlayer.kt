/*
 * Copyright (c) 2021-2022, SagiriXiguajerry. All rights reserved.
 * This repository will be transformed to SuperMic_233.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package dev.luna5ama.trollhack.modules.impl.misc


import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLists
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.event.impl.world.ConnectionEvent
import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.EntityFakePlayer
import dev.luna5ama.trollhack.utils.Helper
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.extension.entry
import dev.luna5ama.trollhack.utils.extension.getValue
import dev.luna5ama.trollhack.utils.extension.setValue
import dev.luna5ama.trollhack.utils.extension.velocityX
import dev.luna5ama.trollhack.utils.extension.velocityY
import dev.luna5ama.trollhack.utils.extension.velocityZ
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.runSafe
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.resources.ResourceKey
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.phys.Vec3
import java.util.concurrent.atomic.AtomicInteger


internal object FakePlayer : Module("Fake Player", category = Category.MISC), Helper {
    private val copyInventory by setting("Copy Inventory", true)
    private val copyPotions by setting("Copy Potions", true)
    private val maxArmor by setting("Max Armor", false)
    private val gappleEffects by setting("Gapple Effects", false)
    private val playerName by setting("Player Name", "Nilquadium")
    private val record by setting("Record", false)
    private val play by setting("Play", false)
    private val recordList = ObjectLists.synchronize(ObjectArrayList<PositionRecord>())
    private var pointer by AtomicInteger(0)

    @Volatile
    private var recording = false

    private const val ENTITY_ID = -696969420
    private var fakePlayer: EntityFakePlayer? = null

    override fun getDisplayInfo(): Any {
        return playerName
    }

    init {
        onEnabled {
            runSafe {
                spawnFakePlayer()
                recording = record
            } ?: disable()
            if (play && record) disable()
            else if (record) recordList.clear()
            else if (play) pointer = 0
        }

        onDisabled {
            runCatching {
                if (fakePlayer != null) {
//                    fakePlayer!!.kill()
                    fakePlayer!!.setRemoved(Entity.RemovalReason.KILLED)
                    fakePlayer!!.onClientRemoval()
                    fakePlayer = null
                }
            }
            if (play) pointer = 0
        }

        handler<ConnectionEvent.Disconnect> {
            disable()
        }

        nonNullHandler<TickEvent.Pre> {
            runSafe {
                if (recording) {
                    recordList.add(
                        PositionRecord(
                            player.position(),
                            Vec3(player.velocityX, player.velocityY, player.velocityZ),
                            Vec2f(player.yBob, player.xBob)
                        )
                    )
                } else if (play) {
                    if (pointer !in recordList.indices) {
                        pointer = 0
                        return@nonNullHandler
                    }
                    val current = recordList[pointer]
                    val fakePlayer = fakePlayer ?: return@nonNullHandler

                    fakePlayer.snapTo(
                        current.pos.x, current.pos.y, current.pos.z,
                        current.rotation.x, current.rotation.y
                    )
                    fakePlayer.setDeltaMovement(current.motion.x, current.motion.y, current.motion.z)
                    pointer++
                    if (pointer == recordList.size) pointer = 0
                    else {
                    }
                } else {
                }
            }
        }

        handler<WorldEvent.Load> {
            disable()
        }

        nonNullHandler<PacketEvent.Send>(Int.MIN_VALUE) {
            when (it.packet) {
                is ServerboundInteractPacket -> {
                    val packet = it.packet
                    if (packet.action.type == ServerboundInteractPacket.ActionType.ATTACK
                        && packet.entityId == ENTITY_ID
                    ) {
                        it.cancel()
                    }
                }
            }
        }
    }

    private fun NonNullContext.spawnFakePlayer() {
        fakePlayer = EntityFakePlayer(world, playerName).apply {
            val viewEntity = mc.cameraEntity ?: player
            copyPosition(viewEntity)

            yHeadRot = viewEntity.yHeadRot
            if (copyInventory) inventory.replaceWith(player.inventory)
            if (copyPotions) copyPotions(player)
            if (maxArmor) addMaxArmor()
            if (gappleEffects) addGappleEffects()
        }.also {
            runSafe {
                world.addEntity(it)
            }
        }
    }

    private fun Player.copyPotions(otherPlayer: Player) {
        for ((effectType, potionEffect) in otherPlayer.activeEffectsMap) {
            addPotionEffectForce(MobEffectInstance(effectType, Int.MAX_VALUE, potionEffect.amplifier))
        }
    }

    private fun Player.addMaxArmor() {
        setItemSlot(EquipmentSlot.HEAD, ItemStack(Items.DIAMOND_HELMET).apply {
            addMaxEnchantment(Enchantments.PROTECTION)
            addMaxEnchantment(Enchantments.UNBREAKING)
            addMaxEnchantment(Enchantments.RESPIRATION)
            addMaxEnchantment(Enchantments.AQUA_AFFINITY)
            addMaxEnchantment(Enchantments.MENDING)
        })

        setItemSlot(EquipmentSlot.CHEST, ItemStack(Items.DIAMOND_CHESTPLATE).apply {
            addMaxEnchantment(Enchantments.PROTECTION)
            addMaxEnchantment(Enchantments.UNBREAKING)
            addMaxEnchantment(Enchantments.MENDING)
        })

        setItemSlot(EquipmentSlot.LEGS, ItemStack(Items.DIAMOND_LEGGINGS).apply {
            addMaxEnchantment(Enchantments.BLAST_PROTECTION)
            addMaxEnchantment(Enchantments.UNBREAKING)
            addMaxEnchantment(Enchantments.MENDING)
        })

        setItemSlot(EquipmentSlot.FEET, ItemStack(Items.DIAMOND_BOOTS).apply {
            addMaxEnchantment(Enchantments.PROTECTION)
            addMaxEnchantment(Enchantments.FEATHER_FALLING)
            addMaxEnchantment(Enchantments.DEPTH_STRIDER)
            addMaxEnchantment(Enchantments.UNBREAKING)
            addMaxEnchantment(Enchantments.MENDING)
        })
    }

    private fun ItemStack.addMaxEnchantment(enchantment0: ResourceKey<Enchantment>) {
        val enchantment = enchantment0.entry
        enchant(enchantment, enchantment.value().maxLevel)
    }

    private fun Player.addGappleEffects() {
        addPotionEffectForce(MobEffectInstance(MobEffects.REGENERATION, Int.MAX_VALUE, 1))
        addPotionEffectForce(MobEffectInstance(MobEffects.ABSORPTION, Int.MAX_VALUE, 3))
        addPotionEffectForce(MobEffectInstance(MobEffects.RESISTANCE, Int.MAX_VALUE, 0))
        addPotionEffectForce(MobEffectInstance(MobEffects.FIRE_RESISTANCE, Int.MAX_VALUE, 0))
    }

    private fun Player.addPotionEffectForce(potionEffect: MobEffectInstance) {
        addEffect(potionEffect)
        potionEffect.effect.value().onEffectStarted(this, potionEffect.amplifier)
    }

    private data class PositionRecord(val pos: Vec3, val motion: Vec3, val rotation: Vec2f)
}
