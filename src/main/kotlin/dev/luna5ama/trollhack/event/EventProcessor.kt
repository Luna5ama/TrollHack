package dev.luna5ama.trollhack.event

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.LoopEvent
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.event.impl.player.PlayerPopEvent
import dev.luna5ama.trollhack.event.impl.world.ConnectionEvent
import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.world.entity.EntityEvent
import net.minecraft.world.entity.player.Player

// May be used in the future
object EventProcessor : AlwaysListening {
    private val lock = Any()
    private val oldBlockStateMap = Object2ObjectOpenHashMap<BlockPos, BlockState>()
    private val newBlockStateMap = Object2ObjectOpenHashMap<BlockPos, BlockState>()
    private val timeoutMap = Object2LongOpenHashMap<BlockPos>()
    private val pendingPos = ObjectArrayList<BlockPos>()
    private val pendingOldState = ObjectArrayList<BlockState>()
    private val pendingNewState = ObjectArrayList<BlockState>()

    init {
        handler<ConnectionEvent.Disconnect> {
            synchronized(lock) {
                oldBlockStateMap.clear()
                oldBlockStateMap.trim()
                newBlockStateMap.clear()
                newBlockStateMap.trim()
                timeoutMap.clear()
                timeoutMap.trim()
            }

            pendingPos.clear()
            pendingPos.trim()
            pendingOldState.clear()
            pendingOldState.trim()
            pendingNewState.clear()
            pendingNewState.trim()
        }

        nonNullHandler<PacketEvent.Receive> { event ->
            when (val packet = event.packet) {
                is ClientboundEntityEventPacket -> {
                    val entity = packet.getEntity(world)
                    if (packet.eventId == EntityEvent.PROTECTED_FROM_DEATH && entity is Player) {
                        PlayerPopEvent(entity).post()
                    }
                }
            }
            if (event.packet !is ClientboundBlockUpdatePacket) return@nonNullHandler
            synchronized(lock) {
                val key = event.packet.pos
                oldBlockStateMap[key] = world.getBlockState(key)
                newBlockStateMap[key] = event.packet.blockState
                timeoutMap[key] = System.currentTimeMillis()
            }
        }

        handler<LoopEvent.Tick> {
            synchronized(lock) {
                val iterator = timeoutMap.object2LongEntrySet().fastIterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val oldState = oldBlockStateMap.remove(entry.key)
                    val newState = newBlockStateMap.remove(entry.key)
                    oldState ?: continue
                    newState ?: continue
                    pendingPos.add(entry.key)
                    pendingOldState.add(oldState)
                    pendingNewState.add(newState)
                }
                timeoutMap.clear()
                timeoutMap.trim()
            }

            for (i in pendingPos.indices) {
                val pos = pendingPos[i]
                val oldState = pendingOldState[i]
                val newState = pendingNewState[i]
                WorldEvent.ServerBlockUpdate(pos, oldState, newState).post()
            }

            pendingPos.clear()
            pendingOldState.clear()
            pendingNewState.clear()
        }
    }
}