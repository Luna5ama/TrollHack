package cum.xiaro.trollhack.manager.managers

import cum.xiaro.trollhack.util.extension.fastFloor
import cum.xiaro.trollhack.event.events.ConnectionEvent
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.Manager
import cum.xiaro.trollhack.util.TimeUnit
import cum.xiaro.trollhack.util.delegate.AsyncCachedValue
import cum.xiaro.trollhack.util.threads.defaultScope
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
            defaultScope.launch {
                if (event.packet !is SPacketChunkData || event.packet.isFullChunk) return@launch
                val chunk = world.getChunk(event.packet.chunkX, event.packet.chunkZ)
                if (chunk.isEmpty) return@launch

                if (newChunks0.add(chunk.pos)) {
                    if (newChunks0.size > 8192) {
                        val playerX = player.posX.fastFloor()
                        val playerZ = player.posZ.fastFloor()

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