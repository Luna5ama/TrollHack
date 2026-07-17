package dev.luna5ama.trollhack.modules.impl.combat

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.RotationManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.utils.inventory.everySlots
import dev.luna5ama.trollhack.utils.inventory.hotbarSlots
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.utils.rotation.Priority
import dev.luna5ama.trollhack.utils.world.*
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Items
import net.minecraft.world.inventory.Slot
import net.minecraft.world.phys.AABB

object FeetTrap : Module("Feet Trap", category = Category.COMBAT) {
    private val toggleOnMove by setting("Toggle On Move", true)
    private val toggleOnJump by setting("Toggle On Jump", true)
    private val inAir by setting("In Air", true)
    private val pauseOnEat by setting("Pause On Eat", true)
    private val placeDelay by setting("Place Delay", 50, 0..500, 10)
    private val extend by setting("Extend", true)
    private val blocksPerTick by setting("Blocks Per Tick", 1, 1..8, 1)
    private val rotate by setting("Rotate", true)
    private val rotationSpeed by setting("Rotation Speed", 180f, 18f..180f, 18f, { rotate })
    private val enderChest by setting("Ender Chest", true)
    private val inventorySwap by setting("Inventory Swap", true)

    private var origin = BlockPos.ZERO
    private var lastPlace = 0L

    init {
        onEnabled {
            val player = mc.player ?: return@onEnabled
            origin = player.blockPosition()
            lastPlace = 0L
        }
        nonNullHandler<TickEvent.Pre> {
            if (toggleOnMove && player.blockPosition().distSqr(origin) > 1.0) {
                disable()
                return@nonNullHandler
            }
            if (toggleOnJump && player.input.keyPresses.jump) {
                disable()
                return@nonNullHandler
            }
            if (pauseOnEat && player.isUsingItem) return@nonNullHandler
            if (!inAir && !player.onGround()) return@nonNullHandler
            if (System.currentTimeMillis() - lastPlace < placeDelay) return@nonNullHandler
            val slot = findBlockSlot()
            if (slot == null) {
                disable()
                return@nonNullHandler
            }
            val targets = LinkedHashSet<BlockPos>().apply {
                val feet = player.blockPosition()
                add(feet.below())
                addAll(Direction.Plane.HORIZONTAL.map { feet.relative(it) })
                addAll(Direction.Plane.HORIZONTAL.map { feet.above().relative(it) })
                if (extend) {
                    val occupied = toList().filter { target ->
                        world.getEntitiesOfClass(net.minecraft.world.entity.Entity::class.java, AABB(target))
                            .any { it != player }
                    }
                    occupied.forEach { target ->
                        Direction.Plane.HORIZONTAL.forEach { add(target.relative(it)) }
                    }
                }
            }
            var placed = 0
            for (target in targets) {
                if (placed >= blocksPerTick || !world.getBlockState(target).canBeReplaced()) continue
                val sequence = getPlacementSequence(target, 3, PlacementSearchOption.ENTITY_COLLISION_IGNORE_SELF) ?: continue
                for (info in sequence) {
                    if (placed >= blocksPerTick) break
                    if (rotate) {
                        RotationManager.setRotations(
                            getRotationTo(info.hitVec),
                            rotationSpeed.toDouble(),
                            priority = Priority.High
                        )
                    }
                    ghostSwitch(slot) {
                        placeBlock(info, InteractionHand.MAIN_HAND)
                    }
                    placed++
                }
            }
            if (placed > 0) lastPlace = System.currentTimeMillis()
        }
    }

    private fun findBlockSlot(): Slot? {
        val slots = if (inventorySwap) mc.player?.everySlots else mc.player?.hotbarSlots
        slots ?: return null
        return slots.firstOrNull { it.item.item is BlockItem && (it.item.item == Items.OBSIDIAN || enderChest && it.item.item == Items.ENDER_CHEST) }
    }
}
