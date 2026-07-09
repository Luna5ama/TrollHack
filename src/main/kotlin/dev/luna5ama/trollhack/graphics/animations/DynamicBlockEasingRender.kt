package dev.luna5ama.trollhack.graphics.animations

import dev.luna5ama.trollhack.utils.extension.component1
import dev.luna5ama.trollhack.utils.extension.component2
import dev.luna5ama.trollhack.utils.extension.component3
import dev.luna5ama.trollhack.utils.runSafe
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

open class DynamicBlockEasingRender(pos: BlockPos, movingLength: () -> Float, fadingLength: () -> Float, moveType: () -> Easing, fadingType: () -> Easing) {
    var lastPos: BlockPos; private set
    var newPos: BlockPos; private set
    private val offset: Vec3
        get() = Vec3((newPos.x - lastPos.x).toDouble(), (newPos.y - lastPos.y).toDouble(), (newPos.z - lastPos.z).toDouble())

    private val lastBB: AABB
        get() = lastPos.bb
    private val newBB: AABB
        get() = newPos.bb
    @Suppress("unused")
    private val offsetBB: AABB
        get() = AABB(
            lastBB.minX - newBB.minX,
            lastBB.minY - newBB.minY,
            lastBB.minZ - newBB.minZ,
            lastBB.maxX - newBB.maxX,
            lastBB.maxY - newBB.maxY,
            lastBB.maxZ - newBB.maxZ)

    private val animationX: DynamicAnimationFlag
    private val animationY: DynamicAnimationFlag
    private val animationZ: DynamicAnimationFlag
    private val animationSize: DynamicAnimationFlag

    var isEnded = false
        private set
    private var size = 0.5

    @Suppress("unused")
    private val Double.abs: Double
        get() {
            return abs(this)
        }

    private val BlockPos.bb: AABB
        get() {
            val world = runSafe { world }!!
            return world.getBlockState(this).getShape(world, this).bounds()
        }

    init {
        lastPos = pos
        newPos = pos
        isEnded = true
        animationX = DynamicAnimationFlag(moveType, movingLength)
        animationY = DynamicAnimationFlag(moveType, movingLength)
        animationZ = DynamicAnimationFlag(moveType, movingLength)
        animationSize = DynamicAnimationFlag(fadingType, fadingLength)
    }

    fun updatePos(pos: BlockPos) {
        lastPos = newPos
        newPos = pos
    }

    fun updateVec3Box(): FullRenderInfo {
        val vec3d = getUpdate()
        val (x, y, z) = vec3d
        size = animationSize.getAndUpdate(if (isEnded) 0f else 50f).toDouble()
        val axisAlignedBB = AABB(
            x,
            y,
            z,
            x + 1,
            y + 1,
            z + 1
        )
        val centerX = axisAlignedBB.minX + (axisAlignedBB.maxX - axisAlignedBB.minX) / 2
        val centerY = axisAlignedBB.minY + (axisAlignedBB.maxY - axisAlignedBB.minY) / 2
        val centerZ = axisAlignedBB.minZ + (axisAlignedBB.maxZ - axisAlignedBB.minZ) / 2
        return FullRenderInfo(
            AABB(
            centerX + size / 100,
            centerY + size / 100,
            centerZ + size / 100,
            centerX - size / 100,
            centerY - size / 100,
            centerZ - size / 100
        ), vec3d)
    }

    fun getFullUpdate(): AABB {
        val (x, y, z) = getUpdate()
        size = animationSize.getAndUpdate(if (isEnded) 0f else 50f).toDouble()
        val axisAlignedBB = AABB(
            x,
            y,
            z,
            x + 1,
            y + 1,
            z + 1
        )
        val centerX = axisAlignedBB.minX + (axisAlignedBB.maxX - axisAlignedBB.minX) / 2
        val centerY = axisAlignedBB.minY + (axisAlignedBB.maxY - axisAlignedBB.minY) / 2
        val centerZ = axisAlignedBB.minZ + (axisAlignedBB.maxZ - axisAlignedBB.minZ) / 2
        return AABB(
            centerX + size / 100,
            centerY + size / 100,
            centerZ + size / 100,
            centerX - size / 100,
            centerY - size / 100,
            centerZ - size / 100
        )
    }

    fun getUpdate(): Vec3 {
        return Vec3(
            animationX.getAndUpdate(offset.x.toFloat() + lastPos.x).toDouble(),
            animationY.getAndUpdate(offset.y.toFloat() + lastPos.y).toDouble(),
            animationZ.getAndUpdate(offset.z.toFloat() + lastPos.z).toDouble()
        )
    }

    fun reset() {
        this.animationX.forceUpdate(0f, 0f)
        this.animationY.forceUpdate(0f, 0f)
        this.animationZ.forceUpdate(0f, 0f)
        this.animationSize.forceUpdate(0f, 0f)
        this.isEnded = true
        this.size = 0.0
        this.newPos = BlockPos(0, 0, 0)
        this.lastPos = newPos
    }

    @JvmOverloads
    fun forceUpdatePos(pos: BlockPos, resetSize: Boolean = false) {
        this.newPos = pos
        this.lastPos = newPos
        this.animationX.forceUpdate(pos.x.toFloat(), pos.x.toFloat())
        this.animationY.forceUpdate(pos.y.toFloat(), pos.y.toFloat())
        this.animationZ.forceUpdate(pos.z.toFloat(), pos.z.toFloat())
        if (resetSize) this.animationSize.forceUpdate(0f, 0f )
    }

    fun forceResetSize(size: Float) {
        this.animationSize.forceUpdate(size * 50f, size * 50f)
    }

    fun end() {
        this.isEnded = true
    }

    fun begin() {
        this.isEnded = false
    }
}