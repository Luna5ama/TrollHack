package me.luna.trollhack.module.modules.player

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.TickEvent
import me.luna.trollhack.event.safeParallelListener
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.process.PauseProcess.unpauseBaritone
import me.luna.trollhack.setting.settings.impl.collection.MapSetting
import me.luna.trollhack.util.TimeUnit
import me.luna.trollhack.util.atTrue
import me.luna.trollhack.util.extension.fastCeil
import me.luna.trollhack.util.inventory.InventoryTask
import me.luna.trollhack.util.inventory.confirmedOrTrue
import me.luna.trollhack.util.inventory.inventoryTask
import me.luna.trollhack.util.inventory.operation.moveTo
import me.luna.trollhack.util.inventory.operation.quickMove
import me.luna.trollhack.util.inventory.operation.swapWith
import me.luna.trollhack.util.inventory.operation.throwAll
import me.luna.trollhack.util.inventory.slot.*
import me.luna.trollhack.util.items.id
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

internal object AutoEject : Module(
    name = "AutoEject",
    category = Category.PLAYER,
    description = "Automatically ejects items from your inventory",
    modulePriority = 10
) {
    private val defaultEjectMap = Object2IntOpenHashMap(hashMapOf(
        "minecraft:grass" to 0,
        "minecraft:dirt" to 0,
        "minecraft:netherrack" to 0,
        "minecraft:gravel" to 0,
        "minecraft:sand" to 0,
        "minecraft:stone" to 0,
        "minecraft:cobblestone" to 0,
    ))

    private val fullOnly by setting("Only At Full", false)
    private val delayMs by setting("Delay ms", 50, 0..1000, 5)
    val ejectMap = setting(MapSetting("Eject Map", defaultEjectMap))

    private var lastTask: InventoryTask? = null

    init {
        onDisable {
            lastTask = null
            unpauseBaritone()
        }

        safeParallelListener<TickEvent.Post> {
            if (!lastTask.confirmedOrTrue) return@safeParallelListener
            if (ejectMap.value.isEmpty()) return@safeParallelListener
            if (fullOnly && player.inventorySlots.countEmpty() > 0) return@safeParallelListener

            getEjectSlot()?.let {
                inventoryTask {
                    throwAll(it)
                    runInGui()
                    delay(0)
                    postDelay(delayMs)
                }
            }
        }
    }

    private fun SafeClientEvent.getEjectSlot(): Slot? {
        val countMap = Object2IntOpenHashMap<Item>()
        countMap.defaultReturnValue(0)

        for (slot in player.inventoryContainer.inventorySlots) {
            val stack = slot.stack
            val item = stack.item

            if (stack.isEmpty) continue

            val ejectThreshold = ejectMap.value[item.registryName.toString()] ?: continue
            countMap.put(item, countMap.getInt(item) + 1)
            if (countMap.getInt(item) > ejectThreshold) {
                return slot
            }
        }

        return null
    }
}