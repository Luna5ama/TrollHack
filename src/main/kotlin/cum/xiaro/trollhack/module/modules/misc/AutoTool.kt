package cum.xiaro.trollhack.module.modules.misc

import cum.xiaro.trollhack.event.events.player.InteractEvent
import cum.xiaro.trollhack.event.events.player.PlayerAttackEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.combat.CombatUtils
import cum.xiaro.trollhack.util.combat.CombatUtils.equipBestWeapon
import cum.xiaro.trollhack.util.inventory.equipBestTool
import net.minecraft.entity.EntityLivingBase

internal object AutoTool : Module(
    name = "AutoTool",
    description = "Automatically switch to the best tools when mining or attacking",
    category = Category.MISC
) {
    private val swapWeapon by setting("Switch Weapon", false)
    private val preferWeapon by setting("Prefer", CombatUtils.PreferWeapon.SWORD)

    init {
        safeListener<InteractEvent.Block.LeftClick> {
            if (!player.isCreative && world.getBlockState(it.pos).getBlockHardness(world, it.pos) != -1.0f) {
                equipBestTool(world.getBlockState(it.pos))
            }
        }

        safeListener<PlayerAttackEvent> {
            if (swapWeapon && it.entity is EntityLivingBase) {
                equipBestWeapon(preferWeapon)
            }
        }
    }
}