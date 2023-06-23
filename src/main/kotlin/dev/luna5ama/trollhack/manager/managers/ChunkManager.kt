package dev.luna5ama.trollhack.manager.managers

import dev.fastmc.common.TimeUnit
import dev.fastmc.common.floorToInt
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.util.delegate.AsyncCachedValue
import dev.luna5ama.trollhack.util.threads.BackgroundScope
import io.netty.util.internal.ConcurrentSet
import kotlinx.coroutines.launch
import net.minecraft.network.play.server.SPacketChunkData
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.chunk.Chunk

object ChunkManager : Manager() {
    private val newChunks0 = ConcurrentSet<ChunkPos>()
    val newChunks by AsyncCachedValue(1L, TimeUnit.SECONDS) {
        newChunks0.toList()
    }

    init {
        listener<ConnectionEvent.Disconnect> {
            newChunks0.clear()
        }

        safeListener<PacketEvent.PostReceive> { event ->
            if (event.packet !is SPacketChunkData || event.packet.isFullChunk) return@safeListener

            BackgroundScope.launch {
                val chunk = world.getChunk(event.packet.chunkX, event.packet.chunkZ)
                if (chunk.isEmpty) return@launch

                if (newChunks0.add(chunk.pos)) {
                    if (newChunks0.size > 8192) {
                        val playerX = player.posX.floorToInt()
                        val playerZ = player.posZ.floorToInt()

                        newChunks0.maxByOrNull {
                            (playerX - it.x) + (playerZ - it.z)
                        }?.let {
                            newChunks0.remove(it)
                        }
                    }
                }
            }
        }
    }

    fun isNewChunk(chunk: Chunk) = isNewChunk(chunk.pos)

    fun isNewChunk(chunkPos: ChunkPos) = newChunks0.contains(chunkPos)
}