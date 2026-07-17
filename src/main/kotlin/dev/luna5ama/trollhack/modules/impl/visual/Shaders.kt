package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.Render3DEvent
import dev.luna5ama.trollhack.graphics.blaze3d.Render3DScheduler
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.Displayable
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.player.Player

object Shaders : Module("Shaders", category = Category.RENDER) {
    private val hands by setting("Hands", true)
    private val players by setting("Players", true)
    private val self by setting("Self", true, { players })
    private val friends by setting("Friends", true)
    private val crystals by setting("Crystals", true)
    private val chests by setting("Chests", true)
    private val creatures by setting("Creatures", false)
    private val monsters by setting("Monsters", false)
    private val ambients by setting("Ambients", false)
    private val others by setting("Others", false)
    private val maxRange by setting("Max Range", 64, 16..256, 1)
    private val mode by setting("Mode", ShaderMode.DEFAULT)
    private val handsMode by setting("Hands Mode", ShaderMode.DEFAULT)
    private val chestMode by setting("Chest Mode", ShaderMode.DEFAULT)
    private val lineWidth by setting("Line Width", 2.0f, 0.5f..8.0f, 0.5f)
    private val fillColor by setting("Fill", ColorRGBA(255, 255, 255, 70))
    private val outlineColor by setting("Outline", ColorRGBA(255, 255, 255, 180))

    init {
        nonNullHandler<Render3DEvent> {
            val rangeSq = maxRange.toDouble() * maxRange
            for (entity in world.entitiesForRendering()) {
                if (player.distanceToSqr(entity) > rangeSq || !shouldRender(entity)) continue
                Render3DScheduler.addFilledBox(entity.boundingBox, fillColor, through = true)
            }
        }
    }

    override fun getDisplayInfo(): Any = mode.displayName

    @JvmStatic
    fun outlineArgb(entity: net.minecraft.world.entity.Entity): Int {
        if (!isEnabled || !shouldRender(entity) || outlineColor.a <= 0) return 0
        return (255 shl 24) or (outlineColor.r shl 16) or (outlineColor.g shl 8) or outlineColor.b
    }

    private fun shouldRender(entity: net.minecraft.world.entity.Entity): Boolean {
        val player = dev.luna5ama.trollhack.utils.MinecraftWrapper.mc.player ?: return false
        if (entity is Player) {
            if (entity == player && !self) return false
            if (FriendManager.isFriend(entity.name.string)) return friends
            return players
        }
        if (entity is EndCrystal) return crystals
        return when (entity.type.category) {
            MobCategory.CREATURE, MobCategory.WATER_CREATURE -> creatures
            MobCategory.MONSTER -> monsters
            MobCategory.AMBIENT, MobCategory.WATER_AMBIENT -> ambients
            else -> others
        }
    }

    enum class ShaderMode(override val displayName: CharSequence) : Displayable {
        DEFAULT("Default (Vanilla Outline)"),
        SMOKE("Smoke (Default Fallback)"),
        GRADIENT("Gradient (Default Fallback)"),
        SNOW("Snow (Default Fallback)"),
        FADE("Fade (Default Fallback)")
    }
}
