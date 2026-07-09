package dev.luna5ama.trollhack.utils.extension

import dev.luna5ama.trollhack.utils.MinecraftWrapper
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.enchantment.Enchantment

val ResourceKey<Enchantment>.entry get() = MinecraftWrapper.mc.level!!.registryAccess().get(this).get()