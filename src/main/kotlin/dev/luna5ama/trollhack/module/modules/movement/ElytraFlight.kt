package dev.luna5ama.trollhack.module.modules.movement

import dev.fastmc.common.toRadians
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.player.PlayerTravelEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.player.LagNotifier
import dev.luna5ama.trollhack.util.*
import dev.luna5ama.trollhack.util.MovementUtils.calcMoveYaw
import dev.luna5ama.trollhack.util.MovementUtils.speed
import dev.luna5ama.trollhack.util.accessor.rotationPitch
import dev.luna5ama.trollhack.util.accessor.tickLength
import dev.luna5ama.trollhack.util.accessor.timer
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.runSafe
import dev.luna5ama.trollhack.util.world.getGroundLevel
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.init.Items
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.server.SPacketEntityMetadata
import net.minecraft.network.play.server.SPacketPlayerPosLook
import kotlin.math.*

// TODO: Rewrite
internal object ElytraFlight : Module(
    name = "Elytra Flight",
    description = "Allows infinite and way easier Elytra flying",
    category = Category.MOVEMENT,
    modulePriority = 1000
) {
    private val mode = setting("Mode", ElytraFlightMode.CONTROL)
    private val page = setting("Page", Page.GENERIC_SETTINGS)
    private val durabilityWarning0 = setting("Durability Warning", true, page.atValue(Page.GENERIC_SETTINGS))
    private val durabilityWarning by durabilityWarning0
    private val threshold by setting(
        "Warning Threshold",
        5,
        1..50,
        1,
        page.atValue(Page.GENERIC_SETTINGS) and (durabilityWarning0.atTrue()),
        description = "Threshold of durability to start sending warnings"
    )
    private var autoLanding by setting("Auto Landing", false, page.atValue(Page.GENERIC_SETTINGS), isTransient = true)

    /* Generic Settings */
    /* Takeoff */
    private val easyTakeOff0 = setting("Easy Takeoff", true, page.atValue(Page.GENERIC_SETTINGS))
    private val easyTakeOff by easyTakeOff0
    private val timerControl by setting("Takeoff Timer", true, page.atValue(Page.GENERIC_SETTINGS))
    private val highPingOptimize0 =
        setting("High Ping Optimize", false, page.atValue(Page.GENERIC_SETTINGS) and easyTakeOff0.atTrue())
    private val highPingOptimize by highPingOptimize0
    private val minTakeoffHeight by setting(
        "Min Takeoff Height",
        0.5f,
        0.0f..1.5f,
        0.1f,
        page.atValue(Page.GENERIC_SETTINGS) and easyTakeOff0.atTrue() and highPingOptimize0.atFalse()
    )

    /* Acceleration */
    private val accelerateStartSpeed by setting(
        "Start Speed",
        100,
        0..100,
        5,
        page.atValue(Page.GENERIC_SETTINGS) and mode.notAtValue(ElytraFlightMode.BOOST)
    )
    private val accelerateTime by setting(
        "Accelerate Time",
        0.0f,
        0.0f..20.0f,
        0.25f,
        page.atValue(Page.GENERIC_SETTINGS) and mode.notAtValue(ElytraFlightMode.BOOST)
    )

    /* Spoof Pitch */
    private val spoofPitch0 =
        setting("Spoof Pitch", true, page.atValue(Page.GENERIC_SETTINGS) and mode.notAtValue(ElytraFlightMode.BOOST))
    private val spoofPitch by spoofPitch0
    private val blockInteract by setting(
        "Block Interact",
        false,
        page.atValue(Page.GENERIC_SETTINGS) and mode.notAtValue(ElytraFlightMode.BOOST) and spoofPitch0.atTrue()
    )
    private val forwardPitch by setting(
        "Forward Pitch",
        0,
        -90..90,
        5,
        page.atValue(Page.GENERIC_SETTINGS) and mode.notAtValue(ElytraFlightMode.BOOST) and spoofPitch0.atTrue()
    )

    /* Extra */
    val elytraSounds by setting("Elytra Sounds", true, page.atValue(Page.GENERIC_SETTINGS))
    private val swingSpeed by setting(
        "Swing Speed",
        1.0f,
        0.0f..2.0f,
        0.1f,
        page.atValue(Page.GENERIC_SETTINGS) and mode.atValue(ElytraFlightMode.CONTROL, ElytraFlightMode.PACKET)
    )
    private val swingAmount by setting(
        "Swing Amount",
        0.8f,
        0.0f..2.0f,
        0.1f,
        page.atValue(Page.GENERIC_SETTINGS) and mode.atValue(ElytraFlightMode.CONTROL, ElytraFlightMode.PACKET)
    )
    /* End of Generic Settings */

    /* Mode Settings */
    /* Boost */
    private val speedBoost by setting(
        "Speed B",
        1.0f,
        0.0f..10.0f,
        0.1f,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.BOOST)
    )
    private val upSpeedBoost by setting(
        "Up Speed B",
        1.0f,
        1.0f..5.0f,
        0.1f,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.BOOST)
    )
    private val downSpeedBoost by setting(
        "Down Speed B",
        1.0f,
        1.0f..5.0f,
        0.1f,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.BOOST)
    )

    /* Control */
    private val boostPitchControl by setting(
        "Base Boost Pitch",
        20,
        0..90,
        5,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.CONTROL)
    )
    private val ncpStrict by setting(
        "NCP Strict",
        true,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.CONTROL)
    )
    private val legacyLookBoost by setting(
        "Legacy Look Boost",
        false,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.CONTROL)
    )
    private val altitudeHoldControl by setting(
        "Auto Control Altitude",
        false,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.CONTROL)
    )
    private val dynamicDownSpeed0 = setting(
        "Dynamic Down Speed",
        false,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.CONTROL)
    )
    private val dynamicDownSpeed by dynamicDownSpeed0
    private val speedControl by setting(
        "Speed C",
        1.81f,
        0.0f..10.0f,
        0.1f,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.CONTROL)
    )
    private val fallSpeedControl by setting(
        "Fall Speed C",
        0.00000000000003f,
        0.0f..0.3f,
        0.01f,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.CONTROL)
    )
    private val downSpeedControl by setting(
        "Down Speed C",
        1.0f,
        1.0f..5.0f,
        0.1f,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.CONTROL)
    )
    private val fastDownSpeedControl by setting(
        "Dynamic Down Speed C",
        2.0f,
        1.0f..5.0f,
        0.1f,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.CONTROL) and dynamicDownSpeed0.atTrue()
    )

    /* Creative */
    private val speedCreative by setting(
        "Speed CR",
        1.8f,
        0.0f..10.0f,
        0.1f,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.CREATIVE)
    )
    private val fallSpeedCreative by setting(
        "Fall Speed CR",
        0.00001f,
        0.0f..0.3f,
        0.01f,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.CREATIVE)
    )
    private val upSpeedCreative by setting(
        "Up Speed CR",
        1.0f,
        1.0f..5.0f,
        0.1f,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.CREATIVE)
    )
    private val downSpeedCreative by setting(
        "Down Speed CR",
        1.0f,
        1.0f..5.0f,
        0.1f,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.CREATIVE)
    )

    /* Packet */
    private val speedPacket by setting(
        "Speed P",
        1.8f,
        0.0f..20.0f,
        0.1f,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.PACKET)
    )
    private val fallSpeedPacket by setting(
        "Fall Speed P",
        0.00001f,
        0.0f..0.3f,
        0.01f,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.PACKET)
    )
    private val downSpeedPacket by setting(
        "Down Speed P",
        1.0f,
        0.1f..5.0f,
        0.1f,
        page.atValue(Page.MODE_SETTINGS) and mode.atValue(ElytraFlightMode.PACKET)
    )
    /* End of Mode Settings */

    private enum class ElytraFlightMode(override val displayName: CharSequence) : DisplayEnum {
        BOOST("Boost"),
        CONTROL("Control"),
        CREATIVE("Creative"),
        PACKET("Packet")
    }

    private enum class Page {
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

    override fun getHudInfo(): String {
        return mode.value.displayString
    }

    override fun isActive(): Boolean {
        return isEnabled && (isFlying || isPacketFlying)
    }

    /* Event Listeners */
    init {
        safeListener<PacketEvent.Receive> {
            if (player.isSpectator || !elytraIsEquipped || elytraDurability <= 1 || !isFlying || mode.value == ElytraFlightMode.BOOST) return@safeListener
            if (it.packet is SPacketPlayerPosLook && mode.value != ElytraFlightMode.PACKET) {
                val packet = it.packet
                packet.rotationPitch = player.rotationPitch
            }

            /* Cancels the elytra opening animation */
            if (it.packet is SPacketEntityMetadata && isPacketFlying) {
                val packet = it.packet
                if (packet.entityId == player.entityId) it.cancel()
            }
        }

        safeListener<PlayerTravelEvent> {
            if (player.isSpectator) return@safeListener
            stateUpdate(it)
            if (elytraIsEquipped && elytraDurability > 1) {
                if (autoLanding) {
                    landing(it)
                    return@safeListener
                }
                if (!isFlying && !isPacketFlying) {
                    takeoff(it)
                } else {
                    mc.timer.tickLength = 50.0f
                    player.isSprinting = false
                    when (mode.value) {
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
    private fun SafeClientEvent.stateUpdate(event: PlayerTravelEvent) {
        /* Elytra Check */
        val armorSlot = player.inventory.armorInventory[2]
        elytraIsEquipped = armorSlot.item == Items.ELYTRA

        /* Elytra Durability Check */
        if (elytraIsEquipped) {
            val oldDurability = elytraDurability
            elytraDurability = armorSlot.maxDamage - armorSlot.itemDamage

            /* Elytra Durability Warning, runs when player is in the air and durability changed */
            if (!player.onGround && oldDurability != elytraDurability) {
                if (durabilityWarning && elytraDurability > 1 && elytraDurability < threshold * armorSlot.maxDamage / 100) {
                    mc.soundHandler.playSound(
                        PositionedSoundRecord.getRecord(
                            SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                            1.0f,
                            1.0f
                        )
                    )
                    NoSpamMessage.sendMessage("$chatName Warning: Elytra has " + (elytraDurability - 1) + " durability remaining")
                } else if (elytraDurability <= 1 && !outOfDurability) {
                    outOfDurability = true
                    if (durabilityWarning) {
                        mc.soundHandler.playSound(
                            PositionedSoundRecord.getRecord(
                                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                                1.0f,
                                1.0f
                            )
                        )
                        NoSpamMessage.sendMessage("$chatName Elytra is out of durability, holding player in the air")
                    }
                }
            }
        } else elytraDurability = 0

        /* Holds player in the air if run out of durability */
        if (!player.onGround && elytraDurability <= 1 && outOfDurability) {
            holdPlayer(event)
        } else if (outOfDurability) outOfDurability =
            false /* Reset if players is on ground or replace with a new elytra */

        /* wasInLiquid check */
        if (player.isInWater || player.isInLava) {
            wasInLiquid = true
        } else if (player.onGround || isFlying || isPacketFlying) {
            wasInLiquid = false
        }

        /* Elytra flying status check */
        isFlying = player.isElytraFlying || (player.capabilities.isFlying && mode.value == ElytraFlightMode.CREATIVE)

        /* Movement input check */
        isStandingStillH = player.movementInput.moveForward == 0f && player.movementInput.moveStrafe == 0f
        isStandingStill = isStandingStillH && !player.movementInput.jump && !player.movementInput.sneak

        /* Reset acceleration */
        if (!isFlying || isStandingStill) speedPercentage = accelerateStartSpeed.toFloat()

        /* Modify leg swing */
        if (shouldSwing()) {
            player.prevLimbSwingAmount = player.limbSwingAmount
            player.limbSwing += swingSpeed
            val speedRatio = (player.speed / getSettingSpeed()).toFloat()
            player.limbSwingAmount += ((speedRatio * swingAmount) - player.limbSwingAmount) * 0.4f
        }
    }

    private fun SafeClientEvent.reset(cancelFlying: Boolean) {
        wasInLiquid = false
        isFlying = false
        isPacketFlying = false
        mc.timer.tickLength = 50.0f
        player.capabilities.flySpeed = 0.05f
        if (cancelFlying) player.capabilities.isFlying = false
    }

    /* Holds player in the air */
    private fun SafeClientEvent.holdPlayer(event: PlayerTravelEvent) {
        event.cancel()
        mc.timer.tickLength = 50.0f
        player.setVelocity(0.0, -0.01, 0.0)
    }

    /* Auto landing */
    private fun SafeClientEvent.landing(event: PlayerTravelEvent) {
        when {
            player.onGround -> {
                NoSpamMessage.sendMessage("$chatName Landed!")
                autoLanding = false
                return
            }
            LagNotifier.paused && LagNotifier.pauseTakeoff -> {
                holdPlayer(event)
            }
            player.capabilities.isFlying || !player.isElytraFlying || isPacketFlying -> {
                reset(true)
                takeoff(event)
                return
            }
            else -> {
                when {
                    player.posY > world.getGroundLevel(player) + 1.0 -> {
                        mc.timer.tickLength = 50.0f
                        player.motionY = max(min(-(player.posY - world.getGroundLevel(player)) / 20.0, -0.5), -5.0)
                    }
                    player.motionY != 0.0 -> { /* Pause falling to reset fall distance */
                        if (!mc.isSingleplayer) mc.timer.tickLength = 200.0f /* Use timer to pause longer */
                        player.motionY = 0.0
                    }
                    else -> {
                        player.motionY = -0.2
                    }
                }
            }
        }
        player.setVelocity(0.0, player.motionY, 0.0) /* Kills horizontal motion */
        event.cancel()
    }

    /* The best takeoff method <3 */
    private fun SafeClientEvent.takeoff(event: PlayerTravelEvent) {
        /* Pause Takeoff if server is lagging, player is in water/lava, or player is on ground */
        val timerSpeed = if (highPingOptimize) 400.0f else 200.0f
        val height = if (highPingOptimize) 0.0f else minTakeoffHeight
        val closeToGround = player.posY <= world.getGroundLevel(player) + height && !wasInLiquid && !mc.isSingleplayer

        if (!easyTakeOff || (LagNotifier.paused && LagNotifier.pauseTakeoff) || player.onGround) {
            if (LagNotifier.paused && LagNotifier.pauseTakeoff && player.posY - world.getGroundLevel(player) > 4.0f) holdPlayer(
                event
            ) /* Holds player in the air if server is lagging and the distance is enough for taking fall damage */
            reset(player.onGround)
            return
        }

        if (player.motionY < 0 && !highPingOptimize || player.motionY < -0.02) {
            if (closeToGround) {
                mc.timer.tickLength = 25.0f
                return
            }

            if (!highPingOptimize && !wasInLiquid && !mc.isSingleplayer) { /* Cringe moment when you use elytra flight in single player world */
                event.cancel()
                player.setVelocity(0.0, -0.02, 0.0)
            }

            if (timerControl && !mc.isSingleplayer) mc.timer.tickLength = timerSpeed * 2.0f
            connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_FALL_FLYING))
            hoverTarget = player.posY + 0.2
        } else if (highPingOptimize && !closeToGround) {
            mc.timer.tickLength = timerSpeed
        }
    }

    /**
     *  Calculate yaw for control and packet mode
     *
     *  @return Yaw in radians based on player rotation yaw and movement input
     */
    private fun SafeClientEvent.getYaw(): Double {
        val yawRad = player.calcMoveYaw()
        packetYaw = Math.toDegrees(yawRad).toFloat()
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
        return when (mode.value) {
            ElytraFlightMode.BOOST -> speedBoost
            ElytraFlightMode.CONTROL -> speedControl
            ElytraFlightMode.CREATIVE -> speedCreative
            ElytraFlightMode.PACKET -> speedPacket
        }
    }

    private fun SafeClientEvent.setSpeed(yaw: Double, boosting: Boolean) {
        val acceleratedSpeed = getSpeed(boosting)
        player.setVelocity(sin(-yaw) * acceleratedSpeed, player.motionY, cos(yaw) * acceleratedSpeed)
    }
    /* End of Generic Functions */

    /* Boost mode */
    private fun SafeClientEvent.boostMode() {
        val yaw = player.rotationYaw.toDouble().toRadians()
        player.motionX -= player.movementInput.moveForward * sin(yaw) * speedBoost / 20
        if (player.movementInput.jump) player.motionY += upSpeedBoost / 15 else if (player.movementInput.sneak) player.motionY -= downSpeedBoost / 15
        player.motionZ += player.movementInput.moveForward * cos(yaw) * speedBoost / 20
    }

    /* Control Mode */
    private fun SafeClientEvent.controlMode(event: PlayerTravelEvent) {
        /* States and movement input */
        val currentSpeed = sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ)
        val moveUp =
            if (!legacyLookBoost) player.movementInput.jump else player.rotationPitch < -10.0f && !isStandingStillH
        val moveDown =
            if (InventoryMove.isEnabled && !InventoryMove.sneak && mc.currentScreen != null || moveUp) false else player.movementInput.sneak

        /* Dynamic down speed */
        val calcDownSpeed = if (dynamicDownSpeed) {
            val minDownSpeed = min(downSpeedControl, fastDownSpeedControl).toDouble()
            val maxDownSpeed = max(downSpeedControl, fastDownSpeedControl).toDouble()
            if (player.rotationPitch > 0) {
                player.rotationPitch / 90.0 * (maxDownSpeed - minDownSpeed) + minDownSpeed
            } else minDownSpeed
        } else downSpeedControl.toDouble()

        /* Hover */
        if (hoverTarget < 0.0 || moveUp) hoverTarget = player.posY else if (moveDown) hoverTarget =
            player.posY - calcDownSpeed
        hoverState =
            (if (hoverState) player.posY < hoverTarget else player.posY < hoverTarget - 0.1) && altitudeHoldControl

        /* Set velocity */
        if (!isStandingStillH || moveUp) {
            if ((moveUp || hoverState) && (currentSpeed >= 0.8 || player.motionY > 1.0)) {
                upwardFlight(currentSpeed, getYaw())
            } else { /* Runs when pressing wasd */
                packetPitch = forwardPitch.toFloat()
                player.motionY = -fallSpeedControl.toDouble()
                setSpeed(getYaw(), moveUp)
                boostingTick = 0
            }
        } else player.setVelocity(0.0, 0.0, 0.0) /* Stop moving if no inputs are pressed */

        if (moveDown) player.motionY = -calcDownSpeed /* Runs when holding shift */

        event.cancel()
    }

    private fun SafeClientEvent.upwardFlight(currentSpeed: Double, yaw: Double) {
        val multipliedSpeed = 0.128 * min(speedControl, 2.0f)
        val strictPitch =
            Math.toDegrees(asin((multipliedSpeed - sqrt(multipliedSpeed * multipliedSpeed - 0.0348)) / 0.12)).toFloat()
        val basePitch = if (ncpStrict && strictPitch < boostPitchControl && !strictPitch.isNaN()) -strictPitch
        else -boostPitchControl.toFloat()
        val targetPitch = if (player.rotationPitch < 0.0f) {
            max(
                player.rotationPitch * (90.0f - boostPitchControl.toFloat()) / 90.0f - boostPitchControl.toFloat(),
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

        player.motionX -= upSpeed * targetMotionX / targetSpeed - (targetMotionX / targetSpeed * currentSpeed - player.motionX) * 0.1
        player.motionY += upSpeed * 3.2 + fallSpeed
        player.motionZ -= upSpeed * targetMotionZ / targetSpeed - (targetMotionZ / targetSpeed * currentSpeed - player.motionZ) * 0.1

        /* Passive motion loss */
        player.motionX *= 0.99
        player.motionY *= 0.98
        player.motionZ *= 0.99
    }
    /* End of Control Mode */

    /* Creative Mode */
    private fun SafeClientEvent.creativeMode() {
        if (player.onGround) {
            reset(true)
            return
        }

        packetPitch = forwardPitch.toFloat()
        player.capabilities.isFlying = true
        player.capabilities.flySpeed = getSpeed(false).toFloat()

        val motionY = when {
            isStandingStill -> 0.0
            player.movementInput.jump -> upSpeedCreative.toDouble()
            player.movementInput.sneak -> -downSpeedCreative.toDouble()
            else -> -fallSpeedCreative.toDouble()
        }
        player.setVelocity(0.0, motionY, 0.0) /* Remove the creative flight acceleration and set the motionY */
    }

    /* Packet Mode */
    private fun SafeClientEvent.packetMode(event: PlayerTravelEvent) {
        isPacketFlying = !player.onGround
        connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_FALL_FLYING))

        /* Set velocity */
        if (!isStandingStillH) { /* Runs when pressing wasd */
            setSpeed(getYaw(), false)
        } else player.setVelocity(0.0, 0.0, 0.0)
        player.motionY = (if (player.movementInput.sneak) -downSpeedPacket else -fallSpeedPacket).toDouble()

        event.cancel()
    }

    fun shouldSwing(): Boolean {
        return isEnabled && isFlying && !autoLanding && (mode.value == ElytraFlightMode.CONTROL || mode.value == ElytraFlightMode.PACKET)
    }

    private fun SafeClientEvent.spoofRotation() {
        if (player.isSpectator || !elytraIsEquipped || elytraDurability <= 1 || !isFlying) return

        var cancelRotation = false
        var rotation = Vec2f(player)

        if (autoLanding) {
            rotation = Vec2f(rotation.x, -20f)
        } else if (mode.value != ElytraFlightMode.BOOST) {
            if (!isStandingStill && mode.value != ElytraFlightMode.CREATIVE) rotation = Vec2f(packetYaw, rotation.y)
            if (spoofPitch) {
                if (!isStandingStill) rotation = Vec2f(rotation.x, packetPitch)

                /* Cancels rotation packets if player is not moving and not clicking */
                cancelRotation =
                    isStandingStill && ((!mc.gameSettings.keyBindUseItem.isKeyDown && !mc.gameSettings.keyBindAttack.isKeyDown && blockInteract) || !blockInteract)
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
        onEnable {
            autoLanding = false
            speedPercentage = accelerateStartSpeed.toFloat() /* For acceleration */
            hoverTarget = -1.0 /* For control mode */
        }

        onDisable {
            runSafe { reset(true) }
        }

        /* Reset isFlying states when switching mode */
        mode.listeners.add {
            runSafe { reset(true) }
        }
    }
}