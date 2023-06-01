package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.and
import dev.luna5ama.trollhack.util.atFalse
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.atValue
import dev.luna5ama.trollhack.util.extension.sumOfFloat
import dev.luna5ama.trollhack.util.math.RotationUtils.setPitch
import dev.luna5ama.trollhack.util.math.RotationUtils.setYaw
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

internal object ViewLock : Module(
    name = "View Lock",
    alias = arrayOf("YawLock", "PitchLock"),
    category = Category.PLAYER,
    description = "Locks your camera view"
) {
    private val page = setting("Page", Page.YAW)

    private val yaw0 = setting("Yaw", true, page.atValue(Page.YAW))
    private val yaw by yaw0
    private val autoYaw = setting("Auto Yaw", true, page.atValue(Page.YAW) and yaw0.atTrue())
    private val disableMouseYaw by setting("Disable Mouse Yaw", false, page.atValue(Page.YAW) and yaw0.atTrue())
    private val specificYaw by setting(
        "Specific Yaw",
        180.0f,
        -180.0f..180.0f,
        1.0f,
        page.atValue(Page.YAW) and yaw0.atTrue() and autoYaw.atFalse()
    )
    private val yawSlice =
        setting("Yaw Slice", 8, 2..32, 1, page.atValue(Page.YAW) and yaw0.atTrue() and autoYaw.atTrue())

    private val pitch0 = setting("Pitch", true, page.atValue(Page.PITCH))
    private val pitch by pitch0
    private val autoPitch = setting("Auto Pitch", true, page.atValue(Page.PITCH) and pitch0.atTrue())
    private val disableMousePitch by setting("Disable Mouse Pitch", false, page.atValue(Page.PITCH) and pitch0.atTrue())
    private val specificPitch by setting(
        "Specific Pitch",
        0.0f,
        -90.0f..90.0f,
        1.0f,
        page.atValue(Page.PITCH) and pitch0.atTrue() and autoPitch.atFalse()
    )
    private val pitchSlice =
        setting("Pitch Slice", 5, 2..32, 1, page.atValue(Page.PITCH) and pitch0.atTrue() and autoPitch.atTrue())

    private enum class Page {
        YAW, PITCH
    }

    private var yawSnap = 0
    private var pitchSnap = 0
    private val deltaXQueue = ArrayDeque<Pair<Float, Long>>()
    private val deltaYQueue = ArrayDeque<Pair<Float, Long>>()
    private var pitchSliceAngle = 1.0f
    private var yawSliceAngle = 1.0f

    init {
        onEnable {
            yawSliceAngle = 360.0f / yawSlice.value
            pitchSliceAngle = 180.0f / (pitchSlice.value - 1)
            if (autoYaw.value || autoPitch.value) snapToNext()
        }

        safeListener<TickEvent.Post> {
            if (autoYaw.value || autoPitch.value) {
                snapToSlice()
            }

            if (yaw && !autoYaw.value) {
                player.setYaw(specificYaw)
            }

            if (pitch && !autoPitch.value) {
                player.setPitch(specificPitch)
            }
        }
    }

    @JvmStatic
    fun handleTurn(entity: Entity, deltaX: Float, deltaY: Float, ci: CallbackInfo) {
        if (isDisabled) return
        val player = mc.player ?: return
        if (entity != player) return

        val multipliedX = deltaX * 0.15f
        val multipliedY = deltaY * -0.15f

        val yawChange = if (yaw && autoYaw.value) handleDelta(multipliedX, deltaXQueue, yawSliceAngle) else 0
        val pitchChange = if (pitch && autoPitch.value) handleDelta(multipliedY, deltaYQueue, pitchSliceAngle) else 0

        turn(player, multipliedX, multipliedY)
        changeDirection(yawChange, pitchChange)
        player.ridingEntity?.applyOrientationToEntity(player)

        ci.cancel()
    }

    private fun turn(player: EntityPlayerSP, deltaX: Float, deltaY: Float) {
        if (!yaw || !disableMouseYaw) {
            player.prevRotationYaw += deltaX
            player.rotationYaw += deltaX
        }

        if (!pitch || !disableMousePitch) {
            player.prevRotationPitch += deltaY
            player.rotationPitch += deltaY
            player.rotationPitch = player.rotationPitch.coerceIn(-90.0f, 90.0f)
        }
    }

    private fun handleDelta(delta: Float, list: ArrayDeque<Pair<Float, Long>>, slice: Float): Int {
        val currentTime = System.currentTimeMillis()
        list.add(Pair(delta, currentTime))

        val sum = list.sumOfFloat { it.first }
        return if (abs(sum) > slice) {
            list.clear()
            sign(sum).toInt()
        } else {
            while (list.first().second < currentTime - 500) {
                list.removeFirstOrNull()
            }
            0
        }
    }

    private fun changeDirection(yawChange: Int, pitchChange: Int) {
        yawSnap = Math.floorMod(yawSnap + yawChange, yawSlice.value)
        pitchSnap = (pitchSnap + pitchChange).coerceIn(0, pitchSlice.value - 1)
        snapToSlice()
    }

    private fun snapToNext() {
        mc.player?.let {
            yawSnap = (it.rotationYaw / yawSliceAngle).roundToInt()
            pitchSnap = ((it.rotationPitch + 90.0f) / pitchSliceAngle).roundToInt()
            snapToSlice()
        }
    }

    private fun snapToSlice() {
        mc.player?.let { player ->
            if (yaw && autoYaw.value) {
                player.setYaw(yawSnap * yawSliceAngle)
                player.ridingEntity?.let { it.rotationYaw = player.rotationYaw }
            }
            if (pitch && autoPitch.value) {
                player.setPitch(pitchSnap * pitchSliceAngle - 90.0f)
            }
        }
    }

    init {
        yawSlice.listeners.add {
            yawSliceAngle = 360.0f / yawSlice.value
            if (isEnabled && autoYaw.value) snapToNext()
        }

        pitchSlice.listeners.add {
            pitchSliceAngle = 180.0f / (pitchSlice.value - 1)
            if (isEnabled && autoPitch.value) snapToNext()
        }

        with({ _: Boolean, it: Boolean -> if (isEnabled && it) snapToNext() }) {
            autoPitch.valueListeners.add(this)
            autoYaw.valueListeners.add(this)
        }
    }
}