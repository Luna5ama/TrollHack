package me.luna.trollhack.module.modules.misc

import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.PacketEvent
import me.luna.trollhack.event.safeConcurrentListener
import me.luna.trollhack.gui.hudgui.elements.client.Notification
import me.luna.trollhack.manager.managers.EntityManager
import me.luna.trollhack.manager.managers.FriendManager
import me.luna.trollhack.manager.managers.HoleManager
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.module.modules.combat.Burrow
import me.luna.trollhack.util.EntityUtils.betterPosition
import me.luna.trollhack.util.interfaces.DisplayEnum
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.SPacketBlockBreakAnim
import kotlin.math.abs

internal object BreakWarning : Module(
    name = "BreakWarning",
    description = "Sends warning when someone breaking blocks near you",
    category = Category.MISC
) {
    private val ignoreFriends by setting("Ignore Friends", true)
    private val burrow by setting("Burrow", BlockMode.ANY)
    private val surround by setting("Surround", BlockMode.ANY)
    private val nearby by setting("Nearby", BlockMode.OFF)
    private val range by setting("Range", 8, 1..16, 1, { nearby != BlockMode.OFF })

    private enum class BlockMode(override val displayName: CharSequence) : DisplayEnum {
        OFF("Off") {
            override fun checkBlock(block: Block): Boolean {
                return false
            }
        },
        ANY("Any") {
            override fun checkBlock(block: Block): Boolean {
                return true
            }
        },
        OBSIDIAN("Obsidian") {
            override fun checkBlock(block: Block): Boolean {
                return block == Blocks.OBSIDIAN
            }
        };

        abstract fun checkBlock(block: Block): Boolean
    }

    init {
        safeConcurrentListener<PacketEvent.PostReceive> {
            if (it.packet !is SPacketBlockBreakAnim) return@safeConcurrentListener
            val player = getPlayer(it.packet.breakerId) ?: return@safeConcurrentListener

            if (trySendBurrow(it.packet, player)) return@safeConcurrentListener
            if (trySendSurround(it.packet, player)) return@safeConcurrentListener
            if (trySendNearby(it.packet, player)) return@safeConcurrentListener
        }
    }

    private fun SafeClientEvent.getPlayer(id: Int): EntityPlayer? {
        if (id == player.entityId) return null
        val entity = EntityManager.getPlayerByID(id) ?: return null
        if (ignoreFriends && FriendManager.isFriend(entity.name)) return null
        return entity
    }

    private fun SafeClientEvent.trySendBurrow(packet: SPacketBlockBreakAnim, player: EntityPlayer): Boolean {
        if (burrow == BlockMode.OFF) return false

        val blockState = world.getBlockState(packet.position)
        if (!burrow.checkBlock(blockState.block)) return false

        val box = blockState.getCollisionBoundingBox(world, packet.position) ?: return false
        if (!player.entityBoundingBox.intersects(box.offset(packet.position))) return false

        Notification.send(hash(packet, 1), "${player.name} is breaking your burrow!")

        return true
    }

    private fun SafeClientEvent.trySendSurround(packet: SPacketBlockBreakAnim, player: EntityPlayer): Boolean {
        if (surround == BlockMode.OFF) return false

        val blockState = world.getBlockState(packet.position)
        if (!surround.checkBlock(blockState.block)) return false

        val holeInfo = HoleManager.getHoleInfo(player)

        if (holeInfo.isHole) {
            if (!holeInfo.surroundPos.contains(packet.position)) return false
        } else {
            val playerPos = player.betterPosition
            val xDiff = abs(packet.position.x - playerPos.x)
            val zDiff = abs(packet.position.z - playerPos.z)
            if (packet.position.y != playerPos.y || xDiff > 1 || zDiff > 1 || xDiff + zDiff != 1) return false
        }

        Notification.send(hash(packet, 2), "${player.name} is breaking your surround!")

        return true
    }

    private fun SafeClientEvent.trySendNearby(packet: SPacketBlockBreakAnim, player: EntityPlayer): Boolean {
        if (nearby == BlockMode.OFF) return false

        val blockState = world.getBlockState(packet.position)
        if (!nearby.checkBlock(blockState.block)) return false

        if (player.getDistanceSq(packet.position) > range * range) return false

        Notification.send(hash(packet, 3), "${player.name} is breaking block near you!")

        return true
    }

    private fun hash(packet: SPacketBlockBreakAnim, type: Int): Long {
        var result = Burrow.hashCode().toLong()
        result = 31L * result + packet.breakerId.hashCode().toLong()
        result = 31L * result + type.hashCode().toLong()
        return result
    }
}