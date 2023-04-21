package dev.luna5ama.trollhack.util.accessor

import net.minecraft.client.gui.*
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.tileentity.TileEntitySign
import net.minecraft.util.text.ITextComponent
import net.minecraft.world.BossInfo
import java.util.*

val GuiBossOverlay.mapBossInfos: Map<UUID, BossInfoClient>? get() = (this as dev.luna5ama.trollhack.mixins.accessor.gui.AccessorGuiBossOverlay).mapBossInfos
fun GuiBossOverlay.render(x: Int, y: Int, info: BossInfo) =
    (this as dev.luna5ama.trollhack.mixins.accessor.gui.AccessorGuiBossOverlay).invokeRender(x, y, info)

var GuiChat.historyBuffer: String
    get() = (this as dev.luna5ama.trollhack.mixins.accessor.gui.AccessorGuiChat).historyBuffer
    set(value) {
        (this as dev.luna5ama.trollhack.mixins.accessor.gui.AccessorGuiChat).historyBuffer = value
    }
var GuiChat.sentHistoryCursor: Int
    get() = (this as dev.luna5ama.trollhack.mixins.accessor.gui.AccessorGuiChat).sentHistoryCursor
    set(value) {
        (this as dev.luna5ama.trollhack.mixins.accessor.gui.AccessorGuiChat).sentHistoryCursor = value
    }

val GuiDisconnected.parentScreen: GuiScreen get() = (this as dev.luna5ama.trollhack.mixins.accessor.gui.AccessorGuiDisconnected).parentScreen
val GuiDisconnected.reason: String get() = (this as dev.luna5ama.trollhack.mixins.accessor.gui.AccessorGuiDisconnected).reason
val GuiDisconnected.message: ITextComponent get() = (this as dev.luna5ama.trollhack.mixins.accessor.gui.AccessorGuiDisconnected).message

val GuiEditSign.tileSign: TileEntitySign get() = (this as dev.luna5ama.trollhack.mixins.accessor.gui.AccessorGuiEditSign).tileSign
val GuiEditSign.editLine: Int get() = (this as dev.luna5ama.trollhack.mixins.accessor.gui.AccessorGuiEditSign).editLine