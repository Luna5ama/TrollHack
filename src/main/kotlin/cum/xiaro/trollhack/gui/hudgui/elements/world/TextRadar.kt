package cum.xiaro.trollhack.gui.hudgui.elements.world

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.util.math.MathUtils
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.gui.hudgui.LabelHud
import cum.xiaro.trollhack.manager.managers.FriendManager
import cum.xiaro.trollhack.module.modules.combat.AntiBot
import cum.xiaro.trollhack.util.delegate.AsyncCachedValue
import cum.xiaro.trollhack.util.graphics.color.ColorGradient
import cum.xiaro.trollhack.util.threads.runSafe
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.MobEffects

internal object TextRadar : LabelHud(
    name = "TextRadar",
    category = Category.WORLD,
    description = "List of players nearby"
) {
    private val health by setting("Health", true)
    private val ping by setting("Ping", false)
    private val combatPotion by setting("Combat Potion", true)
    private val distance by setting("Distance", true)
    private val friend by setting("Friend", true)
    private val maxEntries by setting("Max Entries", 8, 4..32, 1)
    private val range by setting("Range", 64, 16..256, 2)

    private val healthColorGradient = ColorGradient(
        ColorGradient.Stop(0.0f, ColorRGB(180, 20, 20)),
        ColorGradient.Stop(10.0f, ColorRGB(240, 220, 20)),
        ColorGradient.Stop(20.0f, ColorRGB(20, 232, 20))
    )

    private val pingColorGradient = ColorGradient(
        ColorGradient.Stop(0f, ColorRGB(101, 101, 101)),
        ColorGradient.Stop(0.1f, ColorRGB(20, 232, 20)),
        ColorGradient.Stop(20f, ColorRGB(20, 232, 20)),
        ColorGradient.Stop(150f, ColorRGB(20, 232, 20)),
        ColorGradient.Stop(300f, ColorRGB(150, 0, 0))
    )

    private val cacheList by AsyncCachedValue(50L) {
        runSafe {
            val list = world.playerEntities.toList().asSequence()
                .filter { it != null && !it.isDead && it.health > 0.0f }
                .filter { it != player && it != mc.renderViewEntity }
                .filter { !AntiBot.isBot(it) }
                .filter { friend || !FriendManager.isFriend(it.name) }
                .map { it to player.getDistance(it) }
                .filter { it.second <= range }
                .sortedBy { it.second }
                .toList()

            remainingEntries = list.size - maxEntries
            list.take(maxEntries)
        } ?: emptyList()
    }
    private var remainingEntries = 0

    override fun SafeClientEvent.updateText() {
        cacheList.forEach {
            addHealth(it.first)
            addName(it.first)
            addPing(it.first)
            addPotion(it.first)
            addDist(it.second)
            displayText.currentLine++
        }
        if (remainingEntries > 0) {
            displayText.addLine("...and $remainingEntries more")
        }
    }

    private fun addHealth(player: EntityPlayer) {
        if (health) {
            val hp = MathUtils.round(player.health, 1).toString()
            displayText.add(hp, healthColorGradient.get(player.health))
        }
    }

    private fun addName(player: EntityPlayer) {
        val color = if (FriendManager.isFriend(player.name)) ColorRGB(32, 255, 32) else primaryColor
        displayText.add(player.name, color)
    }

    private fun SafeClientEvent.addPing(player: EntityPlayer) {
        if (ping) {
            val ping = connection.getPlayerInfo(player.name)?.responseTime ?: 0
            val color = pingColorGradient.get(ping.toFloat())
            displayText.add("${ping}ms", color)
        }
    }

    private fun addPotion(player: EntityPlayer) {
        if (combatPotion) {
            if (player.isPotionActive(MobEffects.WEAKNESS)) displayText.add("W", secondaryColor)
            if (player.isPotionActive(MobEffects.STRENGTH)) displayText.add("S", secondaryColor)
        }
    }

    private fun addDist(distIn: Float) {
        if (distance) {
            val dist = MathUtils.round(distIn, 1)
            displayText.add(dist.toString(), secondaryColor)
        }
    }
}