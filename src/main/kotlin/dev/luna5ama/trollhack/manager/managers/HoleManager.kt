package dev.luna5ama.trollhack.manager.managers

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeConcurrentListener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.EntityUtils.flooredPosition
import dev.luna5ama.trollhack.util.combat.HoleInfo
import dev.luna5ama.trollhack.util.combat.HoleUtils.checkHoleM
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.threads.BackgroundScope
import dev.luna5ama.trollhack.util.threads.runSafe
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.longs.LongSets
import kotlinx.coroutines.launch
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate

object HoleManager : Manager() {
    private val holeMap0 = ConcurrentHashMap<BlockPos, HoleInfo>()
    private var holeSet: LongSet = LongSets.EMPTY_SET
    val holeMap: Map<BlockPos, HoleInfo> get() = holeMap0
    var holeInfos = emptyList<HoleInfo>(); private set

    private val mainTimer = TickTimer()
    private val updateTimer = TickTimer()
    private val removeTimer = TickTimer()

    private val dirty = AtomicBoolean(false)

    private const val RANGE = 16
    private const val RANGE_SQ = 256
    private const val MAX_RANGE_SQ = 1024

    init {
        listener<WorldEvent.Unload> {
            holeMap0.clear()
            holeInfos = emptyList()
            dirty.set(false)
        }

        safeListener<WorldEvent.ClientBlockUpdate> {
            BackgroundScope.launch {
                val playerPos = player.flooredPosition
                val mutablePos = BlockPos.MutableBlockPos()

                val sequence = sequence {
                    for (x in it.pos.x + 2 downTo it.pos.x - 2) {
                        for (y in it.pos.y + 1 downTo it.pos.y - 2) {
                            for (z in it.pos.z + 2 downTo it.pos.z - 2) {
                                if (playerPos.distanceSqTo(x, y, z) > RANGE_SQ) continue
                                yield(mutablePos.setPos(x, y, z))
                            }
                        }
                    }
                }

                updatePosSequence(sequence)
            }
        }

        safeConcurrentListener<RunGameLoopEvent.Render> {
            if (mainTimer.tickAndReset(100L)) {
                BackgroundScope.launch {
                    if (removeTimer.tickAndReset(500L)) {
                        removeInvalidPos()
                    }

                    updatePos(updateTimer.tickAndReset(1000L))
                }
            }

            if (dirty.getAndSet(false)) {
                updateHoleInfoList()
            }
        }
    }

    fun getHoleBelow(
        pos: BlockPos,
        yRange: Int
    ) = getHoleBelow(pos, yRange) { true }

    fun getHoleBelow(pos: BlockPos, yRange: Int, predicate: Predicate<HoleInfo>): HoleInfo? {
        for (yOffset in 0..yRange) {
            val offsetPos = pos.down(yOffset)
            val info = getHoleInfo(offsetPos)
            if (info.isHole && predicate.test(info)) return info
        }

        return null
    }

    fun getHoleInfo(entity: Entity) =
        getHoleInfo(entity.betterPosition)

    fun getHoleInfo(pos: BlockPos) =
        holeMap0.computeIfAbsent(pos) {
            runSafe { checkHoleM(it) } ?: HoleInfo.empty(it)
        }

    private fun SafeClientEvent.removeInvalidPos() {
        val playerPos = player.flooredPosition
        var modified = false

        val iterator = holeMap0.keys.iterator()
        while (iterator.hasNext()) {
            val pos = iterator.next()
            if (playerPos.distanceSqTo(pos) > MAX_RANGE_SQ) {
                iterator.remove()
                modified = true
            }
        }

        if (modified) {
            dirty.set(true)
        }
    }

    private fun SafeClientEvent.updatePos(force: Boolean) {
        val playerPos = player.flooredPosition
        val checked = LongOpenHashSet()
        val mutablePos = BlockPos.MutableBlockPos()

        var modified = false

        for (x in RANGE downTo -RANGE) {
            for (y in RANGE downTo -RANGE) {
                for (z in RANGE downTo -RANGE) {
                    mutablePos.setPos(playerPos.x + x, playerPos.y + y, playerPos.z + z)
                    if (mutablePos.y !in 0..255) continue
                    if (!force && holeSet.contains(mutablePos.toLong())) continue
                    modified = updatePos(playerPos, checked, mutablePos) || modified
                }
            }
        }

        if (modified) {
            dirty.set(true)
        }
    }

    private fun SafeClientEvent.updatePosSequence(sequence: Sequence<BlockPos.MutableBlockPos>) {
        val playerPos = player.flooredPosition
        val checked = LongOpenHashSet()

        var modified = false

        sequence.forEach {
            modified = updatePos(playerPos, checked, it) || modified
        }

        if (modified) {
            dirty.set(true)
        }
    }

    private fun SafeClientEvent.updatePos(
        playerPos: BlockPos,
        checked: LongSet,
        pos: BlockPos.MutableBlockPos
    ): Boolean {
        val long = pos.toLong()
        if (checked.contains(long)) return false
        if (pos.distanceSq(playerPos) > RANGE_SQ) return false

        val holeInfo = checkHoleM(pos)
        return if (!holeInfo.isHole) {
            val prev = holeMap0.put(holeInfo.origin, holeInfo)
            checked.add(long)
            prev == null || prev.isHole
        } else {
            var modified = false

            for (holePos in holeInfo.holePos) {
                val prev = holeMap0.put(holePos, holeInfo)
                checked.add(holePos.toLong())

                modified = modified
                    || prev == null || prev.type != holeInfo.type
                    || prev.isTrapped != holeInfo.isTrapped
                    || prev.isFullyTrapped != holeInfo.isFullyTrapped
            }

            modified
        }
    }

    private fun updateHoleInfoList() {
        holeSet = LongOpenHashSet(holeMap0.size).apply {
            holeMap0.keys.forEach {
                add(it.toLong())
            }
        }

        holeInfos = holeMap0.values.asSequence()
            .filter { it.isHole }
            .distinct()
            .toList()
    }
}