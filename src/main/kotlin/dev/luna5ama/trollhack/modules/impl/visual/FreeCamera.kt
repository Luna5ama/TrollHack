package dev.luna5ama.trollhack.modules.impl.visual

import com.mojang.blaze3d.platform.InputConstants
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.event.impl.player.PlayerMoveEvent
import dev.luna5ama.trollhack.event.impl.player.InputUpdateEvent
import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import dev.luna5ama.trollhack.mixins.accessor.ICameraAccessor
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import net.minecraft.client.Camera
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket
import net.minecraft.network.protocol.game.ClientboundRespawnPacket
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.minecraft.world.entity.player.Input

object FreeCamera : Module("Free Camera", category = Category.RENDER) {
    private val speed by setting("Speed", 1.0f, 0.0f..10.0f, 0.1f)
    private val speedScrollSensitivity by setting("Speed Scroll Sensitivity", 0.0f, 0.0f..2.0f, 0.1f)
    private val staySneaking by setting("Stay Sneaking", true)
    private val showHands by setting("Show Hands", true)
    private val toggleOnDamage by setting("Toggle On Damage", false)
    private val toggleOnDeath by setting("Toggle On Death", false)
    private val toggleOnLog by setting("Toggle On Log", true)
    private val staticView by setting("Static", true)

    private var pos = Vec3.ZERO
    private var prevPos = Vec3.ZERO
    private var yaw = 0f
    private var pitch = 0f
    private var oldFovScale = 1.0
    private var oldBob = true
    private var staticViewApplied = false
    private var speedValue = 1.0f
    private var wasSneaking = false

    init {
        onEnabled {
            val player = mc.player ?: return@onEnabled
            pos = mc.gameRenderer.mainCamera.position()
            prevPos = pos
            yaw = player.yRot
            pitch = player.xRot
            oldFovScale = mc.options.fovEffectScale().get()
            oldBob = mc.options.bobView().get()
            speedValue = speed
            wasSneaking = keyDown(mc.options.keyShift)
            staticViewApplied = staticView
            if (staticViewApplied) {
                mc.options.fovEffectScale().set(0.0)
                mc.options.bobView().set(false)
            }
            mc.levelRenderer.allChanged()
        }
        onDisabled {
            if (staticViewApplied) {
                mc.options.fovEffectScale().set(oldFovScale)
                mc.options.bobView().set(oldBob)
                staticViewApplied = false
            }
            wasSneaking = false
            mc.levelRenderer.allChanged()
        }
        nonNullHandler<TickEvent.Post> {
            val forward = Vec3.directionFromRotation(0f, yaw)
            val right = Vec3.directionFromRotation(0f, yaw + 90f)
            var velocity = Vec3.ZERO
            if (keyDown(mc.options.keyUp)) velocity = velocity.add(forward)
            if (keyDown(mc.options.keyDown)) velocity = velocity.subtract(forward)
            if (keyDown(mc.options.keyRight)) velocity = velocity.add(right)
            if (keyDown(mc.options.keyLeft)) velocity = velocity.subtract(right)
            if (keyDown(mc.options.keyJump)) velocity = velocity.add(0.0, 1.0, 0.0)
            if (keyDown(mc.options.keyShift)) velocity = velocity.add(0.0, -1.0, 0.0)
            val multiplier = if (keyDown(mc.options.keySprint)) speedValue.toDouble() else speedValue * 0.5
            prevPos = pos
            if (velocity.lengthSqr() > 0.0) pos = pos.add(velocity.normalize().scale(multiplier))
        }
        nonNullHandler<InputUpdateEvent> {
            it.movementInput.keyPresses = Input(
                false, false, false, false, false,
                staySneaking && wasSneaking, false
            )
        }
        nonNullHandler<PlayerMoveEvent.Pre> {
            it.cancel()
            player.deltaMovement = Vec3.ZERO
        }
        nonNullHandler<PacketEvent.Receive> {
            when (val packet = it.packet) {
                is ClientboundRespawnPacket -> disable()
                is ClientboundPlayerCombatKillPacket -> if (toggleOnDeath && packet.playerId() == player.id) disable()
                is ClientboundSetHealthPacket -> if (toggleOnDamage && packet.health < player.health) disable()
            }
        }
        handler<WorldEvent.Unload> {
            if (toggleOnLog) disable()
        }
    }

    private fun keyDown(mapping: net.minecraft.client.KeyMapping): Boolean = InputConstants.isKeyDown(
        mc.window,
        InputConstants.getKey(mapping.saveString()).value
    )

    @JvmStatic
    fun changeLookDirection(deltaX: Double, deltaY: Double) {
        if (!isEnabled) return
        yaw += (deltaX * 0.15).toFloat()
        pitch = Mth.clamp(pitch + (deltaY * 0.15).toFloat(), -90f, 90f)
    }

    @JvmStatic
    fun adjustSpeed(scroll: Double): Boolean {
        if (!isEnabled || mc.screen != null || speedScrollSensitivity <= 0.0f) return false
        speedValue = (speedValue + scroll.toFloat() * 0.25f * (speedScrollSensitivity * speedValue))
            .coerceIn(0.1f, 10.0f)
        return true
    }

    @JvmStatic
    fun shouldRenderHands(vanilla: Boolean): Boolean = if (isEnabled) showHands else vanilla

    @JvmStatic
    fun applyCamera(camera: Camera, partialTick: Float) {
        if (!isEnabled) return
        val accessor = camera as ICameraAccessor
        accessor.`trollhack$setPosition`(Vec3(
            Mth.lerp(partialTick.toDouble(), prevPos.x, pos.x),
            Mth.lerp(partialTick.toDouble(), prevPos.y, pos.y),
            Mth.lerp(partialTick.toDouble(), prevPos.z, pos.z)
        ))
        accessor.`trollhack$setRotation`(yaw, pitch)
    }
}
