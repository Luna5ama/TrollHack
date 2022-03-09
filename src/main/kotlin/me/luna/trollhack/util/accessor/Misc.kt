package me.luna.trollhack.util.accessor

import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.EntityTippedArrow
import net.minecraft.item.ItemTool
import net.minecraft.potion.PotionType
import net.minecraft.util.Timer
import net.minecraft.world.chunk.BlockStateContainer

val Entity.isInWeb: Boolean get() = (this as me.luna.trollhack.mixins.accessor.entity.AccessorEntity).`troll$getIsInWeb`()

fun Entity.getFlag(flag: Int) {
    (this as me.luna.trollhack.mixins.accessor.entity.AccessorEntity).`troll$getFlag`(flag)
}

fun Entity.setFlag(flag: Int, value: Boolean) {
    (this as me.luna.trollhack.mixins.accessor.entity.AccessorEntity).`troll$setFlag`(flag, value)
}

val EntityTippedArrow.potion: PotionType
    get() = (this as me.luna.trollhack.mixins.accessor.entity.AccessorEntityTippedArrow).potion

val ItemTool.attackDamage get() = (this as me.luna.trollhack.mixins.accessor.AccessorItemTool).attackDamage

val Minecraft.timer: Timer get() = (this as me.luna.trollhack.mixins.accessor.AccessorMinecraft).trollGetTimer()
val Minecraft.renderPartialTicksPaused: Float get() = (this as me.luna.trollhack.mixins.accessor.AccessorMinecraft).renderPartialTicksPaused
var Minecraft.rightClickDelayTimer: Int
    get() = (this as me.luna.trollhack.mixins.accessor.AccessorMinecraft).rightClickDelayTimer
    set(value) {
        (this as me.luna.trollhack.mixins.accessor.AccessorMinecraft).rightClickDelayTimer = value
    }

fun Minecraft.rightClickMouse() = (this as me.luna.trollhack.mixins.accessor.AccessorMinecraft).invokeRightClickMouse()

fun Minecraft.sendClickBlockToController(leftClick: Boolean) = (this as me.luna.trollhack.mixins.accessor.AccessorMinecraft).invokeSendClickBlockToController(leftClick)

var Timer.tickLength: Float
    get() = (this as me.luna.trollhack.mixins.accessor.AccessorTimer).trollGetTickLength()
    set(value) {
        (this as me.luna.trollhack.mixins.accessor.AccessorTimer).trollSetTickLength(value)
    }

fun KeyBinding.unpressKey() = (this as me.luna.trollhack.mixins.accessor.AccessorKeyBinding).`troll$invoke$unpressKey`()

val BlockStateContainer.storage get() = (this as me.luna.trollhack.mixins.accessor.AccessorBlockStateContainer).trollGetStorage()

val BlockStateContainer.palette get() = (this as me.luna.trollhack.mixins.accessor.AccessorBlockStateContainer).trollGetPalette()