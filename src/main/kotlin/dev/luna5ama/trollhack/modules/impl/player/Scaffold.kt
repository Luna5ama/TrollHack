package dev.luna5ama.trollhack.modules.impl.player

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.LoopEvent
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.event.impl.player.InputUpdateEvent
import dev.luna5ama.trollhack.event.impl.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.impl.player.PlayerMoveEvent
import dev.luna5ama.trollhack.event.impl.render.Render3DEvent
import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import dev.luna5ama.trollhack.manager.managers.EntityMovementManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.Override
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.doSwap
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.RotationManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.compat.clearDirectionalInputCompat
import dev.luna5ama.trollhack.utils.compat.forwardImpulseCompat
import dev.luna5ama.trollhack.utils.compat.leftImpulseCompat
import dev.luna5ama.trollhack.utils.extension.realSpeed
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.utils.extension.velocityX
import dev.luna5ama.trollhack.utils.extension.velocityY
import dev.luna5ama.trollhack.utils.extension.velocityZ
import dev.luna5ama.trollhack.utils.extension.yaw
import dev.luna5ama.trollhack.utils.inventory.currentHotbarSlot
import dev.luna5ama.trollhack.utils.inventory.everySlots
import dev.luna5ama.trollhack.utils.inventory.hotbarSlots
import dev.luna5ama.trollhack.utils.inventory.offhandSlot
import dev.luna5ama.trollhack.utils.math.RotationUtils
import dev.luna5ama.trollhack.utils.math.floorToInt
import dev.luna5ama.trollhack.utils.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.utils.math.vectors.Vec2d
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.rotation.Priority
import dev.luna5ama.trollhack.utils.runSafe
import dev.luna5ama.trollhack.utils.timing.TickTimer
import dev.luna5ama.trollhack.utils.world.*
import dev.luna5ama.trollhack.utils.world.EntityUtils.spoofSneak
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Input
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.BlockItem
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3

object Scaffold : Module(
    name = "Scaffold",
    category = Category.PLAYER,
    description = "Places blocks under you",
    modulePriority = 500
) {
    private enum class BridgeMode(override val displayName: CharSequence) : dev.luna5ama.trollhack.utils.Displayable {
        TELLY_BRIDGE("TellyBridge"),
        GOD_BRIDGE("GodBridge")
    }

    private enum class SwapMode(override val displayName: CharSequence) : dev.luna5ama.trollhack.utils.Displayable {
        NONE("None"),
        NORMAL("Normal"),
        SILENT("Silent"),
        INV_SWITCH("InvSwitch")
    }

    private enum class RotationMode(override val displayName: CharSequence) : dev.luna5ama.trollhack.utils.Displayable {
        RISE("Rise"),
        HYPIXEL("Hypixel")
    }

    private enum class RaytraceMode(override val displayName: CharSequence) : dev.luna5ama.trollhack.utils.Displayable {
        NORMAL("Normal"),
        STRICT("Strict")
    }

    private val mode by setting("Mode", BridgeMode.TELLY_BRIDGE)
    private val swapMode by setting("Swap Mode", SwapMode.NORMAL)
    private val swapBack by setting("Swap Back", true, { swapMode == SwapMode.NORMAL })
    private val skipTicks by setting("Skip Ticks", false)
    private val snap by setting("Snap", false, { mode == BridgeMode.GOD_BRIDGE })
    private val rotationMode by setting("Rotation Mode", RotationMode.RISE)
    private val raytraceMode by setting("Raytrace Mode", RaytraceMode.NORMAL)
    private val rotationSpeed by setting("Rotation Speed", 180, 10..180, 10, { rotationMode == RotationMode.RISE })
    private val rotationBackSpeed by setting("Rotation Back Speed", 180, 10..180, 10, { mode == BridgeMode.TELLY_BRIDGE })
    private val tellyTicks by setting("Telly Ticks", 1, 0..6, 1, { mode == BridgeMode.TELLY_BRIDGE })

    private val blockPlaceRotation by setting("Rotation", true)
    private val safeWalk by setting("Safe Walk", true)
    private val setbackOnFailure by setting("Setback on Failure", true)
    private val setbackOnFalling by setting("Setback on Falling", true)
    private val grimMode by setting("Grim",false)
    private val towerMode by setting("Tower Mode", true)
    private val towerMotion by setting("Tower Jump Motion", 0.42f, 0.0f..1.0f, 0.01f, { _ -> towerMode })
    private val towerJumpHeight by setting("Tower Jump Height Threshold", 0.05f, 0.0f..1.0f, 0.01f, { _ -> towerMode })
    private val towerPlaceHeight by setting("Tower Place Height Threshold", 1.1f, 0.0f..1.5f, 0.01f, { _ -> towerMode })
    private val towerChainLimit by setting("Tower Chain Limit", 10, 1..20, 1, { _ -> towerMode })
    private val towerCooldown by setting("Tower Cooldown", 1, 1..5, 1, { _ -> towerMode })
    private val towerFailureTimeout by setting("Tower Failure Timeout", 500, 0..5000, 1, { _ -> towerMode })
    private val maxPendingPlace by setting("Max Pending Place", 2, 1..5, 1)
    private val placeTimeout by setting("Place Timeout", 200, 0..5000, 50)
    private val placeDelay by setting("Place Delay", 100, 0..1000, 1)
    private val extendAxis by setting("Extend Axis", 0.02f, 0.0f..0.1f, 0.001f)
    private val extendDiagonal by setting("Extend Diagonal", 0.02f, 0.0f..0.1f, 0.001f)
    private val maxDepth by setting("Max Depth", 5, 0..10)

    private val towerFailTimer = TickTimer()
    private var lastJumpHeight = Int.MIN_VALUE
    private var towerCount = -1
    private var lastPos: Vec3? = null
    private var lastSequence: List<PlaceInfo>? = null
    private val placingPos = LongOpenHashSet()
    private val pendingPlace = Long2LongOpenHashMap().apply {
        defaultReturnValue(-1L)
    }

    private val placeTimer = TickTimer()
    private val renderer = ESPRenderer().apply { aFilled = 31; aOutline = 233 }
    private var lastRotation: Vec2f? = null
    private var airTicks = 0
    private var yLevel = Int.MIN_VALUE
    private var rotateCount = 0
    private var originalHotbarSlot = -1
    private var tellyAutoJump = false

    val shouldSafeWalk: Boolean
        get() = isEnabled && safeWalk

    init {
        onDisabled {
            resetTower()
            EntityMovementManager.isSafeWalk = false
            lastPos = null
            lastSequence = null
            lastRotation = null
            airTicks = 0
            yLevel = Int.MIN_VALUE
            rotateCount = 0
            tellyAutoJump = false
            runSafe { restoreNormalSlot() }
            placingPos.clear()
            pendingPlace.clear()
            placeTimer.reset(-69420L)
        }

        handler<WorldEvent.Load> {
            disable()
        }

        nonNullHandler<PacketEvent.PostReceive> {
            if (it.packet !is ClientboundPlayerPositionPacket) return@nonNullHandler
            towerFailTimer.reset()
        }

        nonNullHandler<WorldEvent.ClientBlockUpdate> {
            if (setbackOnFailure && pendingPlace.remove(it.pos.asLong()) != -1L && it.newState.canBeReplaced()) {
                sendSetbackPacket()
            }

            if (!placingPos.contains(it.pos.asLong())) return@nonNullHandler
            updateSequence()
        }

        nonNullHandler<InputUpdateEvent> {
            tellyAutoJump = false
            if (mode == BridgeMode.TELLY_BRIDGE && player.onGround() &&
                !it.movementInput.keyPresses.jump &&
                (it.movementInput.forwardImpulseCompat != 0.0f || it.movementInput.leftImpulseCompat != 0.0f)
            ) {
                tellyAutoJump = true
                val input = it.movementInput.keyPresses
                it.movementInput.keyPresses = Input(
                    input.forward(), input.backward(), input.left(), input.right(),
                    true, input.shift(), input.sprint()
                )
            }
            if (isTowering()) {
                it.movementInput.clearDirectionalInputCompat()
            }
        }

        nonNullHandler<PlayerMoveEvent.Pre>(-9999) {
            if (player.onGround()) {
                airTicks = 0
                yLevel = player.y.floorToInt() - 1
            } else {
                airTicks++
            }

            if (!isTowering()) {
                resetTower()
                return@nonNullHandler
            }

            player.deltaMovement = Vec3(0.0, player.velocityY, 0.0)

            if (!towerFailTimer.tick(towerFailureTimeout)) {
                resetTower()
                return@nonNullHandler
            }

            val floorY = player.y.floorToInt()
            if (player.y - floorY > towerJumpHeight) return@nonNullHandler
            if (floorY == lastJumpHeight) return@nonNullHandler

            lastJumpHeight = floorY
            towerCount++

            if (towerCount <= 0) return@nonNullHandler
            if (towerCount > towerChainLimit) {
                towerCount = -towerCooldown
                return@nonNullHandler
            }

            player.deltaMovement = Vec3(player.velocityX, towerMotion.toDouble(), player.velocityZ)
        }

        nonNullHandler<PlayerMoveEvent.Post> {
            if (setbackOnFalling && player.fallDistance > 3.0f) {
                sendSetbackPacket()
                return@nonNullHandler
            }

            val currentTime = System.currentTimeMillis()
            val prevSize = pendingPlace.size
            pendingPlace.values.removeIf {
                it < currentTime
            }
            if (setbackOnFailure && pendingPlace.size > prevSize) {
                sendSetbackPacket()
                return@nonNullHandler
            }
            updateSequence()
        }

        nonNullHandler<Render3DEvent> {
            renderer.render(false)
        }

        nonNullHandler<OnUpdateWalkingPlayerEvent.Pre> {
            if (!blockPlaceRotation) return@nonNullHandler
            val placeInfo = nextPlaceInfo()
            val shouldRotate = mode == BridgeMode.TELLY_BRIDGE || !isBridgeAir() || !snap
            if (skipTicks && placeInfo != null) {
                rotateCount++
            } else {
                rotateCount = 0
            }

            if (mode == BridgeMode.TELLY_BRIDGE && player.onGround()) {
                val fallback = lastRotation ?: Vec2f(RotationUtils.normalizeAngle(player.yaw - 180.0f), 80.0f)
                RotationManager.setRotations(fallback, rotationBackSpeed.toDouble(), priority = Priority.High)
            } else if (placeInfo != null && shouldRotate) {
                val target = getBridgeRotation(placeInfo)
                lastRotation = target
                RotationManager.setRotations(
                    target,
                    rotationSpeedForTick(),
                    raytrace = { rotation -> isRotationValid(rotation, placeInfo, raytraceMode == RaytraceMode.STRICT) },
                    priority = Priority.High
                )
            }
        }

        nonNullHandler<OnUpdateWalkingPlayerEvent.Post> {
            runPlacing()
            EntityMovementManager.isSafeWalk = shouldSafeWalk
        }

        nonNullHandler<LoopEvent.Tick> {
            runPlacing()
            EntityMovementManager.isSafeWalk = shouldSafeWalk
        }
    }

    private fun resetTower() {
        lastJumpHeight = Int.MIN_VALUE
        towerCount = 0
    }

    private fun NonNullContext.sendSetbackPacket() {
        netHandler.send(ServerboundMovePlayerPacket.Pos(player.x, player.y + 10.0, player.z, false, player.horizontalCollision))
    }

    private fun NonNullContext.runPlacing() {
        if (pendingPlace.size >= maxPendingPlace) return
        if (mode == BridgeMode.TELLY_BRIDGE && !isTowering() && (player.onGround() || airTicks <= tellyTicks)) return
        if (skipTicks && rotateCount > 8) rotateCount = 1
        if (!skipTicks && !placeTimer.tick(placeDelay)) return
        if (skipTicks && !placeTimer.tick(1)) return

        lastSequence?.let {
            getBlockSlot() ?: return
            for (placeInfo in it) {
                if (pendingPlace.containsKey(placeInfo.placedPos.asLong())) continue
                if (isSideVisible(player.eyePosition.x, player.eyePosition.y, player.eyePosition.z, placeInfo.pos, placeInfo.direction)) {
                    if (isTowering() && player.y - placeInfo.placedPos.y <= towerPlaceHeight) return
                    if (blockPlaceRotation && !checkPlaceRotation(placeInfo)) return
                    if (!placeWithSwap(placeInfo)) return
                    pendingPlace.put(placeInfo.placedPos.asLong(), System.currentTimeMillis() + placeTimeout)
                    placeTimer.reset()
                    rotateCount = 0
                    break
                }
            }

            renderer.clear()
            for (info in it) {
                val posLong = info.pos.asLong()
                val placedLong = info.placedPos.asLong()
                placingPos.add(posLong)
                placingPos.add(placedLong)

                val box = AABB(info.placedPos)
                if (pendingPlace.containsKey(placedLong)) {
                    renderer.add(box, ColorRGBA(64, 255, 64))
                } else {
                    renderer.add(box, ColorRGBA(255, 255, 255))
                }
            }
        }
    }

    private fun NonNullContext.getBlockSlot(): BlockSelection? {
        if (isValidBlock(player.offhandSlot.item)) return BlockSelection(player.offhandSlot, InteractionHand.OFF_HAND)
        if (swapMode == SwapMode.NONE) {
            return player.currentHotbarSlot.takeIf { isValidBlock(it.item) }
                ?.let { BlockSelection(it, InteractionHand.MAIN_HAND) }
        }
        val slots = if (swapMode == SwapMode.INV_SWITCH) player.everySlots else player.hotbarSlots
        val slot = slots.firstOrNull { isValidBlock(it.item) } ?: return null
        return BlockSelection(slot, InteractionHand.MAIN_HAND)
    }

    private fun NonNullContext.placeWithSwap(placeInfo: PlaceInfo): Boolean {
        val selection = getBlockSlot() ?: return false
        var placed = false
        val action = {
            val rotation = RotationManager.rotation
            if (!blockPlaceRotation || isRotationValid(rotation, placeInfo, raytraceMode == RaytraceMode.STRICT)) {
                player.spoofSneak { placeBlock(placeInfo, selection.hand) }
                placed = true
            }
        }

        when (swapMode) {
            SwapMode.NONE -> {
                if (selection.hand == InteractionHand.OFF_HAND || selection.slot.index == player.inventory.selectedSlot + 36) {
                    action()
                } else return false
            }
            SwapMode.NORMAL -> {
                if (selection.hand == InteractionHand.MAIN_HAND) {
                    val target = selection.slot.index - 36
                    if (target !in 0..8) return false
                    if (player.inventory.selectedSlot != target) {
                        if (originalHotbarSlot == -1) originalHotbarSlot = player.inventory.selectedSlot
                        doSwap(target)
                    }
                }
                action()
            }
            SwapMode.SILENT -> {
                if (selection.hand == InteractionHand.OFF_HAND) action()
                else ghostSwitch(Override.NONE, selection.slot) { action() }
            }
            SwapMode.INV_SWITCH -> {
                if (selection.hand == InteractionHand.OFF_HAND) {
                    action()
                } else {
                    val selected = player.inventory.selectedSlot
                    if (selection.slot.index != selected + 36) {
                        interaction.handleInventoryMouseClick(player.containerMenu.containerId, selection.slot.index, selected, ClickType.SWAP, player)
                        action()
                        interaction.handleInventoryMouseClick(player.containerMenu.containerId, selection.slot.index, selected, ClickType.SWAP, player)
                    } else action()
                }
            }
        }
        return placed
    }

    private fun NonNullContext.restoreNormalSlot() {
        if (originalHotbarSlot !in 0..8) return
        if (swapBack) doSwap(originalHotbarSlot)
        originalHotbarSlot = -1
    }

    private fun NonNullContext.nextPlaceInfo(): PlaceInfo? {
        return lastSequence?.firstOrNull {
            !pendingPlace.containsKey(it.placedPos.asLong()) &&
                isSideVisible(player.eyePosition.x, player.eyePosition.y, player.eyePosition.z, it.pos, it.direction)
        }
    }

    private fun NonNullContext.getBridgeRotation(placeInfo: PlaceInfo): Vec2f {
        val direct = getRotationTo(placeInfo.hitVec)
        if (rotationMode == RotationMode.RISE) return direct
        return Vec2f(RotationUtils.normalizeAngle(player.yaw - 180.0f), 82.0f)
    }

    private fun rotationSpeedForTick(): Double {
        return if (rotationMode == RotationMode.HYPIXEL) {
            if (airTicks <= 1) 127.0 else 35.0
        } else rotationSpeed.toDouble()
    }

    private fun NonNullContext.isRotationValid(rotation: Vec2f, placeInfo: PlaceInfo, strict: Boolean): Boolean {
        val end = RotationUtils.traceRotation(player.eyePosition, rotation, 6.0)
        val hit = world.clip(ClipContext(player.eyePosition, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player))
        return hit is BlockHitResult && hit.blockPos == placeInfo.pos && (!strict || hit.direction == placeInfo.direction)
    }

    private fun NonNullContext.isBridgeAir(): Boolean {
        val feetY = bridgeYLevel()
        return world.getBlockState(BlockPos(player.x.floorToInt(), feetY, player.z.floorToInt())).canBeReplaced()
    }

    private fun NonNullContext.bridgeYLevel(): Int {
        val moving = player.input.forwardImpulseCompat != 0.0f || player.input.leftImpulseCompat != 0.0f
        return if (mode == BridgeMode.TELLY_BRIDGE && yLevel != Int.MIN_VALUE &&
            (!player.input.keyPresses.jump || tellyAutoJump) && moving && player.fallDistance <= 0.25f
        ) yLevel else player.y.floorToInt() - 1
    }

    private fun isValidBlock(stack: net.minecraft.world.item.ItemStack): Boolean {
        return !stack.isEmpty && stack.item is BlockItem
    }

    private data class BlockSelection(val slot: Slot, val hand: InteractionHand)

    private val towerSides = arrayOf(
        Direction.DOWN,
        Direction.NORTH,
        Direction.SOUTH,
        Direction.EAST,
        Direction.WEST
    )

    private fun NonNullContext.updateSequence() {
        lastPos = player.position()
        lastSequence = null
        renderer.clear()

        val sequence = if (isTowering()) {
            val feetPos = world.getGroundPos(player)
            getPlacementSequence(
                feetPos,
                maxDepth,
                towerSides,
                PlacementSearchOption.ENTITY_COLLISION_IGNORE_SELF,
            )
        } else {
            val feetY = bridgeYLevel()
            val feetPos = BlockPos.MutableBlockPos()
            calcSortedOffsets().asSequence()
                .mapNotNull { getSequence(feetPos.set(it.x.floorToInt(), feetY, it.y.floorToInt())) }
                .firstOrNull()
        }

        lastSequence = sequence

        if (sequence != null) {
            for (info in sequence) {
                val posLong = info.pos.asLong()
                val placedLong = info.placedPos.asLong()
                placingPos.add(posLong)
                placingPos.add(placedLong)

                val box = AABB(info.placedPos)
                if (pendingPlace.containsKey(placedLong)) {
                    renderer.add(box, ColorRGBA(64, 255, 64))
                } else {
                    renderer.add(box, ColorRGBA(255, 255, 255))
                }
            }
        }
    }

    private fun NonNullContext.calcSortedOffsets(): Array<Vec2d> {
        val center = Vec2d(player.x, player.z)
        if (player.realSpeed < 0.05) return arrayOf(center)

        val w = player.bbWidth / 2.0
        val axis = w + extendAxis
        val diag = w + extendDiagonal
        val results = arrayOf(
            center,
            center.plus(axis, 0.0),
            center.plus(-axis, 0.0),
            center.plus(0.0, axis),
            center.plus(0.0, -axis),
            center.plus(diag, diag),
            center.plus(diag, -diag),
            center.plus(-diag, diag),
            center.plus(-diag, -diag)
        )
        val bX = player.x.floorToInt() + 0.5
        val bZ = player.z.floorToInt() + 0.5
        val oX = player.x - bX
        val oZ = player.z - bZ
        results.sortWith(compareByDescending {
            oX * (it.x - bX) + oZ * (it.y - bZ)
        }, 1, results.size)
        return results
    }

    private fun NonNullContext.getSequence(feetPos: BlockPos): List<PlaceInfo>? {
        if (!world.getBlockState(feetPos).canBeReplaced()) {
            return null
        }

        return getPlacementSequence(
            feetPos,
            maxDepth,
            PlacementSearchOption.ENTITY_COLLISION,
        )
    }

    private fun NonNullContext.isTowering(): Boolean {
        return towerMode && player.input.keyPresses.jump && !tellyAutoJump
    }

    context(ctx: NonNullContext)
    private fun isOffsetBBEmpty(x: Double, z: Double): Boolean = ctx.run {
        return !world.getBlockCollisions(
            player,
            player.boundingBox.inflate(-0.1, 0.1, -0.1).move(x, -2.0, z)
        ).iterator().hasNext()
    }
}
