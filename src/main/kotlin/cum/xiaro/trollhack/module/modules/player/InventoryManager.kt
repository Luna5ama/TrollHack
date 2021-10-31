package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.util.extension.fastCeil
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.player.PlayerTravelEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.event.safeParallelListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.process.PauseProcess.pauseBaritone
import cum.xiaro.trollhack.process.PauseProcess.unpauseBaritone
import cum.xiaro.trollhack.setting.settings.impl.collection.CollectionSetting
import cum.xiaro.trollhack.util.TimeUnit
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.inventory.InventoryTask
import cum.xiaro.trollhack.util.inventory.confirmedOrTrue
import cum.xiaro.trollhack.util.inventory.inventoryTask
import cum.xiaro.trollhack.util.inventory.operation.moveTo
import cum.xiaro.trollhack.util.inventory.operation.quickMove
import cum.xiaro.trollhack.util.inventory.operation.swapWith
import cum.xiaro.trollhack.util.inventory.operation.throwAll
import cum.xiaro.trollhack.util.inventory.slot.*
import cum.xiaro.trollhack.util.items.id
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

internal object InventoryManager : Module(
    name = "InventoryManager",
    category = Category.PLAYER,
    description = "Manages your inventory automatically",
    modulePriority = 10
) {
    private val defaultEjectList = linkedSetOf(
        "minecraft:grass",
        "minecraft:dirt",
        "minecraft:netherrack",
        "minecraft:gravel",
        "minecraft:sand",
        "minecraft:stone",
        "minecraft:cobblestone"
    )

    private val autoRefill0 = setting("Auto Refill", true)
    private val autoRefill by autoRefill0
    private val buildingMode by setting("Building Mode", false, autoRefill0.atTrue())
    var buildingBlockID by setting("Building Block ID", 0, 0..1000, 1, { false })
    private val refillThreshold by setting("Refill Threshold", 16, 1..63, 1, autoRefill0.atTrue())
    private val itemSaver0 = setting("Item Saver", false)
    private val itemSaver by itemSaver0
    private val duraThreshold by setting("Durability Threshold", 5, 1..50, 1, itemSaver0.atTrue())
    private val autoEject0 = setting("Auto Eject", false)
    private val autoEject by autoEject0
    private val fullOnly by setting("Only At Full", false, autoEject0.atTrue())
    private val pauseMovement by setting("Pause Movement", true)
    private val delay by setting("Delay Ticks", 1, 0..20, 1)
    val ejectList = setting(CollectionSetting("Eject List", defaultEjectList))

    enum class State {
        IDLE, SAVING_ITEM, REFILLING_BUILDING, REFILLING, EJECTING
    }

    private var currentState = State.IDLE
    private var lastTask: InventoryTask? = null

    override fun isActive(): Boolean {
        return isEnabled && currentState != State.IDLE
    }

    init {
        onDisable {
            lastTask = null
            unpauseBaritone()
        }

        safeListener<PlayerTravelEvent> {
            if (player.isSpectator || !pauseMovement) return@safeListener

            // Pause if it is not null and not confirmed
            val shouldPause = lastTask?.confirmed == false

            if (shouldPause) {
                player.setVelocity(0.0, mc.player.motionY, 0.0)
                it.cancel()
                pauseBaritone()
            } else {
                unpauseBaritone()
            }
        }

        safeParallelListener<TickEvent.Post> {
            if (player.isSpectator || mc.currentScreen is GuiContainer || !lastTask.confirmedOrTrue) return@safeParallelListener

            setState()

            lastTask = when (currentState) {
                State.SAVING_ITEM -> {
                    saveItem()
                }
                State.REFILLING_BUILDING -> {
                    refillBuilding()
                }
                State.REFILLING -> {
                    refill()
                }
                State.EJECTING -> {
                    eject()
                }
                State.IDLE -> {
                    null
                }
            }
        }
    }

    private fun SafeClientEvent.setState() {
        currentState = when {
            saveItemCheck() -> State.SAVING_ITEM
            refillBuildingCheck() -> State.REFILLING_BUILDING
            refillCheck() -> State.REFILLING
            ejectCheck() -> State.EJECTING
            else -> State.IDLE
        }
    }

    /* State checks */
    private fun SafeClientEvent.saveItemCheck(): Boolean {
        return itemSaver && checkDamage(player.heldItemMainhand)
    }

    private fun SafeClientEvent.refillBuildingCheck(): Boolean {
        if (!autoRefill || !buildingMode || buildingBlockID == 0) return false

        val totalCount = player.inventorySlots.countID(buildingBlockID)
        val hotbarCount = player.hotbarSlots.countID(buildingBlockID)

        return totalCount >= refillThreshold
            && (hotbarCount < refillThreshold
            || (getRefillableSlotBuilding() != null && currentState == State.REFILLING_BUILDING))
    }

    private fun SafeClientEvent.refillCheck(): Boolean {
        return autoRefill && getRefillableSlot() != null
    }

    private fun SafeClientEvent.ejectCheck(): Boolean {
        return autoEject && ejectList.isNotEmpty()
            && (!fullOnly || player.inventorySlots.firstEmpty() == null)
            && getEjectSlot() != null
    }
    /* End of state checks */

    /* Tasks */
    private fun SafeClientEvent.saveItem(): InventoryTask? {
        val currentSlot = player.currentHotbarSlot
        val itemStack = player.heldItemMainhand

        val undamagedItem = getUndamagedItem(itemStack.item.id)
        val emptySlot = player.inventorySlots.firstEmpty()

        return when {
            autoRefill && undamagedItem != null -> {
                inventoryTask {
                    postDelay(delay, TimeUnit.TICKS)
                    swapWith(undamagedItem, currentSlot)
                }
            }
            emptySlot != null -> {
                inventoryTask {
                    postDelay(delay, TimeUnit.TICKS)
                    swapWith(emptySlot, currentSlot)
                }
            }
            else -> {
                player.dropItem(false)
                null
            }
        }
    }

    private fun SafeClientEvent.refillBuilding() =
        player.storageSlots.firstID(buildingBlockID)?.let {
            inventoryTask {
                postDelay(delay, TimeUnit.TICKS)
                quickMove(it)
            }
        }

    private fun SafeClientEvent.refill() =
        getRefillableSlot()?.let { slotTo ->
            getCompatibleStack(slotTo.stack)?.let { slotFrom ->
                inventoryTask {
                    postDelay(delay, TimeUnit.TICKS)
                    moveTo(slotFrom, slotTo)
                }
            }
        }

    private fun SafeClientEvent.eject() =
        getEjectSlot()?.let {
            inventoryTask {
                postDelay(delay, TimeUnit.TICKS)
                throwAll(it)
            }
        }
    /* End of tasks */

    /**
     * Finds undamaged item with given ID in inventory, and return its slot
     *
     * @return Full inventory slot if undamaged item found, else return null
     */
    private fun SafeClientEvent.getUndamagedItem(itemID: Int) =
        player.storageSlots.firstID(itemID) {
            !checkDamage(it)
        }

    private fun checkDamage(itemStack: ItemStack) =
        itemStack.isItemStackDamageable
            && itemStack.itemDamage > itemStack.maxDamage * (1.0f - duraThreshold / 100.0f)

    private fun SafeClientEvent.getRefillableSlotBuilding(): Slot? {
        if (player.storageSlots.firstID(buildingBlockID) == null) return null

        return player.hotbarSlots.firstID(buildingBlockID) {
            it.isStackable && it.count < it.maxStackSize
        }
    }

    private fun SafeClientEvent.getRefillableSlot(): Slot? {
        val slots = player.hotbarSlots + player.offhandSlot
        return slots.firstByStack {
            !it.isEmpty
                && (!buildingMode || it.item.id != buildingBlockID)
                && (!autoEject || !ejectList.contains(it.item.registryName.toString()))
                && it.isStackable
                && it.count < (it.maxStackSize / 64.0f * refillThreshold).fastCeil()
                && getCompatibleStack(it) != null
        }
    }

    private fun SafeClientEvent.getCompatibleStack(stack: ItemStack): Slot? {
        return player.storageSlots.firstByStack {
            stack.isItemEqual(it) && ItemStack.areItemStackTagsEqual(stack, it)
        }
    }

    private fun SafeClientEvent.getEjectSlot(): Slot? {
        return player.inventorySlots.firstByStack {
            !it.isEmpty
                && (!buildingMode || it.item.id != buildingBlockID)
                && ejectList.contains(it.item.registryName.toString())
        }
    }
}