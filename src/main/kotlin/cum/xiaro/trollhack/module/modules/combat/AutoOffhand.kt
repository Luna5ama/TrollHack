package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.util.extension.next
import cum.xiaro.trollhack.util.interfaces.DisplayEnum
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.RunGameLoopEvent
import cum.xiaro.trollhack.event.events.combat.CrystalSpawnEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.CombatManager
import cum.xiaro.trollhack.manager.managers.EntityManager
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.*
import cum.xiaro.trollhack.util.EntityUtils.isFakeOrSelf
import cum.xiaro.trollhack.util.MovementUtils.realSpeed
import cum.xiaro.trollhack.util.accessor.potion
import cum.xiaro.trollhack.util.combat.CombatUtils.calcDamageFromMob
import cum.xiaro.trollhack.util.combat.CombatUtils.calcDamageFromPlayer
import cum.xiaro.trollhack.util.combat.CombatUtils.scaledHealth
import cum.xiaro.trollhack.util.inventory.InventoryTask
import cum.xiaro.trollhack.util.inventory.confirmedOrTrue
import cum.xiaro.trollhack.util.inventory.inventoryTaskNow
import cum.xiaro.trollhack.util.inventory.operation.moveTo
import cum.xiaro.trollhack.util.inventory.operation.swapToItemOrMove
import cum.xiaro.trollhack.util.inventory.slot.craftingSlots
import cum.xiaro.trollhack.util.inventory.slot.hotbarSlots
import cum.xiaro.trollhack.util.inventory.slot.inventorySlots
import cum.xiaro.trollhack.util.inventory.slot.offhandSlot
import cum.xiaro.trollhack.util.items.isWeapon
import cum.xiaro.trollhack.util.pause.MainHandPause
import cum.xiaro.trollhack.util.pause.withPause
import cum.xiaro.trollhack.util.text.MessageSendUtils
import cum.xiaro.trollhack.util.threads.defaultScope
import cum.xiaro.trollhack.util.threads.onMainThread
import cum.xiaro.trollhack.util.threads.runSafe
import kotlinx.coroutines.launch
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.entity.projectile.EntityTippedArrow
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemStack
import net.minecraft.potion.PotionUtils
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

internal object AutoOffhand : Module(
    name = "AutoOffhand",
    description = "Manages item in your offhand",
    category = Category.COMBAT,
    modulePriority = 2000
) {
    // General
    private val priority by setting("Priority", Priority.INVENTORY)
    private val switchMessage by setting("Switch Message", false)
    private val delay by setting("Delay", 1, 1..20, 1, description = "Ticks to wait between each move")
    private val confirmTimeout by setting("Confirm Timeout", 4, 1..20, 1, description = "Maximum ticks to wait for confirm packets from server")
    private val damageTimeout by setting("Damage Timeout", 100, 10..1000, 10)
    private val type = setting("Type", Type.TOTEM)

    // Totem
    private val staticHp by setting("Static Hp", 12.0f, 1f..20f, 0.5f, type.atValue(Type.TOTEM))
    private val damageHp by setting("Damage Hp", 4.0f, 1f..20f, 0.5f, type.atValue(Type.TOTEM))
    private val mainHandTotem by setting("Main Hand Totem", false, type.atValue(Type.TOTEM))
    private val checkDamage0 = setting("Check Damage", true, type.atValue(Type.TOTEM))
    private val checkDamage by checkDamage0
    private val falling by setting("Falling", true, type.atValue(Type.TOTEM) and checkDamage0.atTrue())
    private val mob by setting("Mob", true, type.atValue(Type.TOTEM) and checkDamage0.atTrue())
    private val player by setting("Player", true, type.atValue(Type.TOTEM) and checkDamage0.atTrue())
    private val arrow by setting("Arrow", true, type.atValue(Type.TOTEM) and checkDamage0.atTrue())
    private val crystal0 = setting("Crystal", true, type.atValue(Type.TOTEM) and checkDamage0.atTrue())
    private val crystal by crystal0
    private val crystalBias by setting("Crystal Bias", 1.1f, 0.0f..2.0f, 0.05f, type.atValue(Type.TOTEM) and checkDamage0.atTrue() and crystal0.atTrue())

    // Gapple
    private val offhandGapple0 = setting("Offhand Gapple", true, type.atValue(Type.GAPPLE))
    private val offhandGapple by offhandGapple0
    private val checkAuraG by setting("Check Aura G", true, type.atValue(Type.GAPPLE) and offhandGapple0.atTrue())
    private val checkWeaponG by setting("Check Weapon G", false, type.atValue(Type.GAPPLE) and offhandGapple0.atTrue())
    private val checkCAGapple by setting("Check CrystalAura G", true, type.atValue(Type.GAPPLE) and offhandGapple0.atTrue() and { !offhandCrystal })

    // Strength
    private val offhandStrength0 = setting("Offhand Strength", true, type.atValue(Type.STRENGTH))
    private val offhandStrength by offhandStrength0
    private val checkAuraS by setting("Check KillAura S", true, type.atValue(Type.STRENGTH) and offhandStrength0.atTrue())
    private val checkWeaponS by setting("Check Weapon S", false, type.atValue(Type.STRENGTH) and offhandStrength0.atTrue())

    // Crystal
    private val offhandCrystal0 = setting("Offhand Crystal", true, type.atValue(Type.CRYSTAL))
    private val offhandCrystal by offhandCrystal0
    private val checkCACrystal by setting("Check CrystalAura C", true, type.atValue(Type.CRYSTAL) and offhandCrystal0.atTrue())

    // Bed
    private val offhandBed0 = setting("Offhand Bed", true, type.atValue(Type.BED))
    private val offhandBed by offhandBed0
    private val checkBedAuraB by setting("Check BedAura B", true, type.atValue(Type.BED))

    @Suppress("UnusedEquals")
    enum class Type(override val displayName: CharSequence, val filter: (ItemStack) -> Boolean) : DisplayEnum {
        TOTEM("Totem", { it.item == Items.TOTEM_OF_UNDYING }),
        GAPPLE("Gapple", { it.item == Items.GOLDEN_APPLE }),
        STRENGTH("Strength", { it.item is ItemPotion && PotionUtils.getEffectsFromStack(it).any { it.potion == MobEffects.STRENGTH } }),
        CRYSTAL("Crystal", { it.item == Items.END_CRYSTAL }),
        BED("Bed", { it.item == Items.BED })
    }

    @Suppress("UNUSED")
    private enum class Priority {
        INVENTORY, HOTBAR
    }

    private val timer = TickTimer()
    private val damageTimer = TickTimer()
    private var lastDamage = 0.0f
    private var lastTask: InventoryTask? = null

    var lastType: Type? = null; private set

    override fun getHudInfo(): String {
        return lastType?.displayString ?: ""
    }

    init {
        onDisable {
            damageTimer.reset(-69420L)
            lastDamage = 0.0f
            lastType = null
        }

        listener<CrystalSpawnEvent> {
            if (checkDamage && crystal) {
                val damage = it.crystalDamage.selfDamage.pow(crystalBias)
                val flag = synchronized(damageTimer) {
                    if (damage >= lastDamage) {
                        lastDamage = damage
                        damageTimer.reset()
                        true
                    } else {
                        false
                    }
                }

                if (flag) {
                    runSafe {
                        if (checkTotem()) switchToType(Type.TOTEM)
                    }
                }
            }
        }

        safeListener<RunGameLoopEvent.Tick>(1100) {
            if (player.isDead || player.health <= 0.0f || !lastTask.confirmedOrTrue || !timer.tickAndReset(10L)) return@safeListener

            defaultScope.launch {
                updateDamage()
                switchToType(getType(), true)
            }
        }
    }

    private fun SafeClientEvent.getType() = when {
        checkTotem() -> Type.TOTEM
        checkStrength() -> Type.STRENGTH
        checkGapple() -> Type.GAPPLE
        checkBed() -> Type.BED
        checkCrystal() -> Type.CRYSTAL
        player.heldItemOffhand.isEmpty -> Type.TOTEM
        else -> null
    }

    private fun SafeClientEvent.checkTotem() = player.scaledHealth < staticHp
        || (checkDamage && player.scaledHealth - lastDamage <= damageHp)

    private fun SafeClientEvent.checkGapple() = offhandGapple
        && (checkAuraG && CombatManager.isActiveAndTopPriority(KillAura)
        || checkWeaponG && player.heldItemMainhand.item.isWeapon
        || (checkCAGapple && !offhandCrystal) && CombatManager.isOnTopPriority(TrollAura))

    private fun checkBed(): Boolean {
        return offhandBed
            && (checkBedAuraB && BedAura.isEnabled && BedAura.needOffhandBed)
    }

    private fun checkCrystal() = offhandCrystal
        && checkCACrystal && TrollAura.isEnabled && CombatManager.isOnTopPriority(TrollAura)

    private fun SafeClientEvent.checkStrength() = offhandStrength
        && !player.isPotionActive(MobEffects.STRENGTH)
        && player.inventoryContainer.inventory.any(Type.STRENGTH.filter)
        && (checkAuraS && CombatManager.isActiveAndTopPriority(KillAura)
        || checkWeaponS && player.heldItemMainhand.item.isWeapon)

    private fun SafeClientEvent.switchToType(typeOriginal: Type?, alternativeType: Boolean = false) {
        // First check for whether player is holding the right item already or not
        if (typeOriginal == null) {
            lastType = null
            return
        }

        if (checkOffhandItem(typeOriginal)) return

        val attempts = if (alternativeType) 4 else 1

        getItemSlot(typeOriginal, attempts)?.let { (slot, typeAlt) ->
            if (slot == player.offhandSlot) return

            if (mainHandTotem && typeAlt == Type.TOTEM) {
                if (player.heldItemMainhand.item != Items.TOTEM_OF_UNDYING) {
                    MainHandPause.withPause(AutoOffhand, damageTimeout) {
                        swapToItemOrMove(Items.TOTEM_OF_UNDYING)
                    }
                }
            } else {
                onMainThread {
                    lastTask = inventoryTaskNow {
                        postDelay(delay, TimeUnit.TICKS)
                        timeout(confirmTimeout, TimeUnit.TICKS)
                        moveTo(slot, player.offhandSlot)
                    }
                }
            }

            lastType = typeAlt
            if (switchMessage) MessageSendUtils.sendNoSpamChatMessage("$chatName Offhand now has a ${typeAlt.toString().lowercase()}")
        }
    }

    private fun SafeClientEvent.checkOffhandItem(type: Type) = type.filter(player.heldItemOffhand)

    private fun SafeClientEvent.getItemSlot(type: Type, attempts: Int): Pair<Slot, Type>? =
        getSlot(type)?.to(type)
            ?: if (attempts > 1) {
                getItemSlot(type.next(), attempts - 1)
            } else {
                null
            }

    private fun SafeClientEvent.getSlot(type: Type): Slot? {
        return player.offhandSlot.takeIf(filter(type))
            ?: if (priority == Priority.HOTBAR) {
                player.hotbarSlots.findItemByType(type)
                    ?: player.inventorySlots.findItemByType(type)
                    ?: player.craftingSlots.findItemByType(type)
            } else {
                player.inventorySlots.findItemByType(type)
                    ?: player.hotbarSlots.findItemByType(type)
                    ?: player.craftingSlots.findItemByType(type)
            }
    }

    private fun List<Slot>.findItemByType(type: Type) =
        find(filter(type))

    private fun filter(type: Type) = { it: Slot ->
        type.filter(it.stack)
    }

    private fun SafeClientEvent.updateDamage() {
        var maxDamage = 0.0f

        if (checkDamage) {
            if (mob) maxDamage = max(getMobDamage(), maxDamage)
            if (AutoOffhand.player) maxDamage = max(getPlayerDamage(), maxDamage)
            if (arrow) maxDamage = max(getArrowDamage(), maxDamage)
            if (crystal) maxDamage = max(getCrystalDamage(), maxDamage)
            if (falling && nextFallDist > 3.0f) maxDamage = max(ceil(nextFallDist - 3.0f), maxDamage)
        }

        synchronized(damageTimer) {
            if (maxDamage >= lastDamage) {
                lastDamage = maxDamage
                damageTimer.reset()
            } else if (damageTimer.tick(damageTimeout)) {
                lastDamage = maxDamage
            }
        }
    }

    private fun SafeClientEvent.getMobDamage(): Float {
        return EntityManager.livingBase.asSequence()
            .filterIsInstance<EntityMob>()
            .filter { player.getDistanceSq(it) <= 64.0 }
            .maxOfOrNull {
                calcDamageFromMob(it)
            } ?: 0.0f
    }

    private fun SafeClientEvent.getPlayerDamage(): Float {
        return EntityManager.players.asSequence()
            .filterNot { it.isFakeOrSelf }
            .filter { player.getDistanceSq(it) <= 64.0 }
            .maxOfOrNull {
                calcDamageFromPlayer(it, true)
            } ?: 0.0f
    }

    private fun SafeClientEvent.getArrowDamage(): Float {
        val rawDamage = EntityManager.entity.asSequence()
            .filterIsInstance<EntityArrow>()
            .filter { player.getDistanceSq(it) <= 250 }
            .maxOfOrNull {
                var i = ceil(it.realSpeed * it.damage).toFloat()
                if (it.isCritical) i * 0.5f + 1.0f
                if (it is EntityTippedArrow) i += getTippedArrowDamage(it)

                i
            } ?: return 0.0f

        return CombatManager.getDamageReduction(player)?.calcDamage(rawDamage, false)
            ?: rawDamage
    }

    private fun getTippedArrowDamage(arrow: EntityTippedArrow) = arrow.potion.effects
        .firstOrNull { it.potion == MobEffects.INSTANT_DAMAGE }
        ?.let { 3.0f * 2.0f.pow(it.amplifier + 1) }
        ?: 0.0f

    private fun getCrystalDamage(): Float {
        val damage = CombatManager.crystalList.maxOfOrNull { it.second.selfDamage } ?: 0.0f
        return damage.pow(crystalBias)
    }

    private val SafeClientEvent.nextFallDist get() = player.fallDistance - (player.posY - player.prevPosY).toFloat()
}