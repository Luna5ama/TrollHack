package dev.luna5ama.trollhack.module.modules.player

import dev.fastmc.common.floorToInt
import dev.fastmc.common.toRadians
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.InputEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.player.InputUpdateEvent
import dev.luna5ama.trollhack.event.events.player.PlayerAttackEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.BaritoneUtils
import dev.luna5ama.trollhack.util.MovementUtils
import dev.luna5ama.trollhack.util.MovementUtils.calcMoveYaw
import dev.luna5ama.trollhack.util.MovementUtils.resetJumpSneak
import dev.luna5ama.trollhack.util.MovementUtils.resetMove
import dev.luna5ama.trollhack.util.accessor.unpressKey
import dev.luna5ama.trollhack.util.atValue
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.math.RotationUtils
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.threads.onMainThreadSafe
import dev.luna5ama.trollhack.util.threads.runSafe
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.MoverType
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.util.MovementInput
import net.minecraft.util.MovementInputFromOptions
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import org.lwjgl.input.Keyboard
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal object Freecam : Module(
    name = "Freecam",
    category = Category.PLAYER,
    description = "Leave your body and transcend into the realm of the gods"
) {
    private val directionMode0 = setting("Flight Mode", FlightMode.CREATIVE)
    private val directionMode by directionMode0
    private val horizontalSpeed by setting("Horizontal Speed", 20.0f, 1.0f..50.0f, 1f)
    private val verticalSpeed by setting(
        "Vertical Speed",
        20.0f,
        1.0f..50.0f,
        1f,
        directionMode0.atValue(FlightMode.CREATIVE)
    )
    private val autoRotate by setting("Auto Rotate", true)
    private val arrowKeyMove by setting("Arrow Key Move", true)
    private val relative by setting("Relative", false)

    private enum class FlightMode(override val displayName: CharSequence) : DisplayEnum {
        CREATIVE("Creative"),
        THREE_DEE("3D")
    }

    var cameraGuy: EntityPlayer? = null; private set

    private const val ENTITY_ID = -6969420

    @JvmStatic
    fun handleTurn(entity: Entity, yaw: Float, pitch: Float, ci: CallbackInfo): Boolean {
        if (isDisabled) return false
        val player = mc.player ?: return false
        val cameraGuy = cameraGuy ?: return false

        return if (entity == player) {
            cameraGuy.turn(yaw, pitch)
            ci.cancel()
            true
        } else {
            false
        }
    }

    @JvmStatic
    fun getRenderChunkOffset(playerPos: BlockPos) =
        runSafe {
            BlockPos(
                (player.posX / 16).floorToInt() * 16,
                (player.posY / 16).floorToInt() * 16,
                (player.posZ / 16).floorToInt() * 16
            )
        } ?: playerPos

    @JvmStatic
    fun getRenderViewEntity(renderViewEntity: EntityPlayer): EntityPlayer {
        val player = mc.player
        return if (isEnabled && player != null) {
            player
        } else {
            renderViewEntity
        }
    }

    init {
        onEnable {
            mc.renderChunksMany = false
        }

        onDisable {
            mc.renderChunksMany = true
            resetCameraGuy()
            resetMovementInput(mc.player?.movementInput)
        }

        listener<ConnectionEvent.Disconnect> {
            disable()
        }

        safeListener<PacketEvent.Send> {
            if (it.packet !is CPacketUseEntity) return@safeListener
            // Don't interact with self
            if (it.packet.getEntityFromWorld(world) == player) it.cancel()
        }

        listener<PlayerAttackEvent> {
            if (it.entity == mc.player) it.cancel()
        }

        safeListener<InputEvent.Keyboard> {
            // Force it to stay in first person lol
            if (mc.gameSettings.keyBindTogglePerspective.isKeyDown) {
                mc.gameSettings.thirdPersonView = 0
                mc.gameSettings.keyBindTogglePerspective.unpressKey()
            }
        }

        safeListener<TickEvent.Post> {
            if (!player.isEntityAlive) {
                if (cameraGuy != null) resetCameraGuy()
                return@safeListener
            }

            if (cameraGuy == null && player.ticksExisted > 5) spawnCameraGuy()
        }

        safeListener<InputUpdateEvent>(9999) {
            if (it.movementInput !is MovementInputFromOptions || BaritoneUtils.isPathing) return@safeListener

            resetMovementInput(it.movementInput)

            if (BaritoneUtils.isActive) return@safeListener

            if (autoRotate) updatePlayerRotation()
            if (arrowKeyMove) updatePlayerMovement()
        }
    }

    private fun resetMovementInput(movementInput: MovementInput?) {
        if (movementInput is MovementInputFromOptions) {
            movementInput.resetMove()
            movementInput.resetJumpSneak()
        }
    }

    private fun SafeClientEvent.spawnCameraGuy() {
        // Create a cloned player
        cameraGuy = FakeCamera(world, player).also {
            // Add it to the world
            world.addEntityToWorld(ENTITY_ID, it)

            // Set the render view entity to our camera guy
            mc.renderViewEntity = it

            // Reset player movement input
            resetMovementInput(player.movementInput)

            // Stores prev third person view setting
            mc.gameSettings.thirdPersonView = 0
        }
    }

    private fun SafeClientEvent.updatePlayerRotation() {
        mc.objectMouseOver?.let {
            val hitVec = it.hitVec
            if (it.typeOfHit == RayTraceResult.Type.MISS || hitVec == null) return
            val rotation = getRotationTo(hitVec)
            player.apply {
                rotationYaw = rotation.x
                rotationPitch = rotation.y
            }
        }
    }

    private fun SafeClientEvent.updatePlayerMovement() {
        val cameraGuy = cameraGuy ?: return
        val movementInput = MovementUtils.calcMovementInput(
            Keyboard.isKeyDown(Keyboard.KEY_UP),
            Keyboard.isKeyDown(Keyboard.KEY_DOWN),
            Keyboard.isKeyDown(Keyboard.KEY_LEFT),
            Keyboard.isKeyDown(Keyboard.KEY_RIGHT),
            up = false,
            down = false
        )

        val yawDiff = RotationUtils.normalizeAngle(player.rotationYaw - cameraGuy.rotationYaw)
        val yawRad = calcMoveYaw(yawDiff, movementInput.z, movementInput.x).toFloat()
        val inputTotal = if (movementInput.x == 0.0f && movementInput.z == 0.0f) 0.0f else 1.0f

        player.movementInput?.apply {
            moveForward = cos(yawRad) * inputTotal
            moveStrafe = -sin(yawRad) * inputTotal

            forwardKeyDown = moveForward > 0.0f
            backKeyDown = moveForward < 0.0f
            leftKeyDown = moveStrafe < 0.0f
            rightKeyDown = moveStrafe > 0.0f

            jump = Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
        }
    }

    private fun resetCameraGuy() {
        cameraGuy = null
        onMainThreadSafe {
            world.removeEntityFromWorld(ENTITY_ID)
            mc.renderViewEntity = player
        }

    }

    private class FakeCamera(world: WorldClient, val player: EntityPlayerSP) :
        EntityOtherPlayerMP(world, mc.session.profile) {
        init {
            copyLocationAndAnglesFrom(player)
            capabilities.allowFlying = true
            capabilities.isFlying = true
        }

        override fun onLivingUpdate() {
            // Update inventory
            inventory.copyInventory(player.inventory)

            // Update yaw head
            updateEntityActionState()

            // We have to update movement input from key binds because mc.player.movementInput is used by Baritone
            val movementInput = MovementUtils.calcMovementInput(
                mc.gameSettings.keyBindForward.isKeyDown,
                mc.gameSettings.keyBindBack.isKeyDown,
                mc.gameSettings.keyBindLeft.isKeyDown,
                mc.gameSettings.keyBindRight.isKeyDown,
                mc.gameSettings.keyBindJump.isKeyDown,
                mc.gameSettings.keyBindSneak.isKeyDown,
            )

            moveForward = movementInput.z
            moveStrafing = movementInput.x
            moveVertical = movementInput.y

            // Update sprinting
            isSprinting = mc.gameSettings.keyBindSprint.isKeyDown

            val yawRad = calcMoveYaw(rotationYaw, moveForward, moveStrafing)
            val speed = (horizontalSpeed / 20.0f) * min(abs(moveForward) + abs(moveStrafing), 1.0f)

            if (directionMode == FlightMode.THREE_DEE) {
                val pitchRad = rotationPitch.toDouble().toRadians() * moveForward
                motionX = -sin(yawRad) * cos(pitchRad) * speed
                motionY = -sin(pitchRad) * speed
                motionZ = cos(yawRad) * cos(pitchRad) * speed
            } else {
                motionX = -sin(yawRad) * speed
                motionY = moveVertical.toDouble() * (verticalSpeed / 20.0f)
                motionZ = cos(yawRad) * speed
            }

            if (isSprinting) {
                motionX *= 1.5
                motionY *= 1.5
                motionZ *= 1.5
            }

            if (relative) {
                motionX += player.posX - player.prevPosX
                motionY += player.posY - player.prevPosY
                motionZ += player.posZ - player.prevPosZ
            }

            move(MoverType.SELF, motionX, motionY, motionZ)
        }

        override fun getEyeHeight() = 1.65f

        override fun isSpectator() = true

        override fun isInvisible() = true

        override fun isInvisibleToPlayer(player: EntityPlayer) = true
    }
}