package cum.xiaro.trollhack.util.accessor

import cum.xiaro.trollhack.accessor.*
import cum.xiaro.trollhack.accessor.entity.AccessorEntity
import cum.xiaro.trollhack.accessor.entity.AccessorEntityTippedArrow
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.EntityTippedArrow
import net.minecraft.item.ItemTool
import net.minecraft.potion.PotionType
import net.minecraft.util.Timer
import net.minecraft.world.chunk.BlockStateContainer

val Entity.isInWeb: Boolean get() = (this as AccessorEntity).`troll$getIsInWeb`()

fun Entity.getFlag(flag: Int) {
    (this as AccessorEntity).`troll$getFlag`(flag)
}

fun Entity.setFlag(flag: Int, value: Boolean) {
    (this as AccessorEntity).`troll$setFlag`(flag, value)
}

val EntityTippedArrow.potion: PotionType
    get() = (this as AccessorEntityTippedArrow).potion

val ItemTool.attackDamage get() = (this as AccessorItemTool).attackDamage

val Minecraft.timer: Timer get() = (this as AccessorMinecraft).trollGetTimer()
val Minecraft.renderPartialTicksPaused: Float get() = (this as AccessorMinecraft).renderPartialTicksPaused
var Minecraft.rightClickDelayTimer: Int
    get() = (this as AccessorMinecraft).rightClickDelayTimer
    set(value) {
        (this as AccessorMinecraft).rightClickDelayTimer = value
    }

fun Minecraft.rightClickMouse() = (this as AccessorMinecraft).invokeRightClickMouse()

fun Minecraft.sendClickBlockToController(leftClick: Boolean) = (this as AccessorMinecraft).invokeSendClickBlockToController(leftClick)

var Timer.tickLength: Float
    get() = (this as AccessorTimer).trollGetTickLength()
    set(value) {
        (this as AccessorTimer).trollSetTickLength(value)
    }

fun KeyBinding.unpressKey() = (this as AccessorKeyBinding).`troll$invoke$unpressKey`()

val BlockStateContainer.storage get() = (this as AccessorBlockStateContainer).trollGetStorage()

val BlockStateContainer.palette get() = (this as AccessorBlockStateContainer).trollGetPalette()