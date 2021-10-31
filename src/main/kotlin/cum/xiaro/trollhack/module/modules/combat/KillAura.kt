package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.util.interfaces.DisplayEnum
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.RunGameLoopEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.CombatManager
import cum.xiaro.trollhack.manager.managers.HotbarManager
import cum.xiaro.trollhack.manager.managers.PlayerPacketManager
import cum.xiaro.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.*
import cum.xiaro.trollhack.util.combat.CombatUtils
import cum.xiaro.trollhack.util.combat.CombatUtils.equipBestWeapon
import cum.xiaro.trollhack.util.combat.CombatUtils.scaledHealth
import cum.xiaro.trollhack.util.inventory.operation.swapToSlot
import cum.xiaro.trollhack.util.items.isWeapon
import cum.xiaro.trollhack.util.math.RotationUtils.faceEntityClosest
import cum.xiaro.trollhack.util.math.RotationUtils.getRotationToEntityClosest
import cum.xiaro.trollhack.util.math.isInSight
import cum.xiaro.trollhack.util.pause.MainHandPause
import cum.xiaro.trollhack.util.pause.withPause
import cum.xiaro.trollhack.util.threads.runSafe
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import kotlin.random.Random

@CombatManager.CombatModule
internal object KillAura : Module(
    name = "KillAura",
    alias = arrayOf("KA", "Aura", "TriggerBot"),
    category = Category.COMBAT,
    description = "Hits entities around you",
    modulePriority = 50
) {
    private val mode0 = setting("Mode", Mode.COOLDOWN)
    private val mode by mode0
    private val rotationMode by setting("Rotation Mode", RotationMode.SPOOF)
    private val delayTicks by setting("Delay Ticks", 5, 0..40, 1, mode0.atValue(Mode.TICKS))
    private val delayMs by setting("Delay ms", 50, 0..1000, 1, mode0.atValue(Mode.MS))
    private val randomDelay by setting("Random Delay", 0.0f, 0.0f..5.0f, 0.1f, mode0.notAtValue(Mode.COOLDOWN))
    private val disableOnDeath by setting("Disable On Death", false)
    private val tpsSync by setting("TPS Sync", false)
    private val armorFucker0 = setting("Armor Ddos", false)
    private val armorDdos by armorFucker0
    private val weaponOnly by setting("Weapon Only", false, armorFucker0.atFalse())
    private val autoWeapon0 = setting("Auto Weapon", true, armorFucker0.atFalse())
    private val autoBlock by setting("Auto Block", false)
    private val autoWeapon by autoWeapon0
    private val prefer by setting("Prefer", CombatUtils.PreferWeapon.SWORD, armorFucker0.atFalse() and autoWeapon0.atTrue())
    private val minSwapHealth by setting("Min Swap Health", 5.0f, 1.0f..20.0f, 0.5f)
    private val swapDelay by setting("Swap Delay", 10, 0..50, 1)
    val range by setting("Range", 4.0f, 0.0f..6.0f, 0.1f)

    private val timer = TickTimer()
    private var inactiveTicks = 0
    private var random = 0L
    private var lastSlot = 0
    private var blocking = false

    private enum class Mode(override val displayName: CharSequence) : DisplayEnum {
        COOLDOWN("Cooldown"),
        TICKS("Ticks"),
        MS("ms")
    }

    @Suppress("UNUSED")
    private enum class RotationMode(override val displayName: CharSequence) : DisplayEnum {
        OFF("Off"),
        SPOOF("Spoof"),
        VIEW_LOCK("View Lock")
    }

    override fun isActive(): Boolean {
        return isEnabled && inactiveTicks <= 5
    }

    override fun getHudInfo(): String {
        return mode.displayString
    }

    init {
        onDisable {
            runSafe {
                stopBlocking()
            }
        }

        safeListener<RunGameLoopEvent.Tick> {
            if (mode == Mode.MS) {
                runKillAura()
            }
        }

        safeListener<TickEvent.Post> {
            inactiveTicks++
            runKillAura()

            if (autoBlock && inactiveTicks < 5 && player.heldItemMainhand.item is ItemSword) {
                startBlocking()
            } else {
                stopBlocking()
            }
        }
    }

    private fun SafeClientEvent.runKillAura() {
        if (!player.isEntityAlive) {
            if (disableOnDeath) disable()
            return
        }
        val target = CombatManager.target ?: return

        if (CombatSetting.pause) return
        if (player.getDistance(target) >= range) return
        if (swapDelay > 0 && System.currentTimeMillis() - HotbarManager.swapTime < swapDelay * 50L) return

        MainHandPause.withPause(KillAura) {
            inactiveTicks = 0
            if (!armorDdos) {
                if (autoWeapon && player.scaledHealth > minSwapHealth) equipBestWeapon(prefer)
                if (weaponOnly && !player.heldItemMainhand.item.isWeapon) return
            }

            rotate(target)
            if (canAttack(target)) {
                if (armorDdos) {
                    swapToSlot(lastSlot)
                    lastSlot = (lastSlot + 1) % 9
                }
                stopBlocking()
                attack(target)
            }
        }
    }

    private fun SafeClientEvent.rotate(target: EntityLivingBase) {
        when (rotationMode) {
            RotationMode.SPOOF -> {
                sendPlayerPacket {
                    rotate(getRotationToEntityClosest(target))
                }
            }
            RotationMode.VIEW_LOCK -> {
                faceEntityClosest(target)
            }
            else -> {
                // Rotation off
            }
        }
    }

    private fun SafeClientEvent.canAttack(target: EntityLivingBase): Boolean {
        return when (mode) {
            Mode.COOLDOWN -> {
                val adjustTicks = if (!tpsSync) 0.0f
                else TpsCalculator.adjustTicks
                player.getCooledAttackStrength(adjustTicks) > 0.9f
            }
            Mode.TICKS -> {
                timer.tickAndReset(delayTicks * 50L + random)
            }
            Mode.MS -> {
                timer.tickAndReset(delayMs + random)
            }
        } && target.entityBoundingBox.isInSight(PlayerPacketManager.position.add(0.0, player.eyeHeight.toDouble(), 0.0), PlayerPacketManager.rotation) != null
    }

    private fun SafeClientEvent.attack(entity: Entity) {
        playerController.attackEntity(player, entity)
        player.swingArm(EnumHand.MAIN_HAND)

        random = if (mode != Mode.COOLDOWN && randomDelay > 0.0f) {
            Random.nextLong((delay * randomDelay).toLong() + 1L)
        } else {
            0L
        }
    }

    private fun SafeClientEvent.startBlocking() {
        if (!blocking) {
            connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
            blocking = true
        }
    }

    private fun SafeClientEvent.stopBlocking() {
        if (blocking) {
            connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            blocking = false
        }
    }

    private val delay: Long
        get() = if (mode == Mode.TICKS) {
            delayTicks * 50L
        } else {
            delayMs.toLong()
        }
}
