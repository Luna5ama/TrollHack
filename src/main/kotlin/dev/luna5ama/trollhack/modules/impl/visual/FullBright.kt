package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects

object FullBright : Module("Fullbright", "Maxes out the brightness.", category = Category.RENDER) {
    private var potionSnapshot: MobEffectInstance? = null
    private var potionPlayer: LocalPlayer? = null
    private var potionWorld: ClientLevel? = null

    private val modeSetting = setting("Mode", Mode.GAMMA).apply {
        register { previousMode, newMode ->
            if (isEnabled && previousMode == Mode.POTION && newMode == Mode.GAMMA) {
                clearPotionEffect()
            }
            true
        }
    }
    private val mode by modeSetting

    init {
        onDisabled {
            if (isEnabled && mode == Mode.POTION) clearPotionEffect()
        }

        nonNullHandler<TickEvent.Pre> {
            if (mode == Mode.POTION) {
                if (potionPlayer !== player || potionWorld !== world) {
                    clearPotionEffect(potionPlayer, potionWorld)
                }
                if (potionPlayer == null) {
                    potionPlayer = player
                    potionWorld = world
                    potionSnapshot = player.getEffect(MobEffects.NIGHT_VISION)?.let { MobEffectInstance(it) }
                }
                player.addEffect(MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0))
            }
        }
    }

    private fun clearPotionEffect(player: LocalPlayer? = mc.player, world: ClientLevel? = mc.level) {
        val owner = potionPlayer
        if (owner != null && owner === player && potionWorld === world) {
            owner.removeEffect(MobEffects.NIGHT_VISION)
            potionSnapshot?.let { owner.addEffect(MobEffectInstance(it)) }
        }

        potionSnapshot = null
        potionPlayer = null
        potionWorld = null
    }

    @JvmStatic
    fun isGammaMode(): Boolean = isEnabled && mode == Mode.GAMMA

    override fun getDisplayInfo(): Any = mode.displayName

    private enum class Mode(override val displayName: CharSequence) : Displayable {
        GAMMA("Gamma"),
        POTION("Potion")
    }
}
