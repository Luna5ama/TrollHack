package dev.luna5ama.trollhack.modules.impl.movement

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.event.impl.player.PlayerTravelEvent
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.manager.managers.TimerManager.modifyTimer
import dev.luna5ama.trollhack.mixins.accessor.IPositionMoveRotationAccessor
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.ChatUtils
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.compat.forwardImpulseCompat
import dev.luna5ama.trollhack.utils.combat.MovementUtils.calcMoveYaw
import dev.luna5ama.trollhack.utils.extension.pitch
import dev.luna5ama.trollhack.utils.extension.velocityX
import dev.luna5ama.trollhack.utils.extension.velocityY
import dev.luna5ama.trollhack.utils.extension.velocityZ
import dev.luna5ama.trollhack.utils.math.toDegree
import dev.luna5ama.trollhack.utils.math.toRadian
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.runSafe
import dev.luna5ama.trollhack.utils.world.getGroundPos
import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Items
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object ElytraFlight : Module(
    name = "Elytra Flight",
    description = "Allows infinite and way easier Elytra flying",
    category = Category.MOVEMENT,
    modulePriority = 1000
) {
    private val mode0 = setting("Mode", ElytraFlightMode.CONTROL)
    private val mode by mode0
    private val page by setting("Page", Page.GENERIC_SETTINGS)
    private val durabilityWarning0 = setting("Durability Warning", true, { page == Page.GENERIC_SETTINGS })
    private val durabilityWarning by durabilityWarning0
    private val threshold by setting("Warning Threshold", 5, 1..50, 1, { page == Page.GENERIC_SETTINGS && durabilityWarning }, description = "Threshold of durability to start sending warnings")
    private var autoLanding by setting("Auto Landing", false, { page == Page.GENERIC_SETTINGS })

    /* Generic Settings */
    /* Takeoff */
    private val easyTakeOff0 = setting("Easy Takeoff", true, { page == Page.GENERIC_SETTINGS })
    private val easyTakeOff by easyTakeOff0
    private val timerControl by setting("Takeoff Timer", true, { page == Page.GENERIC_SETTINGS })
    private val highPingOptimize0 = setting("High Ping Optimize", false, { page == (Page.GENERIC_SETTINGS) && easyTakeOff })
    private val highPingOptimize by highPingOptimize0
    private val minTakeoffHeight by setting("Min Takeoff Height", 0.5f, 0.0f..1.5f, 0.1f, { page == Page.GENERIC_SETTINGS && easyTakeOff and !highPingOptimize })

    /* Acceleration */
    private val accelerateStartSpeed by setting("Start Speed", 100, 0..100, 5, { page == Page.GENERIC_SETTINGS && mode != ElytraFlightMode.BOOST })
    private val accelerateTime by setting("Accelerate Time", 0.0f, 0.0f..20.0f, 0.25f, { page == Page.GENERIC_SETTINGS && mode != ElytraFlightMode.BOOST }
    )

    /* Spoof Pitch */
    private val spoofPitch0 = setting("Spoof Pitch", true, { page == Page.GENERIC_SETTINGS && mode != ElytraFlightMode.BOOST })
    private val spoofPitch by spoofPitch0
    private val blockInteract by setting("Block Interact", false, { page == (Page.GENERIC_SETTINGS) && mode != (ElytraFlightMode.BOOST) && spoofPitch })
    private val forwardPitch by setting("Forward Pitch", 0, -90..90, 5, { page == (Page.GENERIC_SETTINGS) && mode != (ElytraFlightMode.BOOST) && spoofPitch })

    /* Extra */
    val elytraSounds by setting("Elytra Sounds", true, { page == Page.GENERIC_SETTINGS })
    private val swingSpeed by setting("Swing Speed", 1.0f, 0.0f..2.0f, 0.1f, { page == Page.GENERIC_SETTINGS && (mode == ElytraFlightMode.CONTROL || mode ==  ElytraFlightMode.PACKET) })
    private val swingAmount by setting("Swing Amount", 0.8f, 0.0f..2.0f, 0.1f, { page == Page.GENERIC_SETTINGS && (mode == ElytraFlightMode.CONTROL || mode ==  ElytraFlightMode.PACKET) })
    /* End of Generic Settings */

    /* Mode Settings */
    /* Boost */
    private val speedBoost by setting("Speed B", 1.0f, 0.0f..10.0f, 0.1f, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.BOOST })
    private val upSpeedBoost by setting("Up Speed B", 1.0f, 1.0f..5.0f, 0.1f, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.BOOST })
    private val downSpeedBoost by setting("Down Speed B", 1.0f, 1.0f..5.0f, 0.1f, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.BOOST })

    /* Control */
    private val boostPitchControl by setting("Base Boost Pitch", 20, 0..90, 5, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.CONTROL })
    private val ncpStrict by setting("NCP Strict", true, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.CONTROL })
    private val legacyLookBoost by setting("Legacy Look Boost", false, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.CONTROL })
    private val altitudeHoldControl by setting("Auto Control Altitude", false, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.CONTROL })
    private val dynamicDownSpeed0 = setting("Dynamic Down Speed", false, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.CONTROL })
    private val dynamicDownSpeed by dynamicDownSpeed0
    private val speedControl by setting("Speed C", 1.81f, 0.0f..10.0f, 0.1f, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.CONTROL })
    private val fallSpeedControl by setting("Fall Speed C", 0.00000000000003f, 0.0f..0.3f, 0.01f, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.CONTROL })
    private val downSpeedControl by setting("Down Speed C", 1.0f, 1.0f..5.0f, 0.1f, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.CONTROL })
    private val fastDownSpeedControl by setting("Dynamic Down Speed C", 2.0f, 1.0f..5.0f, 0.1f, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.CONTROL && dynamicDownSpeed })

    /* Creative */
    private val speedCreative by setting("Speed CR", 1.8f, 0.0f..10.0f, 0.1f, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.CREATIVE })
    private val fallSpeedCreative by setting("Fall Speed CR", 0.00001f, 0.0f..0.3f, 0.01f, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.CREATIVE })
    private val upSpeedCreative by setting("Up Speed CR", 1.0f, 1.0f..5.0f, 0.1f, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.CREATIVE })
    private val downSpeedCreative by setting("Down Speed CR", 1.0f, 1.0f..5.0f, 0.1f, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.CREATIVE })

    /* Packet */
    private val speedPacket by setting("Speed P", 1.8f, 0.0f..20.0f, 0.1f, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.PACKET })
    private val fallSpeedPacket by setting("Fall Speed P", 0.00001f, 0.0f..0.3f, 0.01f, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.PACKET })
    private val downSpeedPacket by setting("Down Speed P", 1.0f, 0.1f..5.0f, 0.1f, { page == Page.MODE_SETTINGS && mode == ElytraFlightMode.PACKET })
    /* End of Mode Settings */

    private enum class ElytraFlightMode(override val displayName: CharSequence) : Displayable {
        BOOST("Boost"),
        CONTROL("Control"),
        CREATIVE("Creative"),
        PACKET("Packet")
    }

    private enum class Page : Displayable {
        GENERIC_SETTINGS, MODE_SETTINGS
    }

    /* Generic states */
    private var elytraIsEquipped = false
    private var elytraDurability = 0
    private var outOfDurability = false
    private var wasInLiquid = false
    private var isFlying = false
    private var isPacketFlying = false
    private var isStandingStillH = false
    private var isStandingStill = false
    private var speedPercentage = 0.0f

    /* Control mode states */
    private var hoverTarget = -1.0
    private var packetYaw = 0.0f
    private var packetPitch = 0.0f
    private var hoverState = false
    private var boostingTick = 0

    override fun getDisplayInfo(): Any? {
        return mode.displayString
    }

    /* Event Listeners */
    init {
        nonNullHandler<PacketEvent.Receive> {
            if (player.isSpectator || !elytraIsEquipped || elytraDurability <= 1 || !isFlying || mode == ElytraFlightMode.BOOST) return@nonNullHandler
            if (it.packet is ClientboundPlayerPositionPacket && mode != ElytraFlightMode.PACKET) {
                val packet = it.packet
                (packet.change as IPositionMoveRotationAccessor).setXRot(player.xRot)
            }

            /* Cancels the elytra opening animation */
            if (it.packet is ClientboundAnimatePacket && isPacketFlying) {
                val packet = it.packet
                if (packet.id == player.id) it.cancel()
            }
        }

        nonNullHandler<PlayerTravelEvent.Pre> {
            if (player.isSpectator) return@nonNullHandler
            stateUpdate(it)
            if (elytraIsEquipped && elytraDurability > 1) {
                if (autoLanding) {
                    landing(it)
                    return@nonNullHandler
                }
                if (!isFlying && !isPacketFlying) {
                    takeoff(it)
                } else {
                    modifyTimer(50f)
                    player.isSprinting = false
                    when (mode) {
                        ElytraFlightMode.BOOST -> boostMode()
                        ElytraFlightMode.CONTROL -> controlMode(it)
                        ElytraFlightMode.CREATIVE -> creativeMode()
                        ElytraFlightMode.PACKET -> packetMode(it)
                    }
                }
                spoofRotation()
            } else if (!outOfDurability) {
                reset(true)
            }
        }
    }
    /* End of Event Listeners */

    /* Generic Functions */
    private fun NonNullContext.stateUpdate(event: PlayerTravelEvent.Pre) {
        /* Elytra Check */
        val armorSlot = player.getItemBySlot(EquipmentSlot.CHEST)
        elytraIsEquipped = armorSlot.item == Items.ELYTRA

        /* Elytra Durability Check */
        if (elytraIsEquipped) {
            val oldDurability = elytraDurability
            elytraDurability = armorSlot.maxDamage - armorSlot.damageValue

            /* Elytra Durability Warning, runs when player is in the air and durability changed */
            if (!player.onGround() && oldDurability != elytraDurability) {
                if (durabilityWarning && elytraDurability > 1 && elytraDurability < threshold * armorSlot.maxDamage / 100) {
                    world.playLocalSound(
                        player.x, player.y, player.z,
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS,
                        0.1F,
                        1f,
                        false
                    )
                    ChatUtils.sendMessage("$chatName Warning: Elytra has " + (elytraDurability - 1) + " durability remaining")
                } else if (elytraDurability <= 1 && !outOfDurability) {
                    outOfDurability = true
                    if (durabilityWarning) {
                        world.playLocalSound(
                            player.x, player.y, player.z,
                            SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundSource.PLAYERS,
                            0.1F,
                            1f,
                            false
                        )
                        ChatUtils.sendMessage("$chatName Elytra is out of durability, holding player in the air")
                    }
                }
            }
        } else elytraDurability = 0

        /* Holds player in the air if run out of durability */
        if (!player.onGround() && elytraDurability <= 1 && outOfDurability) {
            holdPlayer(event)
        } else if (outOfDurability) outOfDurability =
            false /* Reset if players is on ground or replace with a new elytra */

        /* wasInLiquid check */
        if (player.isInWater || player.isInLava) {
            wasInLiquid = true
        } else if (player.onGround() || isFlying || isPacketFlying) {
            wasInLiquid = false
        }

        /* Elytra flying status check */
        isFlying = player.isFallFlying || (player.abilities.flying && mode == ElytraFlightMode.CREATIVE)

        /* Movement input check */
        isStandingStillH = player.input.forwardImpulseCompat == 0f
        isStandingStill = isStandingStillH && !player.input.keyPresses.jump && !player.input.keyPresses.shift
        /* Reset acceleration */
        if (!isFlying || isStandingStill) speedPercentage = accelerateStartSpeed.toFloat()
    }

    private fun NonNullContext.reset(cancelFlying: Boolean) {
        wasInLiquid = false
        isFlying = false
        isPacketFlying = false
        modifyTimer(50f)
        player.abilities.flyingSpeed = 0.05f
        if (cancelFlying) player.abilities.flying = false
    }

    /* Holds player in the air */
    private fun NonNullContext.holdPlayer(event: PlayerTravelEvent.Pre) {
        event.cancel()
        modifyTimer(50f)
        player.setDeltaMovement(0.0, -0.01, 0.0)
    }

    /* Auto landing */
    private fun NonNullContext.landing(event: PlayerTravelEvent.Pre) {
        when {
            player.onGround() -> {
                ChatUtils.sendMessage("$chatName Landed!")
                autoLanding = false
                return
            }
//            LagNotifier.paused && LagNotifier.pauseTakeoff -> {
//                holdPlayer(event)
//            }
            player.abilities.flying || !player.isFallFlying || isPacketFlying -> {
                reset(true)
                takeoff(event)
                return
            }
            else -> {
                when {
                    player.y > world.getGroundPos(player).y + 1.0 -> {
                        modifyTimer(50f)
                        val velocityY = max(min(-(player.y - world.getGroundPos(player).y) / 20.0, -0.5), -5.0)
                        player.velocityY = velocityY
                    }
                    player.velocityY != 0.0 -> { /* Pause falling to reset fall distance */
                        if (!mc.isSingleplayer) modifyTimer(200.0f) /* Use timer to pause longer */
                        player.velocityY = 0.0
                    }
                    else -> {
                        player.velocityY -= 0.2
                    }
                }
            }
        }
        player.velocityX = 0.0 /* Kills horizontal motion */
        player.velocityZ = 0.0
        event.cancel()
    }

    /* The best takeoff method <3 */
    private fun NonNullContext.takeoff(event: PlayerTravelEvent.Pre) {
        /* Pause Takeoff if server is lagging, player is in water/lava, or player is on ground */
        val timerSpeed = if (highPingOptimize) 400.0f else 200.0f
        val height = if (highPingOptimize) 0.0f else minTakeoffHeight
        val closeToGround = player.y <= world.getGroundPos(player).y + height && !wasInLiquid && !mc.isSingleplayer

        if (!easyTakeOff/* || (LagNotifier.paused && LagNotifier.pauseTakeoff)*/ || player.onGround()) {
//            if (LagNotifier.paused && LagNotifier.pauseTakeoff && player.posY - world.getGroundLevel(player) > 4.0f)
//                holdPlayer(event) /* Holds player in the air if server is lagging and the distance is enough for taking fall damage */
            reset(player.onGround())
            return
        }

        if (player.velocityY < 0 && !highPingOptimize || player.velocityY < -0.02) {
            if (closeToGround) {
                modifyTimer(25f)
                return
            }

            if (!highPingOptimize && !wasInLiquid && !mc.isSingleplayer) { /* Cringe moment when you use elytra flight in single player world */
                event.cancel()
                player.setDeltaMovement(0.0, -0.02, 0.0)
            }

            if (timerControl && !mc.isSingleplayer) modifyTimer(timerSpeed * 2.0f)
            netHandler.send(ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING))
            hoverTarget = player.y + 0.2
        } else if (highPingOptimize && !closeToGround) {
            modifyTimer(timerSpeed)
        }
    }

    /**
     *  Calculate yaw for control and packet mode
     *
     *  @return Yaw in radians based on player rotation yaw and movement input
     */
    private fun NonNullContext.getYaw(): Double {
        val yawRad = player.calcMoveYaw()
        packetYaw = yawRad.toDegree().toFloat()
        return yawRad
    }

    /**
     * Calculate a speed with a non linear acceleration over time
     *
     * @return boostingSpeed if [boosting] is true, else return a accelerated speed.
     */
    private fun getSpeed(boosting: Boolean): Double {
        return when {
            boosting -> (if (ncpStrict) min(speedControl, 2.0f) else speedControl).toDouble()

            accelerateTime != 0.0f && accelerateStartSpeed != 100 -> {
                speedPercentage =
                    min(speedPercentage + (100.0f - accelerateStartSpeed) / (accelerateTime * 20.0f), 100.0f)
                val speedMultiplier = speedPercentage / 100.0

                getSettingSpeed() * speedMultiplier * (cos(speedMultiplier * PI) * -0.5 + 0.5)
            }

            else -> getSettingSpeed().toDouble()
        }
    }

    private fun getSettingSpeed(): Float {
        return when (mode) {
            ElytraFlightMode.BOOST -> speedBoost
            ElytraFlightMode.CONTROL -> speedControl
            ElytraFlightMode.CREATIVE -> speedCreative
            ElytraFlightMode.PACKET -> speedPacket
        }
    }

    private fun NonNullContext.setSpeed(yaw: Double, boosting: Boolean) {
        val acceleratedSpeed = getSpeed(boosting)
        player.setDeltaMovement(sin(-yaw) * acceleratedSpeed, player.velocityY, cos(yaw) * acceleratedSpeed)
    }
    /* End of Generic Functions */

    /* Boost mode */
    private fun NonNullContext.boostMode() {
        val yaw = player.yRot.toDouble().toRadian()
        player.velocityX -= player.input.forwardImpulseCompat * sin(yaw) * speedBoost / 20
        if (player.input.keyPresses.jump) player.velocityY += upSpeedBoost / 15
        else if (player.input.keyPresses.shift) player.velocityY -= downSpeedBoost / 15
        player.velocityZ += player.input.forwardImpulseCompat * cos(yaw) * speedBoost / 20
    }

    /* Control Mode */
    private fun NonNullContext.controlMode(event: PlayerTravelEvent.Pre) {
        /* States and movement input */
        val currentSpeed = sqrt(player.velocityX * player.velocityX + player.velocityZ * player.velocityZ)
        val moveUp =
            if (!legacyLookBoost) player.input.keyPresses.jump else player.pitch < -10.0f && !isStandingStillH
        @Suppress("RemoveRedundantQualifierName") val guiMoveEnabled = GuiMove.isEnabled
        val moveDown = if (guiMoveEnabled && !GuiMove.sneak && mc.screen != null || moveUp) false else player.input.keyPresses.shift

        /* Dynamic down speed */
        val calcDownSpeed = if (dynamicDownSpeed) {
            val minDownSpeed = min(downSpeedControl, fastDownSpeedControl).toDouble()
            val maxDownSpeed = max(downSpeedControl, fastDownSpeedControl).toDouble()
            if (player.pitch > 0) {
                player.pitch / 90.0 * (maxDownSpeed - minDownSpeed) + minDownSpeed
            } else minDownSpeed
        } else downSpeedControl.toDouble()

        /* Hover */
        if (hoverTarget < 0.0 || moveUp) hoverTarget = player.y else if (moveDown) hoverTarget =
            player.y - calcDownSpeed
        hoverState =
            (if (hoverState) player.y < hoverTarget else player.y < hoverTarget - 0.1) && altitudeHoldControl

        /* Set velocity */
        if (!isStandingStillH || moveUp) {
            if ((moveUp || hoverState) && (currentSpeed >= 0.8 || player.velocityY > 1.0)) {
                upwardFlight(currentSpeed, getYaw())
            } else { /* Runs when pressing wasd */
                packetPitch = forwardPitch.toFloat()
                player.velocityY = -fallSpeedControl.toDouble()
                setSpeed(getYaw(), moveUp)
                boostingTick = 0
            }
        } else player.setDeltaMovement(0.0, 0.0, 0.0) /* Stop moving if no inputs are pressed */

        if (moveDown) player.velocityY = -calcDownSpeed /* Runs when holding shift */

        event.cancel()
    }

    private fun NonNullContext.upwardFlight(currentSpeed: Double, yaw: Double) {
        val multipliedSpeed = 0.128 * min(speedControl, 2.0f)
        val strictPitch =
            Math.toDegrees(asin((multipliedSpeed - sqrt(multipliedSpeed * multipliedSpeed - 0.0348)) / 0.12)).toFloat()
        val basePitch = if (ncpStrict && strictPitch < boostPitchControl && !strictPitch.isNaN()) -strictPitch
        else -boostPitchControl.toFloat()
        val targetPitch = if (player.pitch < 0.0f) {
            max(
                player.pitch * (90.0f - boostPitchControl.toFloat()) / 90.0f - boostPitchControl.toFloat(),
                -90.0f
            )
        } else -boostPitchControl.toFloat()

        packetPitch = if (packetPitch <= basePitch && boostingTick > 2) {
            if (packetPitch < targetPitch) packetPitch += 17.0f
            if (packetPitch > targetPitch) packetPitch -= 17.0f
            max(packetPitch, targetPitch)
        } else basePitch
        boostingTick++

        /* These are actually the original Minecraft elytra fly code lol */
        val pitch = Math.toRadians(packetPitch.toDouble())
        val targetMotionX = sin(-yaw) * sin(-pitch)
        val targetMotionZ = cos(yaw) * sin(-pitch)
        val targetSpeed = sqrt(targetMotionX * targetMotionX + targetMotionZ * targetMotionZ)
        val upSpeed = currentSpeed * sin(-pitch) * 0.04
        val fallSpeed = cos(pitch) * cos(pitch) * 0.06 - 0.08

        player.velocityX -= upSpeed * targetMotionX / targetSpeed - (targetMotionX / targetSpeed * currentSpeed - player.velocityX) * 0.1
        player.velocityY += upSpeed * 3.2 + fallSpeed
        player.velocityZ -= upSpeed * targetMotionZ / targetSpeed - (targetMotionZ / targetSpeed * currentSpeed - player.velocityZ) * 0.1

        /* Passive motion loss */
        player.velocityX *= 0.99
        player.velocityY *= 0.98
        player.velocityZ *= 0.99
    }
    /* End of Control Mode */

    /* Creative Mode */
    private fun NonNullContext.creativeMode() {
        if (player.onGround()) {
            reset(true)
            return
        }

        packetPitch = forwardPitch.toFloat()
        player.abilities.flying = true
        player.abilities.flyingSpeed = getSpeed(false).toFloat()

        val motionY = when {
            isStandingStill -> 0.0
            player.input.keyPresses.jump -> upSpeedCreative.toDouble()
            player.input.keyPresses.shift -> -downSpeedCreative.toDouble()
            else -> -fallSpeedCreative.toDouble()
        }
        player.setDeltaMovement(0.0, motionY, 0.0) /* Remove the creative flight acceleration and set the motionY */
    }

    /* Packet Mode */
    private fun NonNullContext.packetMode(event: PlayerTravelEvent.Pre) {
        isPacketFlying = !player.onGround()
        netHandler.send(ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING))

        /* Set velocity */
        if (!isStandingStillH) { /* Runs when pressing wasd */
            setSpeed(getYaw(), false)
        } else player.setDeltaMovement(0.0, 0.0, 0.0)
        player.velocityY = (if (player.input.keyPresses.shift) -downSpeedPacket else -fallSpeedPacket).toDouble()

        event.cancel()
    }

    fun shouldSwing(): Boolean {
        return isEnabled && isFlying && !autoLanding && (mode == ElytraFlightMode.CONTROL || mode == ElytraFlightMode.PACKET)
    }

    private fun NonNullContext.spoofRotation() {
        if (player.isSpectator || !elytraIsEquipped || elytraDurability <= 1 || !isFlying) return

        var cancelRotation = false
        var rotation = Vec2f(player)

        if (autoLanding) {
            rotation = Vec2f(rotation.x, -20f)
        } else if (mode != ElytraFlightMode.BOOST) {
            if (!isStandingStill && mode != ElytraFlightMode.CREATIVE) rotation = Vec2f(packetYaw, rotation.y)
            if (spoofPitch) {
                if (!isStandingStill) rotation = Vec2f(rotation.x, packetPitch)

                /* Cancels rotation packets if player is not moving and not clicking */
                cancelRotation =
                    isStandingStill && ((!mc.options.keyUse.isDown && !mc.options.keyAttack.isDown && blockInteract) || !blockInteract)
            }
        }

        sendPlayerPacket {
            if (cancelRotation) {
                cancelRotate()
            } else {
                rotate(rotation)
            }
        }
    }

    init {
        onEnabled {
            autoLanding = false
            speedPercentage = accelerateStartSpeed.toFloat() /* For acceleration */
            hoverTarget = -1.0 /* For control mode */
        }

        onDisabled {
            runSafe { reset(true) }
        }

        /* Reset isFlying states when switching mode */
        mode0.register { _, _ ->
            runSafe { reset(true) }
            true
        }
    }
}
