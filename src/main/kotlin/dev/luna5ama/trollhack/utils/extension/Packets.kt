package dev.luna5ama.trollhack.utils.extension

import net.minecraft.network.protocol.game.ClientboundExplodePacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket

val ClientboundExplodePacket.x get() = center.x
val ClientboundExplodePacket.y get() = center.y
val ClientboundExplodePacket.z get() = center.z

val ClientboundPlayerPositionPacket.x get() = this.change.position.x
val ClientboundPlayerPositionPacket.y get() = this.change.position.y
val ClientboundPlayerPositionPacket.z get() = this.change.position.z