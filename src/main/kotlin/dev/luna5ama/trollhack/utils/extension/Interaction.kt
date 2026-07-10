package dev.luna5ama.trollhack.utils.extension

import dev.luna5ama.trollhack.utils.NonNullContext
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.multiplayer.prediction.PredictiveAction

context(ctx: NonNullContext)
fun MultiPlayerGameMode.sendSequencedPacket(f: PredictiveAction) = startPrediction(ctx.world, f)
