package dev.luna5ama.trollhack.util

import dev.fastmc.common.distanceSq
import dev.fastmc.common.sq
import dev.luna5ama.trollhack.util.math.vector.toLong
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.objects.ObjectArrayPriorityQueue
import java.util.*

class PathFinder(
    private val blocks: LongSet,
    private var stepHeight: Int = 2,
    private var fallHeight: Int = 5
) {
    fun calculatePath(start: Node, end: Node, timeout: Int): Deque<PathNode> {
        val openNodes = ObjectArrayPriorityQueue<PathNode>()

        val pathNodeStart = PathNode(start.x, start.y, start.z)
        pathNodeStart.initCalc(start, end)

        openNodes.enqueue(pathNodeStart)

        var countDown = timeout

        while (countDown-- > 0) {
            val current = openNodes.dequeue() ?: error("Unreachable")

            if (current == end) {
                val path = ArrayDeque<PathNode>()
                var node: PathNode? = current

                while (node != null) {
                    path.addFirst(node)
                    node = node.parent
                }

                return path
            }

            for (neighbour in availableNeighbours) {
                if (!neighbour.isPassable(this, blocks, current)) continue

                val nextNode = current.nextNode(neighbour)
                if (nextNode == current.parent) continue

                nextNode.calculate(end)
                openNodes.enqueue(nextNode)
            }
        }

        error("Timeout")
    }

    companion object {
        private val availableNeighbours = arrayOf(
            NeighbourNode(
                1, 0, 0,
                NeighbourNode.Type.HORIZONTAL,
                arrayOf(Node(1, 0, 0), Node(1, 1, 0))
            ),
            NeighbourNode(
                -1, 0, 0,
                NeighbourNode.Type.HORIZONTAL,
                arrayOf(Node(-1, 0, 0), Node(-1, 1, 0))
            ),
            NeighbourNode(
                0, 0, 1,
                NeighbourNode.Type.HORIZONTAL,
                arrayOf(Node(0, 0, 1), Node(0, 1, 1))
            ),
            NeighbourNode(
                0, 0, -1,
                NeighbourNode.Type.HORIZONTAL,
                arrayOf(Node(0, 0, -1), Node(0, 1, -1))
            ),

            NeighbourNode(
                0, 1, 0,
                NeighbourNode.Type.VERTICAL,
                arrayOf(Node(0, 0, 0), Node(0, 1, 0), Node(0, 2, 0))
            ).apply {
                cost = 8
            },
            NeighbourNode(
                0, -1, 0,
                NeighbourNode.Type.VERTICAL,
                arrayOf(Node(0, -1, 0), Node(0, 0, 0), Node(0, 1, 0))
            ).apply {
                cost = 1
            },

            // Diagonal
            NeighbourNode(
                1, 0, 1,
                NeighbourNode.Type.HORIZONTAL,
                arrayOf(Node(1, 0, 1), Node(1, 1, 1), Node(1, 0, 0), Node(1, 1, 0), Node(0, 0, 1), Node(0, 1, 1))
            ),
            NeighbourNode(
                1, 0, -1,
                NeighbourNode.Type.HORIZONTAL,
                arrayOf(Node(1, 0, -1), Node(1, 1, -1), Node(1, 0, 0), Node(1, 1, 0), Node(0, 0, -1), Node(0, 1, -1))
            ),
            NeighbourNode(
                -1, 0, 1,
                NeighbourNode.Type.HORIZONTAL,
                arrayOf(Node(-1, 0, 1), Node(-1, 1, 1), Node(-1, 0, 0), Node(-1, 1, 0), Node(0, 0, 1), Node(0, 1, 1))
            ),
            NeighbourNode(
                -1, 0, -1,
                NeighbourNode.Type.HORIZONTAL,
                arrayOf(
                    Node(-1, 0, -1),
                    Node(-1, 1, -1),
                    Node(-1, 0, 0),
                    Node(-1, 1, 0),
                    Node(0, 0, -1),
                    Node(0, 1, -1)
                )
            )
        )
    }

    class NeighbourNode(
        x: Int,
        y: Int,
        z: Int,
        private val type: Type,
        private val collisions: Array<Node>
    ) : Node(x, y, z) {
        var cost = distanceSq(0, 0, 0, x, y, z) * 2

        fun isPassable(pathFinder: PathFinder, blockSet: LongSet, current: PathNode): Boolean {
            return checkStepHeight(pathFinder, current)
                && type.checkGround(pathFinder, blockSet, current, this)
                && collisions.none { blockSet.contains(current.toLong(it)) }
        }

        private fun checkStepHeight(pathFinder: PathFinder, node: PathNode): Boolean {
            var current = node
            var totalY = y
            var lastY = y

            while (true) {
                if (totalY !in -pathFinder.fallHeight..pathFinder.stepHeight) return false

                val neighbourNode = current.last

                if (neighbourNode.type != Type.VERTICAL) break
                if (neighbourNode.y != lastY) break

                totalY += neighbourNode.y
                current = current.parent ?: break
                lastY = neighbourNode.y
            }

            return totalY in -pathFinder.fallHeight..pathFinder.stepHeight
        }

        enum class Type {
            HORIZONTAL {
                override fun checkGround(
                    pathFinder: PathFinder,
                    blockSet: LongSet,
                    current: PathNode,
                    next: NeighbourNode
                ): Boolean {
                    return current.last.type == VERTICAL && current.last.y == 1 || blockSet.contains(
                        current.x,
                        current.y - 1,
                        current.z
                    )
                }
            },
            VERTICAL {
                override fun checkGround(
                    pathFinder: PathFinder,
                    blockSet: LongSet,
                    current: PathNode,
                    next: NeighbourNode
                ): Boolean {
                    val down = next.y == -1
                        && (current.parent == null || checkWall(pathFinder, blockSet, current, next))

                    val up = next.y == 1
                        && (current.last.y == 1
                        || current.last.y == 0
                        && (current.parent == null || blockSet.contains(current.x, current.y - 1, current.z))
                        && checkWall(pathFinder, blockSet, current, next))

                    return down || up
                }

                private fun checkWall(
                    pathFinder: PathFinder,
                    blockSet: LongSet,
                    current: PathNode,
                    next: NeighbourNode
                ): Boolean {
                    return if (next.y > 0) {
                        for (y in current.y until current.y + pathFinder.stepHeight) {
                            if (checkWallAtY(blockSet, current, y)) return true
                        }
                        false
                    } else {
                        for (y in current.y downTo current.y - pathFinder.fallHeight) {
                            if (checkWallAtY(blockSet, current, y)) return true
                        }
                        false
                    }
                }

                private fun checkWallAtY(blockSet: LongSet, current: PathNode, y: Int): Boolean {
                    return blockSet.contains(current.x - 1, y, current.z)
                        || blockSet.contains(current.x + 1, y, current.z)
                        || blockSet.contains(current.x, y, current.z - 1)
                        || blockSet.contains(current.x, y, current.z + 1)
                        || blockSet.contains(current.x - 1, y, current.z - 1)
                        || blockSet.contains(current.x + 1, y, current.z - 1)
                        || blockSet.contains(current.x - 1, y, current.z + 1)
                        || blockSet.contains(current.x + 1, y, current.z + 1)
                }
            };

            abstract fun checkGround(
                pathFinder: PathFinder,
                blockSet: LongSet,
                current: PathNode,
                next: NeighbourNode
            ): Boolean
        }

        companion object {
            @JvmField
            val ZERO = NeighbourNode(0, 0, 0, Type.HORIZONTAL, emptyArray())
        }
    }

    class PathNode(
        val parent: PathNode?,
        val last: NeighbourNode,
        x: Int,
        y: Int,
        z: Int,
    ) : Node(x, y, z), Comparable<PathNode> {
        var g = Int.MAX_VALUE
        var h = Int.MAX_VALUE
        val f: Int
            get() = g + h

        constructor(x: Int, y: Int, z: Int) : this(null, NeighbourNode.ZERO, x, y, z)

        fun nextNode(neighbourNode: NeighbourNode): PathNode {
            return PathNode(
                this,
                neighbourNode,
                this.x + neighbourNode.x,
                this.y + neighbourNode.y,
                this.z + neighbourNode.z
            )
        }

        fun calculate(end: Node) {
            g = parent!!.g + last.cost
            h = distanceTo(end)
        }

        fun initCalc(start: Node, end: Node) {
            g = distanceTo(start)
            h = distanceTo(end)
        }

        override fun toString(): String {
            return "($x, $y, $z)"
        }


        override fun compareTo(other: PathNode): Int {
            return this.f.compareTo(other.f)
        }
    }

    open class Node(
        val x: Int,
        val y: Int,
        val z: Int,
    ) {
        fun distanceTo(other: Node): Int {
            return ((other.x - this.x).sq + (other.z - this.z).sq) * 2 + (other.y - this.y).sq / 2
        }

        fun toLong(): Long {
            return toLong(this.x, this.y, this.z)
        }

        fun toLong(other: Node): Long {
            return toLong(this.x + other.x, this.y + other.y, this.z + other.z)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Node) return false

            if (x != other.x) return false
            if (y != other.y) return false
            return z == other.z
        }

        override fun hashCode(): Int {
            var result = x
            result = 31 * result + y
            result = 31 * result + z
            return result
        }

        protected companion object {
            @JvmStatic
            fun LongSet.contains(x: Int, y: Int, z: Int): Boolean {
                return this.contains(toLong(x, y, z))
            }
        }
    }
}