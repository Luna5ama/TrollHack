package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.event.events.*
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.Manager
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.SPacketBlockChange
import net.minecraft.util.SoundCategory
import net.minecraft.util.SoundEvent
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IWorldEventListener
import net.minecraft.world.World

object WorldManager : Manager(), IWorldEventListener {
    private val lock = Any()
    private val oldBlockStateMap = Object2ObjectOpenHashMap<BlockPos, IBlockState>()
    private val newBlockStateMap = Object2ObjectOpenHashMap<BlockPos, IBlockState>()
    private val timeoutMap = Object2LongOpenHashMap<BlockPos>()
    private val pendingPos = ObjectArrayList<BlockPos>()
    private val pendingOldState = ObjectArrayList<IBlockState>()
    private val pendingNewState = ObjectArrayList<IBlockState>()

    init {
        listener<ConnectionEvent.Disconnect> {
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
            pendingNewState.clear()
            pendingNewState.trim()
        }

        safeListener<PacketEvent.Receive> { event ->
            if (event.packet !is SPacketBlockChange) return@safeListener
            synchronized(lock) {
                val key = event.packet.blockPosition
                oldBlockStateMap.computeIfAbsent(key) { world.getBlockState(it) }
                newBlockStateMap[key] = event.packet.blockState
                timeoutMap[key] = System.currentTimeMillis() + 5L
            }
        }

        listener<RunGameLoopEvent.Tick> {
            synchronized(lock) {
                val iterator = timeoutMap.object2LongEntrySet().fastIterator()
                val current = System.currentTimeMillis()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (current > entry.longValue) {
                        val oldState = oldBlockStateMap.remove(entry.key)
                        val newState = newBlockStateMap.remove(entry.key)
                        oldState ?: continue
                        newState ?: continue
                        pendingPos.add(entry.key)
                        pendingOldState.add(oldState)
                        pendingNewState.add(newState)
                        iterator.remove()
                    }
                }
            }

            for (i in pendingPos.indices) {
                val pos = pendingPos[i]
                val oldState = pendingOldState[i]
                val newState = pendingNewState[i]
                WorldEvent.ServerBlockUpdate(pos, oldState, newState).post()
            }

            pendingPos.clear()
            pendingNewState.clear()
        }
    }

    override fun notifyBlockUpdate(
        worldIn: World,
        pos: BlockPos,
        oldState: IBlockState,
        newState: IBlockState,
        flags: Int
    ) {
        if (flags and 3 != 0) {
            WorldEvent.ClientBlockUpdate(pos, oldState, newState).post()
        }
    }

    override fun onEntityAdded(entityIn: Entity) {
        WorldEvent.Entity.Add(entityIn).post()
    }

    override fun onEntityRemoved(entityIn: Entity) {
        WorldEvent.Entity.Remove(entityIn).post()
    }

    override fun sendBlockBreakProgress(breakerId: Int, pos: BlockPos, progress: Int) {
        BlockBreakEvent(breakerId, pos, progress).post()
    }

    override fun notifyLightSet(pos: BlockPos) {

    }

    override fun markBlockRangeForRenderUpdate(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int) {
        WorldEvent.RenderUpdate(x1, y1, z1, x2, y2, z2).post()
    }

    override fun playSoundToAllNearExcept(
        player: EntityPlayer?,
        soundIn: SoundEvent,
        category: SoundCategory,
        x: Double,
        y: Double,
        z: Double,
        volume: Float,
        pitch: Float
    ) {

    }

    override fun playRecord(soundIn: SoundEvent, pos: BlockPos) {

    }

    override fun spawnParticle(
        particleID: Int,
        ignoreRange: Boolean,
        xCoord: Double,
        yCoord: Double,
        zCoord: Double,
        xSpeed: Double,
        ySpeed: Double,
        zSpeed: Double,
        vararg parameters: Int
    ) {

    }

    override fun spawnParticle(
        id: Int,
        ignoreRange: Boolean,
        minimiseParticleLevel: Boolean,
        x: Double,
        y: Double,
        z: Double,
        xSpeed: Double,
        ySpeed: Double,
        zSpeed: Double,
        vararg parameters: Int
    ) {

    }

    override fun broadcastSound(soundID: Int, pos: BlockPos, data: Int) {

    }

    @Suppress("WRONG_NULLABILITY_FOR_JAVA_OVERRIDE")
    override fun playEvent(player: EntityPlayer?, type: Int, blockPosIn: BlockPos, data: Int) {

    }
}