package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.process.PauseProcess.unpauseBaritone
import dev.luna5ama.trollhack.setting.settings.impl.collection.MapSetting
import dev.luna5ama.trollhack.util.inventory.InventoryTask
import dev.luna5ama.trollhack.util.inventory.confirmedOrTrue
import dev.luna5ama.trollhack.util.inventory.inventoryTask
import dev.luna5ama.trollhack.util.inventory.operation.throwAll
import dev.luna5ama.trollhack.util.inventory.slot.countEmpty
import dev.luna5ama.trollhack.util.inventory.slot.inventorySlots
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.minecraft.inventory.Slot
import net.minecraft.item.Item

internal object AutoEject : Module(
    name = "Auto Eject",
    category = Category.PLAYER,
    description = "Automatically ejects items from your inventory",
    modulePriority = 10
) {
    private val defaultEjectMap = Object2IntOpenHashMap(
        hashMapOf(
            "minecraft:grass" to 0,
            "minecraft:dirt" to 0,
            "minecraft:netherrack" to 0,
            "minecraft:gravel" to 0,
            "minecraft:sand" to 0,
            "minecraft:stone" to 0,
            "minecraft:cobblestone" to 0,
        )
    )

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