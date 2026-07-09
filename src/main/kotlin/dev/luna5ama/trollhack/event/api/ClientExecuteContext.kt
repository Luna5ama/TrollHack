package dev.luna5ama.trollhack.event.api

import dev.luna5ama.trollhack.command.CommandManager
import dev.luna5ama.trollhack.command.execute.ExecuteEvent
import dev.luna5ama.trollhack.command.execute.IExecuteEvent
import dev.luna5ama.trollhack.utils.ClientContext
import dev.luna5ama.trollhack.utils.NonNullContext
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.LocalPlayer

fun ClientExecuteContext.toSafe() =
    if (world != null && player != null && interaction != null && netHandler != null) NonNullExecuteContext(
        world,
        player,
        interaction,
        netHandler,
        this
    )
    else null

class ClientExecuteContext(
    args: Array<String>
) : ClientContext(), IExecuteEvent by ExecuteEvent(CommandManager, args)

class NonNullExecuteContext internal constructor(
    world: ClientLevel,
    player: LocalPlayer,
    interaction: MultiPlayerGameMode,
    connection: ClientPacketListener,
    event: ClientExecuteContext
) : NonNullContext(world, player, interaction, connection), IExecuteEvent by event