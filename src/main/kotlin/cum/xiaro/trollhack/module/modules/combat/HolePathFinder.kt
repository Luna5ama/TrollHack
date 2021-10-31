package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.util.extension.sq
import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.player.InputUpdateEvent
import cum.xiaro.trollhack.event.events.player.PlayerMoveEvent
import cum.xiaro.trollhack.event.events.render.Render3DEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.HoleManager
import cum.xiaro.trollhack.manager.managers.TimerManager.modifyTimer
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.EntityUtils.betterPosition
import cum.xiaro.trollhack.util.MovementUtils.applySpeedPotionEffects
import cum.xiaro.trollhack.util.MovementUtils.resetMove
import cum.xiaro.trollhack.util.combat.HoleInfo
import cum.xiaro.trollhack.util.graphics.RenderUtils3D
import cum.xiaro.trollhack.util.math.VectorUtils.setAndAdd
import cum.xiaro.trollhack.util.math.vector.distanceSqTo
import cum.xiaro.trollhack.util.PathFinder
import cum.xiaro.trollhack.util.text.NoSpamMessage
import cum.xiaro.trollhack.util.threads.TrollHackScope
import cum.xiaro.trollhack.util.threads.isActiveOrFalse
import cum.xiaro.trollhack.util.threads.runSafe
import cum.xiaro.trollhack.util.world.getGroundPos
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import net.minecraft.util.MovementInputFromOptions
import net.minecraft.util.math.BlockPos
import org.lwjgl.opengl.GL11.*
import kotlin.math.hypot

internal object HolePathFinder : Module(
    name = "HolePathFinder",
    category = Category.COMBAT,
    description = "I love hole"
) {
    private val maxTargetHoles by setting("Max target Holes", 5, 1..10, 1)
    private val timeout by setting("Timeout", 5, 1..100, 1)
    private val range by setting("Range", 8, 1..16, 1)
    private val scanVRange by setting("Scan V Range", 16, 4..32, 1)
    private val scanHRange by setting("Scan H Range", 16, 4..30, 1)

    private var job: Job? = null
    private var hole: HoleInfo? = null
    private var path: ArrayDeque<PathFinder.PathNode>? = null

    override fun isActive(): Boolean {
        return isEnabled && hole != null
    }

    init {
        onDisable {
            job?.cancel()
            job = null
            hole = null
            path = null
        }

        onEnable {
            runSafe {
                if (HoleManager.getHoleInfo(player).isHole) {
                    NoSpamMessage.sendMessage(HolePathFinder, "Already in hole")
                    disable()
                    return@onEnable
                }

                if (world.collidesWithAnyBlock(player.entityBoundingBox)) {
                    NoSpamMessage.sendMessage(HolePathFinder, "Player in block")
                    disable()
                    return@onEnable
                }

                calculatePath()
            } ?: disable()
        }

        listener<InputUpdateEvent>(-68) {
            if (it.movementInput is MovementInputFromOptions && isActive()) {
                it.movementInput.resetMove()
            }
        }

        safeListener<PlayerMoveEvent.Pre> {
            if (!job.isActiveOrFalse && hole == null) {
                NoSpamMessage.sendMessage(HolePathFinder, "Calculation timeout")
                disable()
                return@safeListener
            }

            val playerPos = player.betterPosition

            if (HoleManager.getHoleInfo(playerPos).isHole) {
                disable()
                return@safeListener
            }

            val path = path ?: return@safeListener
            val goal = path.lastOrNull()

            if (goal == null || playerPos.x == goal.x && playerPos.y == goal.y && playerPos.z == goal.z) {
                disable()
                return@safeListener
            }

            if (!clearPreviousNode(path)) {
                calculatePath()
                return@safeListener
            }

            val baseSpeed = player.applySpeedPotionEffects(0.2873)
            var countDown = 20
            var motionX = 0.0
            var motionZ = 0.0

            while (countDown-- > 0) {
                val node = path.firstOrNull() ?: break
                motionX = node.x + 0.5 - player.posX
                motionZ = node.z + 0.5 - player.posZ

                val total = hypot(motionX, motionZ)
                if (total > baseSpeed) {
                    val multiplier = baseSpeed / total
                    motionX *= multiplier
                    motionZ *= multiplier
                    break
                } else {
                    path.removeFirstOrNull()
                }
            }

            player.motionX = motionX
            player.motionZ = motionZ

            modifyTimer(45.87156f)
        }

        safeListener<Render3DEvent> {
            path?.let {
                val color = ColorRGB(32, 255, 32, 200)

                for (cell in it) {
                    RenderUtils3D.putVertex(cell.x + 0.5, cell.y + 0.5, cell.z + 0.5, color)
                }

                glLineWidth(4.0f)
                glDisable(GL_DEPTH_TEST)
                RenderUtils3D.draw(GL_LINE_STRIP)
                glLineWidth(1.0f)
                glEnable(GL_DEPTH_TEST)
            }
        }
    }

    private fun SafeClientEvent.clearPreviousNode(path: ArrayDeque<PathFinder.PathNode>): Boolean {
        do {
            val nextNode = path.firstOrNull() ?: break
            val lastNode = nextNode.parent

            if (lastNode != null) {
                var minX = lastNode.x
                var maxX = nextNode.x
                if (maxX < minX) {
                    minX = maxX
                    maxX = lastNode.x
                }

                var minZ = lastNode.z
                var maxZ = nextNode.z
                if (maxZ < minZ) {
                    minZ = maxZ
                    maxZ = lastNode.z
                }

                if (player.posX >= minX - 0.5 && player.posX <= maxX + 1.5 && player.posZ >= minZ - 0.5 && player.posZ <= maxZ + 1.5) {
                    return true
                }
            }

            path.removeFirstOrNull() ?: break
        } while (path.isNotEmpty())

        return false
    }

    private fun SafeClientEvent.calculatePath() {
        if (!job.isActiveOrFalse) {
            job = TrollHackScope.launch {
                val playerPos = player.betterPosition
                val rangeSq = range.sq

                val holes = HoleManager.holeInfos.asSequence()
                    .filterNot { it.isFullyTrapped }
                    .filter { playerPos.distanceSq(it.origin) <= rangeSq }
                    .sortedBy { playerPos.distanceSqTo(it.origin) }
                    .take(maxTargetHoles)
                    .toList()

                if (holes.isEmpty()) {
                    NoSpamMessage.sendWarning(HolePathFinder, "No Holes")
                    disable()
                } else {
                    val actor = parallelPathFindingActor()

                    coroutineScope {
                        val set = dumpWorld()
                        val pathFinder = PathFinder(set)
                        val start = world.getGroundPos(player).up().toNode()

                        holes.forEach { holeInfo ->
                            launch {
                                runCatching {
                                    pathFinder.calculatePath(start, holeInfo.origin.toNode(), timeout * 100)
                                }.getOrNull()?.let {
                                    actor.send(holeInfo to it)
                                }
                            }
                        }
                    }

                    actor.close()
                }
            }
        }
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private fun CoroutineScope.parallelPathFindingActor(): SendChannel<Pair<HoleInfo, ArrayDeque<PathFinder.PathNode>>> {
        return actor {
            var lastCost = Int.MAX_VALUE
            var result: Pair<HoleInfo, ArrayDeque<PathFinder.PathNode>>? = null

            for (pair in channel) {
                val newCost = pair.second.lastOrNull()?.g ?: -1
                if (result == null) {
                    lastCost = newCost
                    result = pair
                } else {
                    if (newCost < lastCost) {
                        lastCost = newCost
                        result = pair
                    }
                }
            }

            if (result == null) {
                hole = null
                path = null
            } else {
                hole = result.first
                path = result.second
            }
        }
    }

    private fun SafeClientEvent.dumpWorld(): LongSet {
        val set = LongOpenHashSet()
        val playerPos = player.betterPosition
        val mutablePos = BlockPos.MutableBlockPos()

        for (x in -scanHRange..scanHRange) {
            for (z in -scanHRange..scanHRange) {
                for (y in -scanVRange..scanVRange) {
                    val pos = mutablePos.setAndAdd(playerPos, x, y, z)
                    val blockState = world.getBlockState(pos)
                    if (blockState.getCollisionBoundingBox(world, pos) != null) {
                        set.add(pos.toLong())
                    }
                }
            }
        }

        return set
    }

    private fun BlockPos.toNode(): PathFinder.Node {
        return PathFinder.Node(x, y, z)
    }
}

