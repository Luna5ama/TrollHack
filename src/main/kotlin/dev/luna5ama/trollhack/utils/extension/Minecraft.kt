package dev.luna5ama.trollhack.utils.extension

import net.minecraft.client.Minecraft

val Minecraft.profiler get() = metricsRecorder.profiler