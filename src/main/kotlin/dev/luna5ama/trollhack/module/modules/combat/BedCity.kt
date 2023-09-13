package dev.luna5ama.trollhack.module.modules.combat

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.HoleManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.math.VectorUtils.setAndAdd
import dev.luna5ama.trollhack.util.world.canBreakBlock
import dev.luna5ama.trollhack.util.world.checkBlockCollision
import dev.luna5ama.trollhack.util.world.isAir
import dev.luna5ama.trollhack.util.world.isFullBox
import net.minecraft.block.BlockBed
import net.minecraft.block.BlockConcretePowder
import net.minecraft.block.BlockFalling
import net.minecraft.block.BlockSand
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

internal object BedCity : Module(
    name = "Bed City",
    category = Category.COMBAT,
    description = "Auto city for bed pvp",
    modulePriority = 100
) {
    private val ignoreNonFullBox by setting("Ignore Non-Full Box", true)

    private var lastSurrounded = false
    private var lastTargetPos: BlockPos? = null
    private var lastMinePos: BlockPos? = null

    private val facings = arrayOf(
        EnumFacing.WEST,
        EnumFacing.NORTH,
        EnumFacing.EAST,
        EnumFacing.SOUTH
    )

    override fun isActive(): Boolean {
        return isEnabled && lastMinePos != null
    }

    init {
        onEnable {
            enable()
        }

        onDisable {
            lastSurrounded = false
            lastTargetPos = null
            lastMinePos = null
            PacketMine.reset(this)
        }

        safeParallelListener<TickEvent.Post> {
            run()
        }
    }

    private fun SafeClientEvent.run() {
        val target = CombatManager.target ?: return
        val targetPos = target.betterPosition
        val minePos = BlockPos.MutableBlockPos()

        val currentSurrounded = HoleManager.getHoleInfo(targetPos).isHole
        val surrounded = currentSurrounded && lastSurrounded
        lastSurrounded = currentSurrounded

        val diffX = target.posX - (targetPos.x + 0.5)
        val diffZ = target.posZ - (targetPos.z + 0.5)
        facings.sortWith(
            compareByDescending<EnumFacing> {
                lastMinePos == minePos.setAndAdd(targetPos, it)
            }.thenBy {
                val directionVec = it.directionVec
                diffX * directionVec.x + diffZ * directionVec.z
            }
        )

        fun checkEmpty(pos: BlockPos): Boolean {
            if (!world.canBreakBlock(pos)) return true

            val blockState = world.getBlockState(pos)
            val block = blockState.block
            if (block is BlockBed) return true
            if (block is BlockFalling) return true

            return if (ignoreNonFullBox) !world.getBlockState(pos).isFullBox else world.isAir(pos)
        }

        fun minePos(minePos: BlockPos?): Boolean {
            if (minePos == null) return false
            PacketMine.mineBlock(BedCity, minePos, if (checkEmpty(targetPos)) -100 else modulePriority)
            lastTargetPos = targetPos
            lastMinePos = minePos
            return true
        }

        fun tryMine(pos: BlockPos): Boolean {
            if (checkEmpty(pos)) return false

            minePos(pos)
            return true
        }

        fun tryMineSurround(pos: BlockPos): Boolean {
            if (checkEmpty(pos)) return false
            if (!surrounded && !world.checkBlockCollision(pos, target.entityBoundingBox)) return false

            minePos(pos)
            return true
        }

        fun tryHeadMineSurround(pos: BlockPos): Boolean {
            if (checkEmpty(pos)) return false
            if (!world.checkBlockCollision(pos, target.entityBoundingBox)) return false

            minePos(pos)
            return true
        }

        tryMine(minePos.setPos(targetPos))
            || tryMine(minePos.setAndAdd(targetPos, EnumFacing.UP))
            || tryMineSurround(minePos.setAndAdd(targetPos, facings[0]))
            || tryMineSurround(minePos.setAndAdd(targetPos, facings[1]))
            || tryMineSurround(minePos.setAndAdd(targetPos, facings[2]))
            || tryMineSurround(minePos.setAndAdd(targetPos, facings[3]))
            || tryHeadMineSurround(minePos.setAndAdd(targetPos, facings[0]).move(EnumFacing.UP))
            || tryHeadMineSurround(minePos.setAndAdd(targetPos, facings[1]).move(EnumFacing.UP))
            || tryHeadMineSurround(minePos.setAndAdd(targetPos, facings[2]).move(EnumFacing.UP))
            || tryHeadMineSurround(minePos.setAndAdd(targetPos, facings[3]).move(EnumFacing.UP))
            || minePos(lastMinePos)
    }
}
