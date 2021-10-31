package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.util.extension.fastCeil
import cum.xiaro.trollhack.util.graphics.Easing
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.RunGameLoopEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.combat.CrystalSpawnEvent
import cum.xiaro.trollhack.event.safeConcurrentListener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.CombatManager
import cum.xiaro.trollhack.manager.managers.EntityManager
import cum.xiaro.trollhack.manager.managers.HoleManager
import cum.xiaro.trollhack.manager.managers.HotbarManager.spoofHotbar
import cum.xiaro.trollhack.manager.managers.PlayerPacketManager
import cum.xiaro.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.EntityUtils.betterPosition
import cum.xiaro.trollhack.util.EntityUtils.isFakeOrSelf
import cum.xiaro.trollhack.util.MovementUtils.isCentered
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.combat.CombatUtils.scaledHealth
import cum.xiaro.trollhack.util.inventory.InventoryTask
import cum.xiaro.trollhack.util.inventory.executedOrTrue
import cum.xiaro.trollhack.util.inventory.inventoryTask
import cum.xiaro.trollhack.util.inventory.operation.pickUp
import cum.xiaro.trollhack.util.inventory.operation.quickMove
import cum.xiaro.trollhack.util.inventory.slot.*
import cum.xiaro.trollhack.util.items.duraPercentage
import cum.xiaro.trollhack.util.math.vector.Vec2f
import cum.xiaro.trollhack.util.text.NoSpamMessage
import cum.xiaro.trollhack.util.threads.runSafe
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.init.Enchantments
import net.minecraft.init.Items
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.network.play.server.SPacketSpawnExperienceOrb
import net.minecraft.network.play.server.SPacketSpawnObject
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import kotlin.math.min

@CombatManager.CombatModule
internal object AutoMend : Module(
    name = "AutoMend",
    category = Category.COMBAT,
    description = "Automatically mends armour",
    modulePriority = 100
) {
    private val minHealth by setting("Min Health", 8.0f, 0.0f..20.0f, 0.5f)
    private val targetDurability by setting("Target Durability", 85, 50..100, 1)
    private val takeOffInHole by setting("Take Off In Hole", true)
    private val useCraftingSlot by setting("Use Crafting Slot", true)
    private val slowThreshold by setting("Slow Threshold", 0.7f, 0.0f..1.0f, 0.01f)
    private val fastThrow by setting("Fast Throw", 1, 1..10, 1)
    private val minDelay by setting("Min Delay", 10, 0..250, 5)
    private val maxDelay by setting("Max Delay", 50, 0..250, 5)

    private val disableMessageID = Any()
    private val timer = TickTimer()
    private val lastTasks = arrayOfNulls<InventoryTask>(4)
    private val armorSlots = arrayOfNulls<Pair<Slot, ItemStack>>(4)

    private var waiting = false
    private var throwAmount = 0
    private var confirmedAmount = 0
    private var xpDiff = 0
    private var lastBottlePacket = 0L
    private var xpSlot: HotbarSlot? = null
    private var inHole = false

    override fun isActive(): Boolean {
        return isEnabled && (!takeOffInHole || inHole)
    }

    init {
        onEnable {
            runSafe {
                val noArmor = player.armorSlots.all {
                    val itemStack = it.stack
                    itemStack.isEmpty
                        || !itemStack.isItemStackDamageable
                        || EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, itemStack) == 0
                }

                if (noArmor) {
                    NoSpamMessage.sendMessage(disableMessageID, "$chatName No armor to repair")
                    disable()
                    return@runSafe
                }

                updateInHole()
            } ?: disable()
        }

        onDisable {
            lastTasks.fill(null)
            armorSlots.fill(null)

            waiting = false
            throwAmount = 0
            confirmedAmount = 0
            xpDiff = 0
            lastBottlePacket = 0L
            xpSlot = null
        }

        safeListener<PacketEvent.Receive> { event ->
            when (event.packet) {
                // Counts xp bottle spawning
                is SPacketSpawnObject -> {
                    if (event.packet.type == 75
                        && player.getDistanceSq(event.packet.x, event.packet.y, event.packet.z) < 5.0) {
                        confirmedAmount--
                        lastBottlePacket = System.currentTimeMillis()
                    }
                }
                // Throw more xp bottles if we didn't get enough
                is SPacketSpawnExperienceOrb -> {
                    if (player.getDistanceSq(event.packet.x, event.packet.y, event.packet.z) < 5.0
                        && player.armorInventoryList.all { it.itemDamage > 100 }) {
                        xpDiff += 11 - event.packet.xpValue

                        if (xpDiff > 0) {
                            while (xpDiff >= 11) {
                                throwAmount++
                                confirmedAmount++
                                xpDiff -= 11
                            }
                        } else {
                            while (xpDiff <= -11) {
                                throwAmount--
                                confirmedAmount--
                                xpDiff += 11
                            }
                        }
                    }
                }
            }
        }

        safeListener<CrystalSpawnEvent> {
            if (player.scaledHealth - it.crystalDamage.selfDamage <= minHealth) {
                NoSpamMessage.sendMessage(disableMessageID, "$chatName Lethal crystal nearby")
                disable()
            }
        }

        safeListener<TickEvent.Pre> {
            if (!preCheck()) {
                return@safeListener
            }

            updateThrowAmount()

            sendPlayerPacket {
                rotate(Vec2f(player.rotationYaw, 90.0f))
            }
        }

        safeConcurrentListener<TickEvent.Post> {
            updateInHole()
        }

        safeListener<RunGameLoopEvent.Start> {
            if (!preCheck()) {
                return@safeListener
            }

            findAndMoveArmor()
        }

        safeListener<RunGameLoopEvent.Render> {
            if (!preCheck()) {
                return@safeListener
            }

            xpSlot?.let { slot ->
                if (throwAmount > 0
                    && PlayerPacketManager.rotation.y > 85.0f
                    && timer.tick(minDelay)
                    && lastTasks.all { it.executedOrTrue }) {
                    val notNull = armorSlots.filterNotNull()
                    if (notNull.any { it.second.duraPercentage >= targetDurability }) return@safeListener

                    val maxDura = notNull
                        .maxOfOrNull { it.second.duraPercentage.toFloat() }
                        ?: return@safeListener
                    val threshold = targetDurability * slowThreshold
                    val slowing = maxDura >= threshold

                    if (slowing) {
                        if (timer.tickAndReset(Easing.OUT_CUBIC.inc((threshold) / targetDurability, minDelay.toFloat(), maxDelay.toFloat()).toInt())) {
                            spoofHotbar(slot) {
                                connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
                                throwAmount--
                            }
                        }
                    } else {
                        if (timer.tickAndReset(minDelay)) {
                            spoofHotbar(slot) {
                                var count = 0
                                while (count++ < fastThrow && throwAmount > 0) {
                                    connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
                                    throwAmount--
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.preCheck(): Boolean {
        if (player.scaledHealth <= minHealth) {
            NoSpamMessage.sendMessage(disableMessageID, "$chatName Low health")
            disable()
            return false
        }

        if (!checkNearbyPlayers()) {
            NoSpamMessage.sendMessage(disableMessageID, "$chatName Players nearby")
            disable()
            return false
        }

        xpSlot = findXp() ?: run {
            NoSpamMessage.sendMessage(disableMessageID, "$chatName No xp bottle found in hotbar")
            disable()
            return false
        }

        updateArmorSlots()

        if (checkFinished()) {
            NoSpamMessage.sendMessage(disableMessageID, "$chatName Finished mending armors")
            disable()
            return false
        }

        return true
    }

    private fun SafeClientEvent.checkNearbyPlayers(): Boolean {
        val box = AxisAlignedBB(
            player.posX - 0.5, player.posY - 0.5, player.posZ - 0.5,
            player.posX + 0.5, player.posY + 2.5, player.posZ + 0.5,
        )

        return EntityManager.players.none { !it.isFakeOrSelf && it.entityBoundingBox.intersects(box) }
    }

    private fun SafeClientEvent.updateArmorSlots() {
        for ((index, slot) in player.armorSlots.withIndex()) {
            val itemStack = slot.stack
            if (itemStack.isRepairable()) {
                armorSlots[index] = (slot to itemStack)
            } else {
                armorSlots[index] = null
            }
        }
    }

    private fun ItemStack.isRepairable(): Boolean {
        return this.isItemStackDamageable && EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, this) > 0
    }

    private fun SafeClientEvent.findXp(): HotbarSlot? {
        return player.hotbarSlots.firstItem(Items.EXPERIENCE_BOTTLE)
    }

    private fun checkFinished(): Boolean {
        return armorSlots.all {
            it == null || it.second.duraPercentage >= targetDurability
        }
    }

    private fun SafeClientEvent.findAndMoveArmor() {
        var count = 0
        val list = armorSlots.asSequence()
            .filterNotNull()
            .onEach { count++ }
            .filter { it.second.duraPercentage >= targetDurability }
            .sortedBy { it.second.itemDamage }
            .toList()

        if (takeOffInHole && !inHole && list.isNotEmpty()) {
            NoSpamMessage.sendMessage(disableMessageID, "$chatName Finished mending armors")
            disable()
        } else if (count > 1) {
            var emptySlots = player.inventorySlots.countEmpty()
            if (useCraftingSlot) emptySlots += player.craftingSlots.countEmpty()

            if (emptySlots >= list.size - 1) {
                for ((slot, _) in list) {
                    if (!moveArmor(slot)) return
                }
            } else {
                NoSpamMessage.sendMessage(disableMessageID, "$chatName No empty slot for moving armor")
                disable()
            }
        }
    }

    private fun SafeClientEvent.moveArmor(slot: Slot): Boolean {
        val emptyCraftingSlot = player.craftingSlots.firstEmpty()
        val index = slot.slotNumber - 5

        if (lastTasks[index].executedOrTrue) {
            when {
                player.inventorySlots.hasEmpty() -> {
                    lastTasks[index] = inventoryTask {
                        quickMove(slot)
                    }
                }
                useCraftingSlot && emptyCraftingSlot != null -> {
                    lastTasks[index] = inventoryTask {
                        pickUp(slot)
                        pickUp(emptyCraftingSlot)
                    }
                }
                else -> {
                    NoSpamMessage.sendMessage(disableMessageID, "$chatName No empty slot for moving armor")
                    disable()
                    return false
                }
            }
        }

        return true
    }

    private fun updateThrowAmount() {
        val thrown = throwAmount <= 0
        val confirmed = confirmedAmount <= 0

        if (thrown && confirmed) {
            val (leastDamaged, mostDamaged) = getLeastAndMostDamaged() ?: return
            val targetMax = (mostDamaged.maxDamage * (1.0f - targetDurability / 100.0f)).toInt()

            // Xp bottle gives 11 exp at max and 1 exp mends 2 item damage
            val requiredForMin = leastDamaged.itemDamage / 22
            val requiredForMax = ((mostDamaged.itemDamage - targetMax) / 22.0f).fastCeil()

            val minRequired = min(requiredForMin, requiredForMax)
            throwAmount = minRequired
            confirmedAmount = minRequired
            xpDiff = 0

            lastBottlePacket = System.currentTimeMillis()
        } else if (thrown && !confirmed && System.currentTimeMillis() - lastBottlePacket > 100L) {
            throwAmount = 0
            confirmedAmount = 0
        }
    }

    private fun getLeastAndMostDamaged(): Pair<ItemStack, ItemStack>? {
        var leastDamaged: ItemStack? = null
        var minDamage = Int.MAX_VALUE
        var mostDamaged: ItemStack? = null
        var maxDamage = Int.MIN_VALUE

        for (pair in armorSlots) {
            if (pair == null) continue
            val itemStack = pair.second

            if (itemStack.itemDamage < minDamage) {
                leastDamaged = itemStack
                minDamage = itemStack.itemDamage
            }
            if (itemStack.itemDamage > maxDamage) {
                mostDamaged = itemStack
                maxDamage = itemStack.itemDamage
            }
        }

        return if (leastDamaged != null && mostDamaged != null) {
            leastDamaged to mostDamaged
        } else {
            null
        }
    }

    private fun SafeClientEvent.updateInHole() {
        inHole = HoleManager.getHoleInfo(player).isHole
            || player.betterPosition.let { player.isCentered(it) && world.getBlockState(it).getCollisionBoundingBox(world, it) != null }
    }
}
