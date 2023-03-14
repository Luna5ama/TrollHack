package me.luna.trollhack.module.modules.combat

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import me.luna.trollhack.event.events.PacketEvent
import me.luna.trollhack.event.events.TickEvent
import me.luna.trollhack.event.events.WorldEvent
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.event.safeParallelListener
import me.luna.trollhack.gui.hudgui.elements.client.Notification
import me.luna.trollhack.manager.managers.EntityManager
import me.luna.trollhack.manager.managers.HotbarManager.serverSideItem
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.module.modules.player.PacketMine
import me.luna.trollhack.util.EntityUtils.isFakeOrSelf
import me.luna.trollhack.util.EntityUtils.isFriend
import me.luna.trollhack.util.extension.sq
import me.luna.trollhack.util.items.block
import me.luna.trollhack.util.math.vector.distanceSqTo
import me.luna.trollhack.util.world.getBlock
import net.minecraft.block.BlockShulkerBox
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.tileentity.TileEntityShulkerBox
import net.minecraft.util.math.BlockPos

internal object AntiRegear : Module(
    name = "AntiRegear",
    description = "Prevents enemy from regearing using shulkers",
    category = Category.COMBAT,
    modulePriority = 500
) {
    private val newBlockOnly by setting("New Block Only", false)
    private val ignoreSelfPlaced by setting("Ignore Self Placed", true)
    private val selfRange by setting("Self Range", 1.5f, 0.0f..10.0f, 0.1f)
    private val friendRange by setting("Friend Range", 1.5f, 0.0f..10.0f, 0.1f)
    private val otherPlayerRange by setting("Other Player Range", 4.0f, 0.0f..10.0f, 0.1f)
    private val maxDetectRange by setting("Max Detect Range", 16, 1..64, 1)

    private val selfPlaced = ObjectLinkedOpenHashSet<BlockPos>()
    private val mineQueue = ObjectLinkedOpenHashSet<BlockPos>()

    init {
        onDisable {
            synchronized(selfPlaced) {
                selfPlaced.clear()
            }
            mineQueue.clear()
        }

        safeListener<PacketEvent.PostSend> {
            if (it.packet !is CPacketPlayerTryUseItemOnBlock) return@safeListener
            if (player.serverSideItem.item.block !is BlockShulkerBox) return@safeListener

            addSelfPlaced(it.packet.pos.offset(it.packet.direction))
        }

        safeParallelListener<TickEvent.Post> {
            PacketMine.enable()

            synchronized(selfPlaced) {
                selfPlaced.removeIf {
                    world.getBlock(it) !is BlockShulkerBox
                }
            }

            var pos: BlockPos? = null
            val maxDetectRangeSq = maxDetectRange.sq

            if (!newBlockOnly) {
                world.loadedTileEntityList.asSequence()
                    .filterIsInstance<TileEntityShulkerBox>()
                    .filter { player.distanceSqTo(it.pos) <= maxDetectRangeSq }
                    .filter { otherPlayerNearBy(it.pos) }
                    .filter { !ignoreSelfPlaced || !selfPlaced.contains(it.pos) }
                    .mapTo(mineQueue) { it.pos }
            }

            while (!mineQueue.isEmpty()) {
                pos = mineQueue.first()
                if (player.distanceSqTo(pos) > maxDetectRangeSq) {
                    mineQueue.removeFirst()
                } else {
                    break
                }
            }

            if (pos == null) {
                PacketMine.reset(AntiRegear)
            } else {
                PacketMine.mineBlock(AntiRegear, pos, AntiRegear.modulePriority)
            }
        }

        safeListener<WorldEvent.ClientBlockUpdate> { event ->
            val playerDistance = player.distanceSqTo(event.pos)
            if (playerDistance > maxDetectRange.sq) return@safeListener

            val currentMinePos = if (mineQueue.isEmpty()) null else mineQueue.first()

            if (currentMinePos == event.pos && event.newState.block !is BlockShulkerBox) {
                mineQueue.removeFirst()
                return@safeListener
            }

            if (event.newState.block is BlockShulkerBox) {
                if (ignoreSelfPlaced && selfPlaced.contains(event.pos)) return@safeListener
                if (playerDistance <= selfRange.sq) return@safeListener

                val friendRangeSq = friendRange.sq
                if (friendRangeSq > 0.0f && EntityManager.players.asSequence()
                        .filter { it.isFriend }
                        .filter { it.distanceSqTo(event.pos) <= friendRangeSq }
                        .any()
                ) return@safeListener

                if (!otherPlayerNearBy(event.pos)) return@safeListener

                mineQueue.add(event.pos)
            }
        }
    }

    private fun otherPlayerNearBy(
        pos: BlockPos
    ): Boolean {
        val otherPlayerRangeSq = otherPlayerRange.sq
        return EntityManager.players.asSequence()
            .filter { !it.isFakeOrSelf }
            .filter { !it.isFriend }
            .filter { it.distanceSqTo(pos) <= otherPlayerRangeSq }
            .any()
    }

    private fun addSelfPlaced(pos: BlockPos) {
        synchronized(selfPlaced) {
            if (selfPlaced.size > 10) selfPlaced.removeLast()
            selfPlaced.addAndMoveToFirst(pos)
        }
    }
}