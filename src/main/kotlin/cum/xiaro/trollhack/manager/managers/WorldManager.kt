package cum.xiaro.trollhack.manager.managers

import cum.xiaro.trollhack.event.events.BlockBreakEvent
import cum.xiaro.trollhack.event.events.WorldEvent
import cum.xiaro.trollhack.manager.Manager
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.SoundCategory
import net.minecraft.util.SoundEvent
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IWorldEventListener
import net.minecraft.world.World

object WorldManager : Manager(), IWorldEventListener {
    override fun notifyBlockUpdate(worldIn: World, pos: BlockPos, oldState: IBlockState, newState: IBlockState, flags: Int) {
        if (flags and 3 != 0) {
            WorldEvent.BlockUpdate(pos, oldState, newState).post()
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

    override fun playSoundToAllNearExcept(player: EntityPlayer?, soundIn: SoundEvent, category: SoundCategory, x: Double, y: Double, z: Double, volume: Float, pitch: Float) {

    }

    override fun playRecord(soundIn: SoundEvent, pos: BlockPos) {

    }

    override fun spawnParticle(particleID: Int, ignoreRange: Boolean, xCoord: Double, yCoord: Double, zCoord: Double, xSpeed: Double, ySpeed: Double, zSpeed: Double, vararg parameters: Int) {

    }

    override fun spawnParticle(id: Int, ignoreRange: Boolean, minimiseParticleLevel: Boolean, x: Double, y: Double, z: Double, xSpeed: Double, ySpeed: Double, zSpeed: Double, vararg parameters: Int) {

    }

    override fun broadcastSound(soundID: Int, pos: BlockPos, data: Int) {

    }

    override fun playEvent(player: EntityPlayer?, type: Int, blockPosIn: BlockPos, data: Int) {

    }
}