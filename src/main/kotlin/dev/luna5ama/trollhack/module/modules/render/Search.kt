package dev.luna5ama.trollhack.module.modules.render

import dev.fastmc.common.*
import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.common.sort.ObjectIntrosort
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.GlStateUtils
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.color.ColorUtils
import dev.luna5ama.trollhack.graphics.esp.StaticBoxRenderer
import dev.luna5ama.trollhack.graphics.esp.StaticTracerRenderer
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.setting.settings.impl.collection.CollectionSetting
import dev.luna5ama.trollhack.util.BOOLEAN_SUPPLIER_FALSE
import dev.luna5ama.trollhack.util.EntityUtils.flooredPosition
import dev.luna5ama.trollhack.util.accessor.palette
import dev.luna5ama.trollhack.util.accessor.storage
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.or
import dev.luna5ama.trollhack.util.threads.BackgroundScope
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSet
import it.unimi.dsi.fastutil.objects.ObjectSets
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.world.chunk.Chunk
import org.lwjgl.opengl.GL11.*

@Suppress("EXPERIMENTAL_API_USAGE")
internal object Search : Module(
    name = "Search",
    description = "Highlights blocks in the world",
    category = Category.RENDER
) {
    private val defaultSearchList = linkedSetOf("minecraft:portal", "minecraft:end_portal_frame", "minecraft:bed")

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
    private val color by setting("Color", ColorRGB(255, 255, 255), false, customColors0.atTrue())
    private val filledAlpha by setting("Filled Alpha", 63, 0..255, 1, filled0.atTrue())
    private val outlineAlpha by setting("Outline Alpha", 200, 0..255, 1, outline0.atTrue())
    private val tracerAlpha by setting("Tracer Alpha", 200, 0..255, 1, tracer0.atTrue())
    private val width by setting("Width", 2.0f, 0.25f..5.0f, 0.25f, outline0.atTrue() or tracer0.atTrue())

    val searchList = setting(CollectionSetting("Search List", defaultSearchList, BOOLEAN_SUPPLIER_FALSE))

    private var blockSet: ObjectSet<Block> = ObjectSets.emptySet()

    private val boxRenderer = StaticBoxRenderer()
    private val tracerRenderer = StaticTracerRenderer()
    private val updateTimer = TickTimer()

    private var dirty = false
    private var lastUpdatePos: BlockPos? = null
    private var lastUpdateJob: Job? = null
    private val gcTimer = TickTimer()
    private val cachedMainList =
        DoubleBuffered<FastObjectArrayList<BlockRenderInfo>>(FastObjectArrayList.Companion::typed)
    private var cachedSublistPool =
        ConcurrentObjectPool<FastObjectArrayList<BlockRenderInfo>>(FastObjectArrayList.Companion::typed)

    override fun getHudInfo(): String {
        return boxRenderer.size.toString()
    }

    init {
        onEnable {
            updateTimer.reset(-114514L)
        }

        onDisable {
            dirty = true
            lastUpdatePos = null

            boxRenderer.clear()
            tracerRenderer.clear()
            cachedMainList.front.clearAndTrim()
            cachedMainList.back.clearAndTrim()
            cachedSublistPool = ConcurrentObjectPool(FastObjectArrayList.Companion::typed)
        }

        safeListener<WorldEvent.ClientBlockUpdate> {
            val eyeX = player.posX.floorToInt()
            val eyeY = (player.posY + player.getEyeHeight()).floorToInt()
            val eyeZ = player.posZ.floorToInt()
            if (it.pos.distanceSqTo(eyeX, eyeY, eyeZ) <= range.sq
                && (blockSet.contains(it.oldState.block) || blockSet.contains(it.newState.block))
            ) {
                dirty = true
            }
        }

        safeListener<Render3DEvent> {
            glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)
            GlStateManager.glLineWidth(width)
            GlStateUtils.depth(false)

            val filledAlpha = if (filled) filledAlpha else 0
            val outlineAlpha = if (outline) outlineAlpha else 0
            val tracerAlpha = if (tracer) tracerAlpha else 0

            boxRenderer.render(filledAlpha, outlineAlpha)
            tracerRenderer.render(tracerAlpha)

            GlStateUtils.depth(true)
            GlStateManager.glLineWidth(1.0f)

            val playerPos = player.flooredPosition

            if (lastUpdateJob.isCompletedOrNull &&
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
    private fun SafeClientEvent.updateRenderer() {
        lastUpdateJob = BackgroundScope.launch {
            val cleanList = gcTimer.tickAndReset(1000L)

            val eyeX = player.posX.floorToInt()
            val eyeY = (player.posY + player.getEyeHeight()).floorToInt()
            val eyeZ = player.posZ.floorToInt()

            val renderDist = mc.gameSettings.renderDistanceChunks
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
                        val mainListArray = mainList.elements()
                        for (index in 0 until mainList.size) {
                            if (index >= maximumBlocks) break
                            val info = mainListArray[index]
                            pos.setPos(info.x, info.y, info.z)
                            val blockState = world.getBlockState(pos)
                            val box = blockState.getSelectedBoundingBox(world, pos)
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
                        if (!chunk.isLoaded) continue

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

        val mainListArray = mainList.elements()
        val sublistArray = sublist.elements()
        val outputListArray = outputList.elements()

        var i = 0
        var j = 0
        var k = 0

        while (i < mainList.size && j < sublist.size) {
            val a = mainListArray[i]
            val b = sublistArray[j]

            if (a < b) {
                outputListArray[k++] = a
                i++
            } else {
                outputListArray[k++] = b
                j++
            }
        }

        while (i < mainList.size) {
            outputListArray[k++] = mainListArray[i++]
        }

        while (j < sublist.size) {
            outputListArray[k++] = sublistArray[j++]
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

    private suspend fun findBlocksInChunk(
        actor: SendChannel<FastObjectArrayList<BlockRenderInfo>>,
        chunk: Chunk,
        eyeX: Int,
        eyeY: Int,
        eyeZ: Int,
        rangeSq: Int
    ) {
        val xStart = chunk.x shl 4
        val zStart = chunk.z shl 4
        val list = cachedSublistPool.get()

        for (yBlock in chunk.blockStorageArray.indices) {
            val yStart = yBlock shl 4

            val blockStorage = chunk.blockStorageArray[yBlock] ?: continue
            if (blockStorage.isEmpty) continue
            val blockStateContainer = blockStorage.data
            val storage = blockStateContainer.storage
            val palette = blockStateContainer.palette

            for (index in 0 until 4096) {
                val blockState = palette.getBlockState(storage.getAt(index)) ?: continue
                if (!blockSet.contains(blockState.block)) continue

                val x = xStart + (index and 0xF)
                val z = zStart + (index shr 4 and 0xF)
                val y = yStart + (index shr 8 and 0xF)

                val dist = distanceSq(eyeX, eyeY, eyeZ, x, y, z)
                if (dist > rangeSq) continue

                list.add(BlockRenderInfo(x, y, z, dist))
            }
        }

        if (list.isEmpty) {
            cachedSublistPool.put(list)
        } else {
            ObjectIntrosort.sort(list.elements(), 0, list.size)
            actor.send(list)
        }
    }

    private fun SafeClientEvent.getBlockColor(pos: BlockPos, blockState: IBlockState): ColorRGB {
        return if (!customColors) {
            if (blockState.block == Blocks.PORTAL) {
                ColorRGB(82, 49, 153)
            } else {
                val colorArgb = blockState.getMapColor(world, pos).colorValue
                ColorRGB(ColorUtils.argbToRgba(colorArgb)).alpha(255)
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

    init {
        searchList.editListeners.add {
            val newSet = ObjectOpenHashSet<Block>().apply {
                it.forEach {
                    val block = Block.getBlockFromName(it)
                    if (block != Blocks.AIR) add(block)
                }
            }

            if (blockSet.size != newSet.size || !blockSet.containsAll(newSet)) {
                dirty = true
            }

            blockSet = newSet
        }
    }
}