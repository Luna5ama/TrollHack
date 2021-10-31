package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.mixin.MixinMinecraft
import cum.xiaro.trollhack.mixin.render.MixinEntityRenderer
import cum.xiaro.trollhack.mixin.world.MixinBlockLiquid
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.accessor.side
import cum.xiaro.trollhack.util.and
import cum.xiaro.trollhack.util.atFalse
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.threads.runSafeOrFalse
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPickaxe
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.RayTraceResult
import org.lwjgl.input.Keyboard

/**
 * @see MixinBlockLiquid Liquid Interact
 * @see MixinMinecraft Multi Task
 * @see MixinEntityRenderer No Entity Trace
 */
internal object BlockInteraction : Module(
    name = "BlockInteraction",
    alias = arrayOf("LiquidInteract", "MultiTask", "NoEntityTrace", "NoMiningTrace"),
    category = Category.PLAYER,
    description = "Modifies block interaction"
) {
    private val prioritizeEating by setting("Prioritize Eating", true)
    private val noAirHit by setting("No Air Hit", true)
    private val liquidInteract by setting("Liquid Interact", false, description = "Place block on liquid")
    private val buildLimitBypass by setting("Build Limit Bypass", false)
    private val multiTask by setting("Multi Task", true, description = "Breaks block and uses item at the same time")
    private val noEntityTrace0 = setting("No Entity Trace", true, description = "Interact with blocks through entity")
    private val noEntityTrace by noEntityTrace0
    private val checkBlocks by setting("Check Blocks", true, noEntityTrace0.atTrue(), description = "Only ignores entity when there is block behind")
    private val anyItems0 = setting("Any Items", false, noEntityTrace0.atTrue())
    private val anyItems by anyItems0
    private val pickaxe0 = setting("Pickaxe", true, noEntityTrace0.atTrue() and anyItems0.atFalse(), description = "Ignores entity when holding pickaxe")
    private val pickaxe by pickaxe0
    private val gapple0 = setting("Gapple", true, noEntityTrace0.atTrue() and anyItems0.atFalse(), description = "Ignores entity when holding gapple")
    private val gapple by gapple0
    private val crystal0 = setting("Crystal", true, noEntityTrace0.atTrue() and anyItems0.atFalse(), description = "Ignores entity when holding gapple")
    private val crystal by crystal0
    private val totem0 = setting("Totem", true, noEntityTrace0.atTrue() and anyItems0.atFalse(), description = "Ignores entity when holding totem")
    private val totem by totem0
    private val freecam by setting("Freecam", true, noEntityTrace0.atTrue() and anyItems0.atFalse(), description = "Ignores entity in Freecam")
    private val sneakOverrides by setting("Sneak Override", true, noEntityTrace0.atTrue() and anyItems0.atFalse(), description = "Ignores entity when sneaking")
    private val reverseOverride by setting("Reverse Override", false)

    init {
        listener<PacketEvent.Send> {
            if (buildLimitBypass && it.packet is CPacketPlayerTryUseItemOnBlock && it.packet.pos.y == 255) {
                it.packet.side = EnumFacing.DOWN
            }
        }
    }

    @JvmStatic
    fun isNoAirHitEnabled(): Boolean {
        return isEnabled && noAirHit
    }

    @JvmStatic
    fun isLiquidInteractEnabled(): Boolean {
        return isEnabled && liquidInteract
    }

    @JvmStatic
    fun isMultiTaskEnabled(): Boolean {
        return isEnabled && multiTask
    }

    @JvmStatic
    fun isNoEntityTraceEnabled(): Boolean {
        if (isDisabled || !noEntityTrace) return false

        val sneaking = mc.gameSettings.keyBindSneak.isKeyDown
        if (reverseOverride && sneaking) return false

        val objectMouseOver = mc.objectMouseOver
        val item = mc.player?.heldItemMainhand?.item

        return (!checkBlocks || objectMouseOver != null && objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) // Blocks
            && (sneakOverrides && sneaking
            || freecam && Freecam.isEnabled
            || checkItem(item))
    }

    @JvmStatic
    private fun checkItem(item: Item?): Boolean {
        return anyItems
            || gapple && item == Items.GOLDEN_APPLE
            || crystal && item == Items.END_CRYSTAL
            || totem && item == Items.TOTEM_OF_UNDYING
            || pickaxe && item is ItemPickaxe
    }

    @JvmStatic
    fun isPrioritizingEating(): Boolean {
        if (isDisabled || !prioritizeEating) return false
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) return false
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) return false

        return runSafeOrFalse {
            !player.isCreative && EnumHand.values().any {
                player.getHeldItem(it).item is ItemFood
            }
        }
    }
}
