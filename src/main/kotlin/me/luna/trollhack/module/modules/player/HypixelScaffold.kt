package me.luna.trollhack.module.modules.player

import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.WorldEvent
import me.luna.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import me.luna.trollhack.event.events.render.Render3DEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.manager.managers.HotbarManager.resetHotbar
import me.luna.trollhack.manager.managers.HotbarManager.spoofHotbar
import me.luna.trollhack.manager.managers.PlayerPacketManager
import me.luna.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.graphics.ESPRenderer
import me.luna.trollhack.util.graphics.color.ColorRGB
import me.luna.trollhack.util.inventory.slot.HotbarSlot
import me.luna.trollhack.util.inventory.slot.firstItem
import me.luna.trollhack.util.inventory.slot.hotbarSlots
import me.luna.trollhack.util.items.blockBlacklist
import me.luna.trollhack.util.math.RotationUtils.legitYaw
import me.luna.trollhack.util.math.RotationUtils.yaw
import me.luna.trollhack.util.math.vector.Vec2f
import me.luna.trollhack.util.threads.runSafe
import me.luna.trollhack.util.world.*
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos

@Suppress("NOTHING_TO_INLINE")
internal object HypixelScaffold : Module(
    name = "HypixelScaffold",
    category = Category.PLAYER,
    description = "Scaffold bypass for hypixel",
    modulePriority = 5000
) {
    private val desyncPlacement by setting("Desync Placement", true)
    private val delay by setting("Delay", 2, 1..20, 1)
    private val placeTimeout by setting("Place Timeout", 4, 1..20, 1)
    private val rotationTimeout by setting("Rotation Timeout", 10, 1..20, 1)

    private val renderer = ESPRenderer()

    private var lastPos: BlockPos? = null
    private var placeInfo: PlaceInfo? = null
    private var ticks = 0
    private var lastRotation: Vec2f? = null
    private var switchBack = false

    override fun isActive(): Boolean {
        return isEnabled && placeInfo != null
    }

    init {
        onDisable {
            if (switchBack) {
                runSafe {
                    resetHotbar()
                }
            }

            lastPos = null
            placeInfo = null
            ticks = 0
            lastRotation = null
            switchBack = false
        }

        listener<WorldEvent.ServerBlockUpdate> {
            if (it.pos == lastPos && !it.newState.isReplaceable) {
                lastPos = null
            }
        }

        listener<Render3DEvent> {
            renderer.render(false)
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            ticks++
            renderer.clear()

            val slot = player.hotbarSlots.firstItem<ItemBlock, HotbarSlot>()
            val newPlaceInfo = slot?.let {
                getPlaceInfo()
            }

            if (newPlaceInfo != null) {
                spoofHotbar(slot)
                switchBack = true
                renderer.add(newPlaceInfo.placedPos, ColorRGB(32, 255, 32))
                lastRotation = Vec2f(player.legitYaw(newPlaceInfo.side.opposite.yaw), 82.5f)
            } else if (ticks > 3 && switchBack) {
                resetHotbar()
                switchBack = false
            }

            lastRotation?.let {
                if (newPlaceInfo != null || ticks <= rotationTimeout) {
                    sendPlayerPacket {
                        rotate(it)
                    }
                }
            }

            placeInfo = newPlaceInfo
        }

        safeListener<OnUpdateWalkingPlayerEvent.Post> {
            if (PlayerPacketManager.prevRotation.y == 82.5f && PlayerPacketManager.rotation.y == 82.5f) {
                placeInfo?.let { placeInfo ->
                    player.hotbarSlots.firstItem<ItemBlock, HotbarSlot>()?.let {
                        doPlace(placeInfo, it)
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.getPlaceInfo(): PlaceInfo? {
        val posDown = BlockPos(player.posX, player.posY - 1.0, player.posZ)
        return if (world.getBlockState(posDown).isReplaceable) {
            getNeighbor(posDown, 1)
                ?: getNeighbor(posDown, 2, sides = EnumFacing.HORIZONTALS)
        } else {
            null
        }
    }

    private fun SafeClientEvent.doPlace(placeInfo: PlaceInfo, slot: HotbarSlot) {
        if (ticks < delay) return
        if (!desyncPlacement && lastPos == placeInfo.placedPos && ticks < placeTimeout) return

        if (!world.isPlaceable(placeInfo.placedPos)) return


        val itemStack = slot.stack
        val block = (itemStack.item as? ItemBlock?)?.block ?: return
        val metaData = itemStack.metadata
        val blockState = block.getStateForPlacement(world, placeInfo.pos, placeInfo.side, placeInfo.hitVecOffset.x, placeInfo.hitVecOffset.y, placeInfo.hitVecOffset.z, metaData, player, EnumHand.MAIN_HAND)
        val soundType = blockState.block.getSoundType(blockState, world, placeInfo.pos, player)

        val sneak = !player.isSneaking && blockBlacklist.contains(world.getBlock(placeInfo.pos))
        if (sneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))

        spoofHotbar(slot)
        switchBack = true
        connection.sendPacket(placeInfo.toPlacePacket(EnumHand.MAIN_HAND))
        player.swingArm(EnumHand.MAIN_HAND)

        if (sneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))

        world.playSound(player, placeInfo.pos, soundType.placeSound, SoundCategory.BLOCKS, (soundType.getVolume() + 1.0f) / 2.0f, soundType.getPitch() * 0.8f)
        if (desyncPlacement) world.setBlockState(placeInfo.placedPos, blockState)

        ticks = 0
        lastPos = placeInfo.placedPos
    }
}