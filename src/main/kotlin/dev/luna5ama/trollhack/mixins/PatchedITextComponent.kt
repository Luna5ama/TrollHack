package dev.luna5ama.trollhack.mixins

import net.minecraft.util.text.ITextComponent

interface PatchedITextComponent {
    fun inplaceIterator(): Iterator<ITextComponent>
}