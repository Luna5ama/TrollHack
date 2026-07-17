package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.graphics.blaze3d.ShaderHolder
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.Displayable
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
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
    val mode by setting("Mode", ShaderMode.DEFAULT)
    val handsMode by setting("Hands Mode", ShaderMode.DEFAULT)
    val chestMode by setting("Chest Mode", ShaderMode.DEFAULT)

    val maxRange by setting("Max Range", 64, 16..256, 1, visibility = {
        players || crystals || chests || friends || creatures || monsters || ambients || others
    })
    val factor by setting("Gradient Factor", 2.0, 0.0..20.0, 0.1, visibility = {
        usesMode(ShaderMode.GRADIENT)
    })
    val gradient by setting("Gradient", 2.0, 0.0..20.0, 0.1, visibility = {
        usesMode(ShaderMode.GRADIENT)
    })
    val alpha2 by setting("Gradient Alpha", 170, 0..255, 1, visibility = {
        usesMode(ShaderMode.GRADIENT)
    })
    val lineWidth by setting("Line Width", 2, 0..500, 1)
    val quality by setting("Quality", 3, 0..6, 1)
    val octaves by setting("Smoke Octaves", 10, 5..30, 1)
    val fillAlpha by setting("Fill Alpha", 170, 0..255, 1)
    val smokeGlow by setting("Smoke Glow", true)

    val outlineColor by setting("Outline", ColorRGBA(255, 255, 255, 136))
    val smokeOutlineColor1 by setting("Smoke Outline", ColorRGBA(255, 0, 0, 136), visibility = {
        usesMode(ShaderMode.SMOKE)
    })
    val smokeOutlineColor2 by setting("Smoke Outline 2", ColorRGBA(255, 0, 0, 136), visibility = {
        usesMode(ShaderMode.SMOKE)
    })
    val fillColor1 by setting("Fill", ColorRGBA(255, 255, 255, 136))
    val fillColor2 by setting("Smoke Fill", ColorRGBA(255, 255, 255, 136))
    val fillColor3 by setting("Smoke Fill 2", ColorRGBA(255, 255, 255, 136))

    init {
        onDisabled { ShaderHolder.discardCaptures() }
    }

    override fun getDisplayInfo(): Any = mode.displayName

    @JvmStatic
    fun shouldRenderHands(): Boolean = hands

    @JvmStatic
    fun shouldRenderChest(blockPos: BlockPos): Boolean {
        val player = dev.luna5ama.trollhack.utils.MinecraftWrapper.mc.player ?: return false
        return chests && player.blockPosition().distSqr(blockPos) <= maxRange.toDouble() * maxRange
    }

    @JvmStatic
    fun shouldRender(entity: Entity): Boolean {
        val player = dev.luna5ama.trollhack.utils.MinecraftWrapper.mc.player ?: return false
        if (player.distanceToSqr(entity.position()) > maxRange.toDouble() * maxRange) return false

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

    @JvmStatic
    fun outlineArgb(entity: Entity): Int {
        if (!isEnabled || !shouldRender(entity)) return 0
        return outlineColor.toArgb()
    }

    @JvmStatic
    fun outlineArgb(): Int = outlineColor.toArgb()

    private fun usesMode(shaderMode: ShaderMode): Boolean {
        return mode == shaderMode || handsMode == shaderMode || chestMode == shaderMode
    }

    private fun ColorRGBA.toArgb(): Int {
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    enum class ShaderMode(override val displayName: CharSequence) : Displayable {
        DEFAULT("Default"),
        SMOKE("Smoke"),
        GRADIENT("Gradient"),
        SNOW("Snow"),
        FADE("Fade")
    }
}
