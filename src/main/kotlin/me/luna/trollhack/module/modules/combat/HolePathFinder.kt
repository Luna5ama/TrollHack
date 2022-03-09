package me.luna.trollhack.module.modules.combat

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.toList
import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.TickEvent
import me.luna.trollhack.event.events.player.InputUpdateEvent
import me.luna.trollhack.event.events.player.PlayerMoveEvent
import me.luna.trollhack.event.events.render.Render3DEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.gui.hudgui.elements.client.Notification
import me.luna.trollhack.manager.managers.CombatManager
import me.luna.trollhack.manager.managers.HoleManager
import me.luna.trollhack.manager.managers.TimerManager.modifyTimer
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.Bind
import me.luna.trollhack.util.EntityUtils.betterPosition
import me.luna.trollhack.util.MovementUtils.applySpeedPotionEffects
import me.luna.trollhack.util.MovementUtils.resetMove
import me.luna.trollhack.util.PathFinder
import me.luna.trollhack.util.combat.HoleInfo
import me.luna.trollhack.util.extension.sq
import me.luna.trollhack.util.graphics.RenderUtils3D
import me.luna.trollhack.util.graphics.color.ColorRGB
import me.luna.trollhack.util.math.VectorUtils.setAndAdd
import me.luna.trollhack.util.math.vector.distanceSqTo
import me.luna.trollhack.util.threads.TrollHackScope
import me.luna.trollhack.util.threads.isActiveOrFalse
import me.luna.trollhack.util.threads.runSafe
import me.luna.trollhack.util.world.getGroundPos
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.MoverType
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.MovementInputFromOptions
import net.minecraft.util.math.BlockPos
import org.lwjgl.opengl.GL11.*
import kotlin.math.hypot

internal object HolePathFinder : Module(
    name = "HolePathFinder",
    category = Category.COMBAT,
    description = "I love hole"
) {
    private val moveMode by setting("Move Mode", MoveMode.NORMAL)
    private val maxMoveTicks by setting("Max Move Ticks", 20, 1..50, 1, { moveMode == MoveMode.TELEPORT })
    private val enableHoleSnap by setting("Enable Hole Snap", true, { moveMode == MoveMode.NORMAL })
    private val bindtargetHole by setting("Bind Target Hole", Bind(), {
        if (it) {
            if (isDisabled) {
                type = TargetHoleType.TARGET
                enable()
            } else {
                disable()
            }
        }
    })
    private val bindNearTarget by setting("Bind Near Target", Bind(), {
        if (it) {
            if (isDisabled) {
                type = TargetHoleType.NEAR_TARGET
                enable()
            } else {
                disable()
            }
        }
    })
    private val maxTargetHoles by setting("Max target Holes", 5, 1..10, 1)
    private val timeout by setting("Timeout", 5, 1..100, 1)
    private val range by setting("Range", 8, 1..16, 1)
    private val scanVRange by setting("Scan V Range", 16, 4..32, 1)
    private val scanHRange by setting("Scan H Range", 16, 4..30, 1)

    private enum class MoveMode {
        NORMAL, TELEPORT
    }

    private var type = TargetHoleType.NORMAL
    var hole: HoleInfo? = null; private set
    private var job: Job? = null
    private var path: ArrayDeque<PathFinder.PathNode>? = null
    private var targetPos: BlockPos? = null

    enum class TargetHoleType {
        NORMAL, TARGET, NEAR_TARGET
    }

    override fun isActive(): Boolean {
        return isEnabled && hole != null
    }

    init {
        onDisable {
            type = TargetHoleType.NORMAL
            job?.cancel()
            job = null
            hole = null
            path = null
            targetPos = null
        }

        onEnable {
            runSafe {
                if (type == TargetHoleType.NORMAL && HoleManager.getHoleInfo(player).isHole) {
                    Notification.send(HolePathFinder, "Already in hole")
                    disable()
                    return@onEnable
                }

                if (world.collidesWithAnyBlock(player.entityBoundingBox)) {
                    Notification.send(HolePathFinder, "Player in block")
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

        safeListener<PlayerMoveEvent.Pre> { event ->
            if (moveMode == MoveMode.NORMAL) {
                check()?.let {
                    moveLegit(event, it)
                }
            }
        }

        safeListener<TickEvent.Post> {
            if (moveMode == MoveMode.TELEPORT) {
                check()?.let {
                    moveTeleport(it)
                }
            }
        }

        safeListener<Render3DEvent> {
            path?.let {
                val color = ColorRGB(32, 255, 32, 200)

                for (cell in it) {
                    RenderUtils3D.putVertex(cell.x + 0.5, cell.y + 0.5, cell.z + 0.5, color)
                }

                GlStateManager.glLineWidth(2.0f)
                glDisable(GL_DEPTH_TEST)
                RenderUtils3D.draw(GL_LINE_STRIP)
                GlStateManager.glLineWidth(1.0f)
                glEnable(GL_DEPTH_TEST)
            }
        }
    }

    private fun SafeClientEvent.check(): ArrayDeque<PathFinder.PathNode>? {
        if (!job.isActiveOrFalse && hole == null) {
            Notification.send(HolePathFinder, "Calculation timeout")
            disable()
            return null
        }

        val playerPos = player.betterPosition
        val playerHole = HoleManager.getHoleInfo(playerPos)

        if (type != TargetHoleType.NORMAL) {
            getTargetPos()?.let { newPos ->
                targetPos?.let {
                    if (newPos != it) {
                        calculatePath()
                        return null
                    }
                }
            }
        }

        if (playerHole.origin == hole?.origin) {
            disable()
            return null
        }

        val newHole = hole?.let { oldHole ->
            HoleManager.getHoleInfo(oldHole.origin).takeIf { it.isHole }
        }

        if (newHole == null) {
            calculatePath()
            return null
        }

        val path = path ?: return null
        val goal = path.lastOrNull()

        if (goal == null && !playerHole.isHole) {
            calculatePath()
            return null
        }

        if (goal == null || playerPos.x == goal.x && playerPos.y == goal.y && playerPos.z == goal.z) {
            disable()
            return null
        }

        if (!clearPreviousNode(path)) {
            calculatePath()
            return null
        }

        if (moveMode == MoveMode.NORMAL && enableHoleSnap) HoleSnap.enable()
        if (HoleSnap.isActive()) return null

        return path
    }

    private fun SafeClientEvent.moveTeleport(path: ArrayDeque<PathFinder.PathNode>) {
        val baseSpeed = player.applySpeedPotionEffects(0.2873)
        var countDown = maxMoveTicks

        while (countDown-- > 0) {
            val node = path.firstOrNull() ?: break
            var motionX = node.x + 0.5 - player.posX
            var motionZ = node.z + 0.5 - player.posZ

            val total = hypot(motionX, motionZ)

            if (total > baseSpeed) {
                val multiplier = baseSpeed / total
                motionX *= multiplier
                motionZ *= multiplier
            } else if (player.posY.toInt() <= node.y) {
                path.removeFirstOrNull()
            }

            player.motionX = motionX
            player.motionZ = motionZ
            player.move(MoverType.SELF, motionX, player.motionY, motionZ)
            connection.sendPacket(CPacketPlayer.Position(player.posX, player.posY, player.posZ, player.onGround))
        }

        player.motionX = 0.0
        player.motionZ = 0.0
    }

    private fun SafeClientEvent.moveLegit(event: PlayerMoveEvent.Pre, path: ArrayDeque<PathFinder.PathNode>) {
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
            } else if (player.posY.toInt() <= node.y) {
                path.removeFirstOrNull()
            } else {
                break
            }
        }

        player.motionX = 0.0
        player.motionZ = 0.0

        event.x = motionX
        event.z = motionZ

        modifyTimer(45.87156f)
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

                var minY = lastNode.y
                var maxY = nextNode.y
                if (maxY < minY) {
                    minY = maxY
                    maxY = lastNode.y
                }

                var minZ = lastNode.z
                var maxZ = nextNode.z
                if (maxZ < minZ) {
                    minZ = maxZ
                    maxZ = lastNode.z
                }

                if (player.posX >= minX - 0.5 && player.posX <= maxX + 1.5
                    && player.posY >= minY - 0.5 && player.posY <= maxY + 1.5
                    && player.posZ >= minZ - 0.5 && player.posZ <= maxZ + 1.5) {
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
                val targetPos = getTargetPos()

                val holes = HoleManager.holeInfos.asSequence()
                    .filterNot { it.isFullyTrapped }
                    .run {
                        when (type) {
                            TargetHoleType.NORMAL -> {
                                filter { playerPos.distanceSq(it.origin) <= rangeSq }
                                    .sortedBy { playerPos.distanceSqTo(it.origin) }
                            }
                            TargetHoleType.TARGET -> {
                                filter { (targetPos ?: playerPos).distanceSq(it.origin) <= rangeSq }
                                    .sortedBy { (targetPos ?: playerPos).distanceSqTo(it.origin) }
                            }
                            TargetHoleType.NEAR_TARGET -> {
                                val targetHole = targetPos?.let { HoleManager.getHoleInfo(it).origin }
                                filter { (targetPos ?: playerPos).distanceSq(it.origin) <= rangeSq }
                                    .filter { it.origin != targetHole }
                                    .sortedBy { (targetPos ?: playerPos).distanceSqTo(it.origin) }
                            }
                        }
                    }
                    .take(maxTargetHoles)
                    .toList()

                if (holes.isEmpty()) {
                    Notification.send(HolePathFinder, "No Holes")
                    disable()
                } else {
                    val actor = parallelPathFindingActor(targetPos)

                    coroutineScope {
                        val set = dumpWorld()
                        val pathFinder = PathFinder(set)
                        val start = world.getGroundPos(player).up().toNode()

                        holes.forEachIndexed { i, holeInfo ->
                            launch {
                                runCatching {
                                    val lol = pathFinder.calculatePath(start, holeInfo.origin.toNode(), timeout * 100)
                                    lol
                                }.getOrNull()?.let {
                                    actor.send(IndexedValue(i, holeInfo to it))
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
    private fun CoroutineScope.parallelPathFindingActor(targetPos: BlockPos?): SendChannel<IndexedValue<Pair<HoleInfo, ArrayDeque<PathFinder.PathNode>>>> {
        return actor {
            val results = channel.toList()

            val result = (if (type == TargetHoleType.NORMAL) {
                results.minByOrNull { (_, it) ->
                    it.second.lastOrNull()?.g ?: -1
                }
            } else {
                results.minByOrNull { (i, _) ->
                    i
                }
            })?.value

            if (result == null) {
                hole = null
                path = null
                if (type != TargetHoleType.NORMAL) this@HolePathFinder.targetPos = targetPos
            } else {
                hole = result.first
                path = result.second
            }
        }
    }

    private fun getTargetPos(): BlockPos? {
        return (ZealotCrystalPlus.target.takeIf { ZealotCrystalPlus.isEnabled } ?: CombatManager.target)?.betterPosition
    }

    private fun SafeClientEvent.dumpWorld(): LongSet {
        val set = LongOpenHashSet()
        val playerPos = player.betterPosition
        val mutablePos = BlockPos.MutableBlockPos()

        for (x in -scanHRange..scanHRange) {
            for (z in -scanHRange..scanHRange) {
                for (y in -scanVRange..scanVRange) {
                    val pos = mutablePos.setAndAdd(playerPos, x, y, z)
                    if (!world.isOutsideBuildHeight(pos) && world.worldBorder.contains(pos)) {
                        val blockState = world.getBlockState(pos)
                        if (blockState.getCollisionBoundingBox(world, pos) != null) {
                            set.add(pos.toLong())
                        }
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

