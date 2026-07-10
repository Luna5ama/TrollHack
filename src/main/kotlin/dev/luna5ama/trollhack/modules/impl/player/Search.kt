package dev.luna5ama.trollhack.modules.impl.player

import dev.luna5ama.trollhack.utils.DoubleBuffered
import dev.luna5ama.trollhack.utils.collections.FastObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.Render3DEvent
import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import dev.luna5ama.trollhack.graphics.GLHelper
import dev.luna5ama.trollhack.graphics.StaticBoxRenderer
import dev.luna5ama.trollhack.graphics.StaticTracerRenderer
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.color.ColorUtils
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.extension.flooredPosition
import dev.luna5ama.trollhack.utils.math.floorToInt
import dev.luna5ama.trollhack.utils.math.sq
import dev.luna5ama.trollhack.utils.math.vectors.distanceSq
import dev.luna5ama.trollhack.utils.math.vectors.distanceSqTo
import dev.luna5ama.trollhack.utils.threads.Coroutine
import dev.luna5ama.trollhack.utils.timing.TickTimer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.chunk.ChunkAccess

@Suppress("EXPERIMENTAL_API_USAGE")
object Search : Module(
    "Search",
    description = "Highlights blocks in the world",
    category = Category.RENDER
) {
    private val defaultSearchList = linkedSetOf("minecraft:red_bed")

    private val forceUpdateDelay by setting("Force Update Delay", 250, 50..3000, 10)
    private val updateDelay by setting("Update Delay", 50, 5..500, 5)
    private val range by setting("Range", 128, 0..256, 8)
    private val maximumBlocks by setting("Maximum Blocks", 512, 128..8192, 128)
    private val filled0 = setting("Filled", true)
    private val filled by filled0
    private val outline0 = setting("Outline", true)
    private val outline by outline0
    private val tracer0 = setting("Tracer", true)
    private val tracer by tracer0
    private val customColors0 = setting("Custom Colors", true)
    private val customColors by customColors0
    private val color by setting("Color", ColorRGBA(255, 255, 255),  { customColors })
    private val filledAlpha by setting("Filled Alpha", 63, 0..255, 1, { filled })
    private val outlineAlpha by setting("Outline Alpha", 200, 0..255, 1, { outline })
    private val tracerAlpha by setting("Tracer Alpha", 200, 0..255, 1, { tracer })
    private val width by setting("Width", 2.0f, 0.25f..5.0f, 0.25f, { outline || tracer })

    val searchList = setting("Search List", defaultSearchList)

    private var blockSet: ObjectSet<Block> = ObjectOpenHashSet<Block>().apply {
        defaultSearchList.forEach {
            val block = BuiltInRegistries.BLOCK.get(Identifier.parse(it)).get().value()
            if (block != Blocks.AIR) add(block)
        }
    }

    private val boxRenderer = StaticBoxRenderer()
    private val tracerRenderer = StaticTracerRenderer()
    private val updateTimer = TickTimer()

    private var dirty = false
    private var lastUpdatePos: BlockPos? = null
    private var lastUpdateJob: Job? = null
    private val gcTimer = TickTimer()
    private val cachedMainList =
        DoubleBuffered<FastObjectArrayList<BlockRenderInfo>>(::newRenderInfoList)
    private var cachedSublistPool =
        ConcurrentObjectPool<FastObjectArrayList<BlockRenderInfo>>(::newRenderInfoList)

    override fun getDisplayInfo(): Any? {
        return boxRenderer.size.toString()
    }

    init {
        onEnabled {
            updateTimer.reset(-114514L)
        }

        onDisabled {
            dirty = true
            lastUpdatePos = null

            boxRenderer.clear()
            tracerRenderer.clear()
            cachedMainList.front.clearAndTrim()
            cachedMainList.back.clearAndTrim()
            cachedSublistPool = ConcurrentObjectPool(::newRenderInfoList)
        }

        nonNullHandler<WorldEvent.ClientBlockUpdate> {
            val eyeX = player.x.floorToInt()
            val eyeY = (player.y + player.getEyeHeight(player.pose)).floorToInt()
            val eyeZ = player.z.floorToInt()
            if (it.pos.distanceSqTo(eyeX, eyeY, eyeZ) <= range.sq
                && (blockSet.contains(it.oldState.block) || blockSet.contains(it.newState.block))
            ) {
                dirty = true
            }
        }

        nonNullHandler<Render3DEvent> {
            GLHelper.depth = false

            val filledAlpha = if (filled) filledAlpha else 0
            val outlineAlpha = if (outline) outlineAlpha else 0
            val tracerAlpha = if (tracer) tracerAlpha else 0

            boxRenderer.render(filledAlpha, outlineAlpha)
            tracerRenderer.render(tracerAlpha)

            GLHelper.depth = true

            val playerPos = player.flooredPosition

            if ((lastUpdateJob == null || lastUpdateJob?.isCompleted == true) &&
                (updateTimer.tick(forceUpdateDelay)
                        || updateTimer.tick(updateDelay) && (dirty || playerPos != lastUpdatePos))
            ) {
                updateRenderer()
                dirty = false
                lastUpdatePos = playerPos
                updateTimer.reset()
            }
        }
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private fun NonNullContext.updateRenderer() {
        lastUpdateJob = Coroutine.launch {
            val cleanList = gcTimer.tickAndReset(1000L)

            val eyeX = player.x.floorToInt()
            val eyeY = (player.y + player.getEyeHeight(player.pose)).floorToInt()
            val eyeZ = player.z.floorToInt()

            val renderDist = mc.options.renderDistance().get()
            val playerChunkPosX = eyeX shr 4
            val playerChunkPosZ = eyeZ shr 4

            val rangeSq = range.sq
            val maxChunkRange = rangeSq + 256

            @Suppress("RemoveExplicitTypeArguments")
            val actor = actor<FastObjectArrayList<BlockRenderInfo>> {
                loop@ for (sublist in channel) {
                    merge(cachedMainList.front, sublist, cachedMainList.back)
                    cachedMainList.swap()
                    clearList(cleanList, sublist)
                    cachedSublistPool.put(sublist)
                }

                val pos = BlockPos.MutableBlockPos()

                tracerRenderer.update {
                    boxRenderer.update {
                        val mainList = cachedMainList.front
                        for (index in 0 until mainList.size) {
                            if (index >= maximumBlocks) break
                            val info = mainList[index]
                            pos.set(info.x, info.y, info.z)
                            val blockState = world.getBlockState(pos)
                            val box = (blockState.getShape(world, pos).takeUnless { it.isEmpty }?.bounds() ?: continue).move(pos)
                            val color = getBlockColor(pos, blockState)
                            putBox(box, color)
                            putTracer(box, color)
                        }
                    }
                }

                clearList(cleanList, cachedMainList.front)
                clearList(cleanList, cachedMainList.back)
            }

            coroutineScope {
                for (x in playerChunkPosX - renderDist..playerChunkPosX + renderDist) {
                    for (z in playerChunkPosZ - renderDist..playerChunkPosZ + renderDist) {
                        val chunk = world.getChunk(x, z)

                        val chunkX = (x shl 4) + 8
                        val chunkZ = (z shl 4) + 8

                        if (distanceSq(eyeX, eyeZ, chunkX, chunkZ) > maxChunkRange) continue

                        launch {
                            findBlocksInChunk(actor, chunk, eyeX, eyeY, eyeZ, rangeSq)
                        }
                    }
                }
            }

            actor.close()
        }
    }

    private fun merge(
        mainList: FastObjectArrayList<BlockRenderInfo>,
        sublist: FastObjectArrayList<BlockRenderInfo>,
        outputList: FastObjectArrayList<BlockRenderInfo>
    ) {
        outputList.clearFast()
        outputList.ensureCapacity(mainList.size + sublist.size)

        val mainListArray = mainList.toTypedArray()
        val sublistArray = sublist.toTypedArray()

        var i = 0
        var j = 0
        var k = 0

        while (i < mainList.size && j < sublist.size) {
            val a = mainListArray[i]
            val b = sublistArray[j]

            if (a < b) {
                outputList.add(a)
                i++
            } else {
                outputList.add(b)
                j++
            }
            k++
        }

        while (i < mainList.size) {
            outputList.add(mainListArray[i++])
            k++
        }

        while (j < sublist.size) {
            outputList.add(sublistArray[j++])
            k++
        }

        outputList.setSize(k)
    }

    private fun clearList(
        cleanList: Boolean,
        list: FastObjectArrayList<BlockRenderInfo>
    ) {
        if (cleanList) {
            val prevSize = list.size
            list.clear()
            list.trim(prevSize)
        } else {
            list.clearFast()
        }
    }

    private fun getHighestNonEmptySectionYOffset(chunk: ChunkAccess): Int {
        val i = chunk.getHighestFilledSectionIndex()
        if (i == -1) return chunk.minY

        return SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i))
    }

    private suspend fun findBlocksInChunk(
        actor: SendChannel<FastObjectArrayList<BlockRenderInfo>>,
        chunk: ChunkAccess,
        eyeX: Int,
        eyeY: Int,
        eyeZ: Int,
        rangeSq: Int
    ) {
        val xStart = chunk.pos.x shl 4
        val zStart = chunk.pos.z shl 4
        val list = cachedSublistPool.get()
        try {
            for (yBlock in chunk.sections.indices) {
                val yStart = (yBlock shl 4) + chunk.minY

                val blockStorage = chunk.sections[yBlock] ?: continue
                if (blockStorage.hasOnlyAir()) continue

                val blockStateContainer = blockStorage.states.data
                val storage = blockStateContainer.storage
                val palette = blockStateContainer.palette

                for (index in 0 until 4096) {
                    val blockState = palette.valueFor(storage.get(index)) ?: continue
                    if (!blockSet.contains(blockState.block)) continue

                    val x = xStart + (index and 0xF)
                    val z = zStart + (index shr 4 and 0xF)
                    val y = yStart + (index shr 8 and 0xF)

                    val dist = distanceSq(eyeX, eyeY, eyeZ, x, y, z)
                    if (dist > rangeSq) continue

                    list.add(BlockRenderInfo(x, y, z, dist))
                }
            }

        } catch (e: Throwable) {
            e.printStackTrace()
        }

        if (list.isEmpty) {
            cachedSublistPool.put(list)
        } else {
            list.elements().sort(0, list.size)
            actor.send(list)
        }
    }

    private fun NonNullContext.getBlockColor(pos: BlockPos, blockState: BlockState): ColorRGBA {
        return if (!customColors) {
            if (blockState.block == Blocks.NETHER_PORTAL) {
                ColorRGBA(82, 49, 153)
            } else {
                val colorArgb = blockState.getMapColor(world, pos).col
                ColorRGBA(ColorUtils.argbToRgba(colorArgb)).alpha(255)
            }
        } else {
            color
        }
    }

    private class BlockRenderInfo(val x: Int, val y: Int, val z: Int, val dist: Int) : Comparable<BlockRenderInfo> {
        override fun compareTo(other: BlockRenderInfo): Int {
            return this.dist.compareTo(other.dist)
        }
    }

    private fun newRenderInfoList(): FastObjectArrayList<BlockRenderInfo> {
        return FastObjectArrayList.typed()
    }

    private class ConcurrentObjectPool<T>(private val supplier: () -> T) {
        private val values = ConcurrentLinkedQueue<T>()

        fun get(): T = values.poll() ?: supplier()

        fun put(value: T) {
            values.offer(value)
        }
    }

    init {
        searchList.register { prev, new ->
            val newSet = ObjectOpenHashSet<Block>().apply {
                new.forEach {
                    val block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(it))
                    if (block != Blocks.AIR) add(block)
                }
            }

            if (blockSet.size != newSet.size || !blockSet.containsAll(newSet)) {
                dirty = true
            }

            blockSet = newSet

            true
        }
    }
}
