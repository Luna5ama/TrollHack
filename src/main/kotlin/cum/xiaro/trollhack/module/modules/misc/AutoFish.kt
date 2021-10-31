package cum.xiaro.trollhack.module.modules.misc

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.TimeUnit
import cum.xiaro.trollhack.util.accessor.rightClickMouse
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.world.isWater
import net.minecraft.init.Items
import net.minecraft.network.play.server.SPacketSoundEffect
import java.lang.Math.random
import kotlin.math.abs

internal object AutoFish : Module(
    name = "AutoFish",
    category = Category.MISC,
    description = "Automatically catch fish"
) {
    private val mode = setting("Mode", Mode.BOUNCE)
    private val autoCast = setting("Auto Cast", true)
    private val castDelay = setting("Auto Cast Delay", 5, 1..20, 1, autoCast.atTrue(), description = "Delay before starting fishing when holding a fishing rod, in seconds")
    private val catchDelay = setting("Catch Delay", 300, 50..2000, 50, description = "Delay before catching the fish, in milliseconds")
    private val recastDelay = setting("Recast Delay", 450, 50..2000, 50, description = "Delay before recasting the fishing rod, in milliseconds")
    private val variation = setting("Variation", 100, 0..1000, 50, description = "Randomize the delays in specific range, in milliseconds")

    @Suppress("UNUSED")
    private enum class Mode {
        BOUNCE, SPLASH, ANY_SPLASH, ALL
    }

    private var catching = false
    private var recasting = false
    private val timer = TickTimer()

    init {
        safeListener<PacketEvent.Receive> {
            if (player.fishEntity == null || !isStabled()) return@safeListener
            if (mode.value == Mode.BOUNCE || it.packet !is SPacketSoundEffect) return@safeListener
            if (isSplash(it.packet)) catch()
        }

        safeListener<TickEvent.Post> {
            if (player.heldItemMainhand.item != Items.FISHING_ROD) { // If not holding a fishing rod then don't do anything
                reset()
                return@safeListener
            }

            if (player.fishEntity == null) {
                if (recasting) { // Recast the fishing rod
                    if (timer.tickAndReset(recastDelay.value)) {
                        mc.rightClickMouse()
                        reset()
                    }
                } else if (autoCast.value && timer.tickAndReset(castDelay.value, TimeUnit.SECONDS)) { // Cast the fishing rod if a fishing rod is in hand and not fishing
                    mc.rightClickMouse()
                    reset()
                }
            } else if (isStabled() && isOnWater()) {
                if (catching) { // Catch the fish
                    if (timer.tickAndReset(catchDelay.value)) {
                        mc.rightClickMouse()
                        recast()
                    }
                } else {// Bounce detection
                    if ((mode.value == Mode.BOUNCE || mode.value == Mode.ALL) && isBouncing()) {
                        catch()
                    }
                }
            } else if (isStabled()) {// If the fishing rod is not in air and not in water (ex. hooked a block), then we recast it with extra delay
                mc.rightClickMouse()
                reset()
            }
        }

        onToggle {
            reset()
        }
    }

    private fun SafeClientEvent.isStabled(): Boolean {
        if (player.fishEntity?.isAirBorne != false || recasting) return false
        return abs(player.fishEntity!!.motionX) + abs(player.fishEntity!!.motionZ) < 0.01
    }

    private fun SafeClientEvent.isOnWater(): Boolean {
        if (player.fishEntity?.isAirBorne != false) return false
        val pos = player.fishEntity!!.position
        return world.isWater(pos) || world.isWater(pos.down())
    }

    private fun SafeClientEvent.isSplash(packet: SPacketSoundEffect): Boolean {
        if (mode.value == Mode.SPLASH && (player.fishEntity?.getDistance(packet.x, packet.y, packet.z)
                ?: 69420.0) > 2) return false
        val soundName = packet.sound.soundName.toString().lowercase()
        return (mode.value != Mode.SPLASH && isAnySplash(soundName)) || soundName.contains("entity.bobber.splash")
    }

    private fun isAnySplash(soundName: String): Boolean {
        return soundName.contains("entity.generic.splash")
            || soundName.contains("entity.hostile.splash")
            || soundName.contains("entity.player.splash")
    }

    private fun SafeClientEvent.isBouncing(): Boolean {
        if (player.fishEntity == null || !isOnWater()) return false
        return (player.fishEntity?.motionY ?: 911.0) !in -0.05..0.05
    }

    private fun catch() {
        if (catching) return
        resetTimer()
        catching = true
        recasting = false
    }

    private fun recast(extraDelay: Long = 0L) {
        if (recasting) return
        resetTimer()
        timer.reset(extraDelay)
        catching = false
        recasting = true
    }

    private fun reset() {
        resetTimer()
        catching = false
        recasting = false
    }

    private fun resetTimer() {
        val offset = if (variation.value > 0) (random() * (variation.value * 2) - variation.value).toLong() else 0
        timer.reset(offset)
    }
}