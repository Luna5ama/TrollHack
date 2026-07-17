package dev.luna5ama.trollhack.modules.impl.movement

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.event.impl.player.InputUpdateEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ServerboundPongPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Input
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.item.Items
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

object NoSlowDown : Module("No Slow Down", "no slow down", Category.MOVEMENT) {
    // 保留 TrollHack 原有蛛网减速兼容设置，同时不影响 Epsilon 的物品减速模式。
    val web by setting("Web", true)
    val horizontalSpeed by setting("H Speed", 0.7, 0.0..1.0, 0.01, { web })
    val verticalSpeed by setting("V Speed", 0.7, 0.0..1.0, 0.01, { web })
    private val mode by setting("Mode", Mode.VANILLA)
    private val food by setting("Food", true)
    private val bow by setting("Bow", true)
    private val crossbow by setting("Crossbow", true)

    private var onGroundTicks = 0
    private var step = Step.NONE
    private var noUsingItemTicks = 0
    private val packets: Queue<Packet<*>> = ConcurrentLinkedQueue()

    init {
        onEnabled {
            onGroundTicks = 0
            step = Step.NONE
            noUsingItemTicks = 0
        }
        onDisabled {
            releasePackets()
            noUsingItemTicks = 0
            mc.options.keyUse.isDown = false
        }

        nonNullHandler<TickEvent.Pre> {
            onGroundTicks = if (player.onGround()) onGroundTicks + 1 else 0
            if (mode != Mode.GRIM_C0F) return@nonNullHandler

            if (step != Step.EATING) {
                noUsingItemTicks = 0
            } else if (player.isUsingItem) {
                noUsingItemTicks = 0
            } else if (++noUsingItemTicks >= 5) {
                releasePackets()
                swapHands()
            }
        }

        nonNullHandler<InputUpdateEvent> { event ->
            if (mode != Mode.JUMP || !player.onGround() || !player.isUsingItem || !isEnabledFor(player)) {
                return@nonNullHandler
            }
            val input = event.movementInput.keyPresses
            if (!input.forward() && !input.backward() && !input.left() && !input.right()) return@nonNullHandler
            event.movementInput.keyPresses = Input(
                input.forward(),
                input.backward(),
                input.left(),
                input.right(),
                true,
                input.shift(),
                input.sprint()
            )
        }

        nonNullHandler<PacketEvent.Send> { event ->
            if (mode != Mode.GRIM_C0F) return@nonNullHandler
            when (val packet = event.packet) {
                is ServerboundPongPacket -> if (step != Step.NONE) {
                    event.cancel()
                    packets.add(packet)
                    if (step == Step.CANCEL_C0F) {
                        step = Step.SWAP_HANDS
                        swapHands()
                    }
                }
                is ServerboundPlayerActionPacket -> if (
                    packet.action == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM && step == Step.EATING
                ) {
                    releasePackets()
                    swapHands()
                }
            }
        }

        nonNullHandler<PacketEvent.Receive> { event ->
            if (mode == Mode.GRIM_C0F && event.packet is ClientboundContainerSetSlotPacket && step == Step.SWAP_HANDS) {
                mc.options.keyUse.isDown = true
                step = Step.EATING
            }
        }
    }

    @JvmStatic
    fun shouldApplySlowdown(player: LocalPlayer): Boolean {
        if (!isEnabled || !player.isUsingItem) return player.isUsingItem
        if (!isEnabledFor(player)) return true

        return when (mode) {
            Mode.VANILLA -> false
            Mode.JUMP -> !(player.onGround() && onGroundTicks == 1 && player.useItemRemainingTicks <= 30)
            Mode.GRIM_1_2 -> !(player.useItemRemainingTicks <= 30 && player.useItemRemainingTicks % 2 == 0)
            Mode.GRIM_1_3 -> !(player.useItemRemainingTicks <= 30 && player.useItemRemainingTicks % 3 == 0)
            Mode.GRIM_C0F -> {
                if (step == Step.EATING) {
                    player.setSprinting(true)
                    false
                } else {
                    beginGrimC0F(player)
                    true
                }
            }
        }
    }

    private fun isEnabledFor(player: LocalPlayer): Boolean {
        val stack = player.useItem
        if (!food && stack.has(net.minecraft.core.component.DataComponents.FOOD)) return false
        if (!bow && stack.`is`(Items.BOW)) return false
        if (!crossbow && stack.`is`(Items.CROSSBOW)) return false
        return true
    }

    private fun beginGrimC0F(player: LocalPlayer) {
        if (!isUsable(player.useItem.useAnimation)) return
        val opposite = if (player.usedItemHand == InteractionHand.MAIN_HAND) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
        if (isUsable(player.getItemInHand(opposite).useAnimation)) return

        if (step == Step.NONE) {
            mc.options.keyUse.isDown = false
            step = Step.CANCEL_C0F
            if (player.containerMenu !== player.inventoryMenu) {
                mc.connection?.send(ServerboundContainerClosePacket(player.containerMenu.containerId))
            }
        }
    }

    private fun releasePackets() {
        step = Step.NONE
        while (true) {
            val packet = packets.poll() ?: break
            mc.connection?.send(packet)
        }
    }

    private fun swapHands() {
        mc.connection?.send(
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ZERO,
                Direction.DOWN
            )
        )
    }

    private fun isUsable(animation: ItemUseAnimation): Boolean {
        return animation == ItemUseAnimation.EAT || animation == ItemUseAnimation.DRINK
    }

    enum class Mode(override val displayName: CharSequence) : Displayable {
        VANILLA("Vanilla"),
        JUMP("Jump"),
        GRIM_C0F("GrimC0F"),
        GRIM_1_2("Grim1_2"),
        GRIM_1_3("Grim1_3")
    }

    private enum class Step {
        NONE,
        CANCEL_C0F,
        SWAP_HANDS,
        EATING
    }
}
