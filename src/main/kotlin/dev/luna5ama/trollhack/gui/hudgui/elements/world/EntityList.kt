package dev.luna5ama.trollhack.gui.hudgui.elements.world

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.EntityUtils.isHostile
import dev.luna5ama.trollhack.util.EntityUtils.isNeutral
import dev.luna5ama.trollhack.util.EntityUtils.isPassive
import dev.luna5ama.trollhack.util.delegate.AsyncCachedValue
import dev.luna5ama.trollhack.util.inventory.originalName
import dev.luna5ama.trollhack.util.math.vector.distanceTo
import dev.luna5ama.trollhack.util.threads.runSafe
import net.minecraft.entity.Entity
import net.minecraft.entity.item.*
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityEgg
import net.minecraft.entity.projectile.EntitySnowball
import net.minecraft.entity.projectile.EntityWitherSkull
import java.util.*

internal object EntityList : LabelHud(
    name = "Entity List",
    category = Category.WORLD,
    description = "List of entities nearby"
) {
    private val item by setting("Items", true)
    private val passive by setting("Passive Mobs", true)
    private val neutral by setting("Neutral Mobs", true)
    private val hostile by setting("Hostile Mobs", true)
    private val maxEntries by setting("Max Entries", 8, 4..32, 1)
    private val range by setting("Range", 64, 16..256, 16, fineStep = 1)

    private val cacheMap by AsyncCachedValue(50L) {
        val map = TreeMap<String, Int>()

        runSafe {
            for (entity in EntityManager.entity) {
                if (entity == player || entity == mc.renderViewEntity) continue

                if (!item && entity is EntityItem) continue
                if (!passive && entity.isPassive) continue
                if (!neutral && entity.isNeutral) continue
                if (!hostile && entity.isHostile) continue

                if (player.distanceTo(entity) > range) continue

                val name = entity.entityListName

                if (entity is EntityItem) {
                    map[name] = map.getOrDefault(name, 0) + entity.item.count
                } else {
                    map[name] = map.getOrDefault(name, 0) + 1
                }
            }
        }

        remainingEntries = map.size - maxEntries
        map.entries.take(maxEntries)
    }
    private var remainingEntries = 0

    override fun SafeClientEvent.updateText() {
        for ((name, count) in cacheMap) {
            displayText.add(name, GuiSetting.text)
            displayText.addLine("x$count", GuiSetting.primary)
        }
        if (remainingEntries > 0) {
            displayText.addLine("...and $remainingEntries more")
        }
    }

    private val Entity.entityListName
        get() = when (this) {
            is EntityPlayer -> {
                "Player"
            }
            is EntityItem -> {
                this.item.originalName
            }
            is EntityWitherSkull -> {
                "Wither skull"
            }
            is EntityEnderCrystal -> {
                "End crystal"
            }
            is EntityEnderPearl -> {
                "Thrown ender pearl"
            }
            is EntityMinecart -> {
                "Minecart"
            }
            is EntityItemFrame -> {
                "Item frame"
            }
            is EntityEgg -> {
                "Thrown egg"
            }
            is EntitySnowball -> {
                "Thrown snowball"
            }
            else -> {
                this.name ?: this.javaClass.simpleName
            }
        }
}