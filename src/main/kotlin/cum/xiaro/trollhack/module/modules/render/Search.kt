package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.util.extension.fastFloor
import cum.xiaro.trollhack.util.extension.sq
import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.WorldEvent
import cum.xiaro.trollhack.event.events.render.Render3DEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.setting.settings.impl.collection.CollectionSetting
import cum.xiaro.trollhack.util.BOOLEAN_SUPPLIER_FALSE
import cum.xiaro.trollhack.util.EntityUtils.flooredPosition
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.accessor.palette
import cum.xiaro.trollhack.util.accessor.storage
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.graphics.color.ColorUtils
import cum.xiaro.trollhack.util.graphics.esp.StaticBoxRenderer
import cum.xiaro.trollhack.util.graphics.esp.StaticTracerRenderer
import cum.xiaro.trollhack.util.math.vector.distanceSq
import cum.xiaro.trollhack.util.math.vector.distanceSqTo
import cum.xiaro.trollhack.util.or
import cum.xiaro.trollhack.util.threads.TrollHackScope
import cum.xiaro.trollhack.util.threads.defaultScope
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSet
import it.unimi.dsi.fastutil.objects.ObjectSets
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.chunk.Chunk
import org.lwjgl.opengl.GL11.*
import java.util.*

@Suppress("EXPERIMENTAL_API_USAGE")
internal object Search : Module(
    name = "Search",
    description = "Highlights blocks in the world",
    category = Category.RENDER
) {
    private val defaultSearchList = linkedSetOf("minecraft:portal", "minecraft:end_portal_frame", "minecraft:bed")

    private val forceUpdateDelay by setting("Force Update Delay", 250, 50..3000, 10)
    private val updateDelay by setting("Update Delay", 50, 5..500, 5)
    private val updateDistance by setting("Update Distance", 4, 1..32, 1)
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
    private var lastUpdatePos = BlockPos.ORIGIN

    override fun getHudInfo(): String {
        return boxRenderer.size.toString()
    }

    init {
        onEnable {
            updateTimer.reset(-114514L)
        }

        onDisable {
            dirty = true
            lastUpdatePos = BlockPos.ORIGIN
        }

        safeListener<WorldEvent.BlockUpdate> {
            val eyeX = player.posX.fastFloor()
            val eyeY = (player.posY + player.getEyeHeight()).fastFloor()
            val eyeZ = player.posZ.fastFloor()
            if (it.pos.distanceSqTo(eyeX, eyeY, eyeZ) <= range.sq
                && (blockSet.contains(it.oldState.block) || blockSet.contains(it.newState.block))) {
                dirty = true
            }
        }

        safeListener<Render3DEvent> {
            glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)
            glLineWidth(width)
            GlStateUtils.depth(false)

            val filledAlpha = if (filled) filledAlpha else 0
            val outlineAlpha = if (outline) outlineAlpha else 0
            val tracerAlpha = if (tracer) tracerAlpha else 0

            boxRenderer.render(filledAlpha, outlineAlpha)
            tracerRenderer.render(tracerAlpha)

            GlStateUtils.depth(true)
            glLineWidth(1.0f)

            val playerPos = player.flooredPosition

            if (updateTimer.tick(forceUpdateDelay) || updateTimer.tick(updateDelay)
                && (dirty || playerPos.distanceSqTo(lastUpdatePos) > updateDistance.sq)) {
                updateRenderer()
                dirty = false
                lastUpdatePos = playerPos
                updateTimer.reset()
            }
        }
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private fun SafeClientEvent.updateRenderer() {
        defaultScope.launch {
            val eyeX = player.posX.fastFloor()
            val eyeY = (player.posY + player.getEyeHeight()).fastFloor()
            val eyeZ = player.posZ.fastFloor()

            val renderDist = mc.gameSettings.renderDistanceChunks
            val playerChunkPosX = eyeX shr 4
            val playerChunkPosZ = eyeZ shr 4

            val rangeSq = range.sq
            val maxChunkRange = rangeSq + 256
            var mainList = emptyArray<BlockRenderInfo>()

            val actor = actor<ObjectArrayList<BlockRenderInfo>>(TrollHackScope.context) {
                loop@ for (list in channel) {
                    @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
                    val newArray = Arrays.copyOf(mainList, mainList.size + list.size)
                    System.arraycopy(list.elements(), 0, newArray, mainList.size, list.size)
                    mainList = newArray
                }

                Arrays.parallelSort(mainList)

                launch {
                    boxRenderer.update {
                        for ((index, info) in mainList.withIndex()) {
                            if (index >= maximumBlocks) break
                            putBox(info.box, info.color)
                        }
                    }
                }

                launch {
                    tracerRenderer.update {
                        for ((index, info) in mainList.withIndex()) {
                            if (index >= maximumBlocks) break
                            putTracer(info.box, info.color)
                        }
                    }
                }
            }

            coroutineScope {
                for (x in playerChunkPosX - renderDist..playerChunkPosX + renderDist) {
                    for (z in playerChunkPosZ - renderDist..playerChunkPosZ + renderDist) {
                        val chunk = world.getChunk(x, z)
                        if (!chunk.isLoaded) continue

                        val chunkX = (x shl 4) + 8
                        val chunkZ = (z shl 4) + 8

                        if (distanceSq(eyeX, eyeZ, chunkX, chunkZ) > maxChunkRange) continue

                        launch(TrollHackScope.context) {
                            findBlocksInChunk(actor, chunk, eyeX, eyeY, eyeZ, rangeSq)
                        }
                    }
                }
            }

            actor.close()
        }
    }

    private suspend fun SafeClientEvent.findBlocksInChunk(actor: SendChannel<ObjectArrayList<BlockRenderInfo>>, chunk: Chunk, eyeX: Int, eyeY: Int, eyeZ: Int, rangeSq: Int) {
        val xStart = chunk.x shl 4
        val zStart = chunk.z shl 4
        val pos = BlockPos.MutableBlockPos()
        val list = ObjectArrayList<BlockRenderInfo>()

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

                pos.setPos(x, y, z)
                val box = blockState.getSelectedBoundingBox(world, pos)
                val color = getBlockColor(pos, blockState)

                list.add(BlockRenderInfo(box, color, dist))
            }
        }

        if (list.isNotEmpty()) {
            actor.send(list)
        }
    }

    private fun SafeClientEvent.getBlockColor(pos: BlockPos, blockState: IBlockState): ColorRGB {
        return if (!customColors) {
            if (blockState.block == Blocks.PORTAL) {
                ColorRGB(82, 49, 153)
            } else {
                val colorArgb = blockState.getMapColor(world, pos).colorValue
                ColorRGB(ColorUtils.argbToRgba(colorArgb))
            }
        } else {
            color
        }
    }

    private class BlockRenderInfo(val box: AxisAlignedBB, val color: ColorRGB, val dist: Int) : Comparable<BlockRenderInfo> {
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
                updateTimer.reset(-114514L)
            }

            blockSet = newSet
        }
    }
}